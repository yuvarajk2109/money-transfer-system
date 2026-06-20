import {
  ChangeDetectorRef,
  Component,
  OnInit,
  ViewChild
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';
import { RouterModule } from '@angular/router';
import { FormService } from '../../core/services/form.service';
import { BalanceService } from '../../core/services/balance.service';
import { AccountSearch } from '../../shared/account-search/account-search';
import { Dropdown, DropdownOption } from '../../shared/dropdown/dropdown';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    FormsModule,
    AccountSearch,
    Dropdown
  ],
  templateUrl: './transfer.html'
})
export class Transfer implements OnInit {

  accountsAPI = API.ACCOUNTS.SEARCH;

  transferData: any = {
    fromAccountId: Number(localStorage.getItem('accountId')),
    toAccountId: 0,
    amount: null,
    usedRewardPoints: null,
    idempotencyKey: crypto.randomUUID()
  };

  currentBalance = Number(localStorage.getItem('balance') || 0);
  minBalance = 1000;
  
  useRewardPoints = false;
  activePoints = 0;

  loading = false;
  message = '';
  messageType = '';

  // Multi-account
  myAccounts: any[] = [];
  hasMultipleAccounts = false;
  isSelfTransfer = false;
  fromAccountOptions: DropdownOption[] = [];

  @ViewChild(AccountSearch) accountSearch!: AccountSearch;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private form: FormService,
    private balanceService: BalanceService
  ) { }

  ngOnInit(): void {
    this.loadMyAccounts();
    this.loadActivePoints();
  }

  loadActivePoints(): void {
    this.http.get<any>(API.REWARDS.GROUP_TOTAL).subscribe({
      next: (data) => {
        this.activePoints = data.totalPoints || 0;
        this.cdr.detectChanges();
      }
    });
  }

  loadMyAccounts(): void {
    this.http.get<any[]>(API.ACCOUNTS.MY_ACCOUNTS).subscribe({
      next: (data) => {
        this.myAccounts = data;
        this.hasMultipleAccounts = data.length > 1;
        this.fromAccountOptions = data.map(a => ({
          label: `${a.accountType} (ID: ${a.id}) - ₹${a.balance}`,
          value: a.id
        }));
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load accounts', err);
      }
    });
  }

  onFromAccountChange(): void {
    const selected = this.myAccounts.find(a => a.id === this.transferData.fromAccountId);
    if (selected) {
      this.currentBalance = Number(selected.balance);
      this.minBalance = Number(selected.minBalance);
    }
    // Reset self-transfer target when changing from account
    if (this.isSelfTransfer) {
      this.transferData.toAccountId = 0;
    }
    this.clearMessage();
  }

  toggleSelfTransfer(): void {
    this.isSelfTransfer = !this.isSelfTransfer;
    this.transferData.toAccountId = 0;
    this.clearMessage();
  }

  onSelfTransferTargetChange(): void {
    this.clearMessage();
  }

  get selfTransferTargets(): any[] {
    return this.myAccounts.filter(a => a.id !== this.transferData.fromAccountId);
  }

  get selfTransferTargetOptions(): DropdownOption[] {
    return this.selfTransferTargets.map(a => ({
      label: `${a.accountType} (ID: ${a.id}) - ₹${a.balance}`,
      value: a.id
    }));
  }

  get myAccountIds(): number[] {
    return this.myAccounts.map(a => a.id);
  }

  onAccountSelected(acc: any) {
    this.transferData.toAccountId = acc?.id || 0;
  }

  clearMessage(): void {
    this.message = '';
    this.messageType = '';
  }

  transfer(form: NgForm): void {

    if (this.loading) return;

    if (!this.transferData.toAccountId) {
      this.form.setError(this, null, 'Please select a valid account');
      return;
    }

    if (this.useRewardPoints && !this.isSelfTransfer) {
      const usedPoints = (this.transferData as any).usedRewardPoints || 0;
      if (usedPoints <= 0) {
        this.form.setError(this, null, 'Please specify the number of reward points to use.');
        return;
      }
      if (usedPoints > this.activePoints) {
        this.form.setError(this, null, 'You cannot use more reward points than you have available.');
        return;
      }
      if (usedPoints > this.transferData.amount) {
        this.form.setError(this, null, 'Reward points used cannot exceed the transfer amount.');
        return;
      }
    } else {
      (this.transferData as any).usedRewardPoints = 0;
    }

    this.message = '';
    this.messageType = '';
    this.loading = true;

    const currentToAccountId = this.transferData.toAccountId;
    const currentAmount = this.transferData.amount;
    const transferType = this.isSelfTransfer ? 'Self-transfer' : 'Transfer';

    this.http.post(API.TRANSFERS.CREATE, this.transferData)
      .subscribe({
        next: () => {
          this.transferData.idempotencyKey = crypto.randomUUID();
          
          form.resetForm({
            fromAccount: this.transferData.fromAccountId
          });
          
          this.transferData.amount = null;
          this.transferData.toAccountId = 0;
          (this.transferData as any).usedRewardPoints = null;
          this.useRewardPoints = false;
          
          if (!this.isSelfTransfer && this.accountSearch) {
            this.accountSearch.clearSelection(false);
          }

          this.balanceService.notifyBalanceChanged();
          this.loadMyAccounts();
          this.loadActivePoints();
          
          this.loading = false;
          this.form.setSuccess(this, `${transferType} of ₹${currentAmount} to Account ID ${currentToAccountId} successful!`);
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.loading = false;
          this.form.setError(this, err, 'Transfer failed');
          this.cdr.detectChanges();
        }
      });
  }
}
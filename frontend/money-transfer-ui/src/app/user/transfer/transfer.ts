import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';
import { RouterModule } from '@angular/router';
import { FormService } from '../../core/services/form.service';
import { BalanceService } from '../../core/services/balance.service';
import { AccountSearch } from '../../shared/account-search/account-search';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    FormsModule,
    AccountSearch
  ],
  templateUrl: './transfer.html'
})
export class Transfer implements OnInit {

  accountsAPI = API.ACCOUNTS.SEARCH;

  transferData = {
    fromAccountId: Number(localStorage.getItem('accountId')),
    toAccountId: 0,
    amount: 0,
    idempotencyKey: crypto.randomUUID()
  };

  currentBalance = Number(localStorage.getItem('balance') || 0);
  minBalance = 1000;

  loading = false;
  message = '';
  messageType = '';

  // Multi-account
  myAccounts: any[] = [];
  hasMultipleAccounts = false;
  isSelfTransfer = false;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private form: FormService,
    private balanceService: BalanceService
  ) {}

  ngOnInit(): void {
    this.loadMyAccounts();
  }

  loadMyAccounts(): void {
    this.http.get<any[]>(API.ACCOUNTS.MY_ACCOUNTS).subscribe({
      next: (data) => {
        this.myAccounts = data;
        this.hasMultipleAccounts = data.length > 1;
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

  onAccountSelected(acc: any) {
    this.transferData.toAccountId = acc?.id || 0;
  }

  clearMessage(): void {
    this.message = '';
    this.messageType = '';
  }

  transfer(): void {

    if (this.loading) return;

    if (!this.transferData.toAccountId) {
      this.form.setError(this, null, 'Please select a valid account');
      return;
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
          this.form.setSuccess(this, `${transferType} of ₹${currentAmount} to Account ID ${currentToAccountId} successful!`);
          this.transferData.idempotencyKey = crypto.randomUUID();
          this.transferData.amount = 0;
          this.transferData.toAccountId = 0;
          this.balanceService.notifyBalanceChanged();
          this.loadMyAccounts();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.form.setError(this, err, 'Transfer failed');
          this.cdr.detectChanges();
        }
      });
  }
}
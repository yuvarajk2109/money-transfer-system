import { ChangeDetectorRef, Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';
import { RouterModule } from '@angular/router';
import { FormService } from '../../core/services/form.service';
import { AccountSearch } from '../../shared/account-search/account-search';

@Component({
  selector: 'app-deposit',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    FormsModule,
    AccountSearch
  ],
  templateUrl: './deposit.html'
})
export class Deposit {

  accountsAPI = API.ADMIN.ALL_ACCOUNTS;

  @ViewChild(AccountSearch) accountSearch!: AccountSearch;

  accountId = 0;
  amount: number | null = null;
  message = '';
  messageType = '';
  loading = false;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private form: FormService
  ) {}

  onAccountSelected(account: any): void {
    this.accountId = account.id;
  }

  clearMessage(): void {
    this.message = '';
    this.messageType = '';
  }

  deposit(form: NgForm): void {

    if (this.loading) return;

    this.message = '';
    this.messageType = '';
    this.loading = true;

    const currentAccountId = this.accountId;
    const currentAmount = this.amount;

    this.http.post(API.ADMIN.DEPOSIT, {
      accountId: currentAccountId,
      amount: currentAmount
    }).subscribe({
      next: () => {
        this.accountId = 0;
        this.amount = null;
        form.resetForm();
        if (this.accountSearch) {
          this.accountSearch.clearSelection(false);
        }
        this.loading = false;
        this.form.setSuccess(this, `Deposit of ₹${currentAmount} to Account ID ${currentAccountId} successful`);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.form.setError(this, err, 'Deposit failed');
        this.cdr.detectChanges();
      }
    });
  }
}
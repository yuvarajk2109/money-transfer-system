import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';
import { RouterModule } from '@angular/router';
import { PaginationComponent } from '../../shared/pagination/pagination';
import { FormsModule } from '@angular/forms';
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';
import { Dropdown, DropdownOption } from '../../shared/dropdown/dropdown';
import { CustomDatePipe } from '../../shared/pipes/custom-date.pipe';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    FormsModule,
    PaginationComponent,
    ConfirmDialog,
    Dropdown,
    CustomDatePipe
  ],
  templateUrl: './transactions.html'
})
export class Transactions implements OnInit {

  transactions: any[] = [];
  filteredTransactions: any[] = [];

  filters = {
    transactionType: '',
    status: '',
    dateRange: ''
  };

  dateOptions: DropdownOption[] = [
    { label: 'All', value: '' },
    { label: 'Today', value: 'today' },
    { label: 'Yesterday', value: 'yesterday' },
    { label: 'Last 7 Days', value: 'week' },
    { label: 'Last 30 Days', value: 'month' }
  ];

  typeOptions: DropdownOption[] = [
    { label: 'All', value: '' },
    { label: 'Debit', value: 'DEBIT' },
    { label: 'Credit', value: 'CREDIT' },
    { label: 'Self Transfer', value: 'SELF_TRANSFER' },
    { label: 'Reversal', value: 'REVERSAL' }
  ];

  statusOptions: DropdownOption[] = [
    { label: 'All', value: '' },
    { label: 'Success', value: 'SUCCESS' },
    { label: 'Failed', value: 'FAILED' },
    { label: 'Rollback Requested', value: 'ROLLBACK_REQUESTED' },
    { label: 'Rolled Back', value: 'ROLLED_BACK' },
    { label: 'Rollback Rejected', value: 'ROLLBACK_REJECTED' }
  ];

  pageSize = 10;
  currentPage = 1;

  // rollback dialog state
  showRollbackDialog = false;
  pendingRollbackId: string | null = null;

  // Multi-account
  myAccounts: any[] = [];
  hasMultipleAccounts = false;
  selectedAccountId = Number(localStorage.getItem('accountId'));
  selectedAccountType = '';
  accountOptions: DropdownOption[] = [];

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.loadMyAccounts();
  }

  loadMyAccounts(): void {
    this.http.get<any[]>(API.ACCOUNTS.MY_ACCOUNTS).subscribe({
      next: (data) => {
        this.myAccounts = data;
        this.hasMultipleAccounts = data.length > 1;

        this.accountOptions = data.map((a: any) => ({
          label: a.accountType,
          value: a.id
        }));

        // Set the current account's type
        const current = data.find((a: any) => a.id === this.selectedAccountId);
        this.selectedAccountType = current?.accountType || '';

        this.reloadTransactions();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load accounts', err);
        this.reloadTransactions();
      }
    });
  }

  onAccountFilterChange(): void {
    const selected = this.myAccounts.find(a => a.id === this.selectedAccountId);
    this.selectedAccountType = selected?.accountType || '';
    this.reloadTransactions();
  }

  reloadTransactions(): void {
    const url = this.hasMultipleAccounts
      ? `${API.TRANSFERS.HISTORY}?accountId=${this.selectedAccountId}`
      : API.TRANSFERS.HISTORY;

    this.http.get<any[]>(url)
      .subscribe({
        next: (data) => {
          this.transactions = data;
          this.applyFilters();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to load transactions:', err);
        }
      });
  }

  applyFilters(): void {

    const now = new Date();

    this.filteredTransactions = this.transactions.filter(tx => {

      if (this.filters.transactionType &&
        tx.transactionType !== this.filters.transactionType) {
        return false;
      }

      if (this.filters.status &&
        tx.status !== this.filters.status) {
        return false;
      }

      if (this.filters.dateRange) {

        const txDate = new Date(tx.createdOn);

        const todayStart = new Date(now);
        todayStart.setHours(0, 0, 0, 0);

        const yesterdayStart = new Date(todayStart);
        yesterdayStart.setDate(todayStart.getDate() - 1);

        const weekStart = new Date(todayStart);
        weekStart.setDate(todayStart.getDate() - 7);

        const monthStart = new Date(todayStart);
        monthStart.setDate(todayStart.getDate() - 30);

        switch (this.filters.dateRange) {

          case 'today':
            if (txDate < todayStart) return false;
            break;

          case 'yesterday':
            if (txDate < yesterdayStart || txDate >= todayStart) return false;
            break;

          case 'week':
            if (txDate < weekStart) return false;
            break;

          case 'month':
            if (txDate < monthStart) return false;
            break;
        }
      }

      return true;
    });

    this.currentPage = 1;
  }

  resetFilters(): void {
    this.filters.dateRange = '';
    this.filters.transactionType = '';
    this.filters.status = '';
    this.applyFilters();
  }


  getOtherAccount(tx: any): string {
    return tx.transactionType === 'DEBIT' || tx.transactionType === 'SELF_TRANSFER'
      ? tx.toAccountId
      : tx.fromAccountId;
  }

  canRollback(tx: any): boolean {
    if (!tx || !tx.createdOn) return false;
    const txTime = new Date(tx.createdOn).getTime();
    const now = new Date().getTime();
    const diffMins = (now - txTime) / (1000 * 60);
    return diffMins <= 10;
  }

  // open dialog
  requestRollback(transactionId: string): void {
    this.pendingRollbackId = transactionId;
    this.showRollbackDialog = true;
  }

  // confirm dialog
  confirmRollback(): void {

    if (!this.pendingRollbackId) return;

    const rollbackId = this.pendingRollbackId;

    this.showRollbackDialog = false;
    this.pendingRollbackId = null;
    this.cdr.detectChanges();

    this.http.post(API.TRANSFERS.ROLLBACK(rollbackId), {}, { responseType: 'text' })
      .subscribe({
        next: () => {

          // Instant UI update (optimistic update)
          this.transactions = this.transactions.map(tx =>
            tx.id === rollbackId
              ? { ...tx, status: 'ROLLBACK_REQUESTED' }
              : tx
          );

          this.applyFilters();
          this.cdr.detectChanges();

          setTimeout(() => {
            this.reloadTransactions();
          }, 150);
        },

        error: (err) => {
          console.error(err);
          this.cdr.detectChanges();
        }
      });
  }

  // cancel dialog
  cancelRollback(): void {
    this.showRollbackDialog = false;
    this.pendingRollbackId = null;
  }

  get paginatedTransactions() {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredTransactions.slice(start, start + this.pageSize);
  }

  onPageChange(page: number) {
    this.currentPage = page;
  }
}
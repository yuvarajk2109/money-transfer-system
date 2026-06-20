import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { API } from '../../core/api';
import { PaginationComponent } from '../../shared/pagination/pagination';
import { Dropdown, DropdownOption } from '../../shared/dropdown/dropdown';
import { CustomDatePipe } from '../../shared/pipes/custom-date.pipe';

@Component({
  selector: 'app-admin-transactions',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, PaginationComponent, Dropdown, CustomDatePipe],
  templateUrl: './admin-transactions.html'
})
export class AdminTransactions implements OnInit {

  transactions: any[] = [];            // raw normalized
  filteredTransactions: any[] = [];    // filtered result

  // Filter model
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
    { label: 'Transfer', value: 'TRANSFER' },
    { label: 'Deposit', value: 'DEPOSIT' }
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

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.loadTransactions();
  }

  normalizeTransactions(transactions: any[]): any[] {

    const seenKeys = new Set<string>();
    const result: any[] = [];

    for (const tx of transactions) {

      // Deposits remain unchanged
      if (tx.transactionType === 'DEPOSIT') {
        result.push(tx);
        continue;
      }

      const keyBase = tx.idempotencyKey
        ?.replace('-DEBIT', '')
        .replace('-CREDIT', '');

      if (!keyBase) {
        result.push(tx);
        continue;
      }

      if (!seenKeys.has(keyBase)) {
        seenKeys.add(keyBase);

        result.push({
          ...tx,
          transactionType: 'TRANSFER'
        });
      }
    }

    return result;
  }

  loadTransactions(): void {
    this.http.get<any[]>(API.ADMIN.TRANSACTIONS)
      .subscribe({
        next: (data) => {
          this.transactions = this.normalizeTransactions(data);
          console.log('Loaded transactions:', this.transactions);
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

      // Type filter
      if (this.filters.transactionType &&
        tx.transactionType !== this.filters.transactionType) {
        return false;
      }

      // Status filter
      if (this.filters.status &&
        tx.status !== this.filters.status) {
        return false;
      }

      // Date filter
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


  formatAmount(amount: number): string {
    return Number(amount).toFixed(2);
  }

  get paginatedTransactions() {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredTransactions.slice(start, start + this.pageSize);
  }

  onPageChange(page: number) {
    this.currentPage = page;
  }
}
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';
import { RouterModule } from '@angular/router';
import { FormService } from '../../core/services/form.service';
import { PaginationComponent } from '../../shared/pagination/pagination';
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [RouterModule, CommonModule, PaginationComponent, ConfirmDialog],
  templateUrl: './approvals.html'
})
export class Approvals implements OnInit {

  accounts: any[] = [];
  loading = false;
  message = '';
  messageType = '';

  // modal
  showConfirm = false;
  confirmAction: 'approve' | 'reject' = 'approve';
  selectedAccountId: number = 0;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private form: FormService
  ) { }

  ngOnInit(): void {
    this.loadPending();
  }

  loadPending(): void {

    this.loading = true;

    this.http.get<any[]>(API.ADMIN.PENDING)
      .subscribe({
        next: (data) => {
          this.accounts = data;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.form.setError(this, err, 'Failed to load pending accounts');
          this.cdr.detectChanges();
        }
      });
  }

  openConfirm(id: number, action: 'approve' | 'reject') {
    this.selectedAccountId = id;
    this.confirmAction = action;
    this.showConfirm = true;
  }

  closeConfirm() {
    this.showConfirm = false;
  }

  confirmActionExecute(): void {
    if (this.confirmAction === 'approve') {
      this.approve(this.selectedAccountId);
    } else {
      this.reject(this.selectedAccountId);
    }
    this.showConfirm = false;
  }

  approve(id: number): void {

    if (this.loading) return;

    this.message = '';
    this.messageType = '';
    this.loading = true;

    this.http.post(API.ADMIN.APPROVE(id), {})
      .subscribe({
        next: () => {
          this.form.setSuccess(this, `Account ID ${id} approved successfully`);

          this.accounts = this.accounts.filter(a => a.id !== id);
          if (this.currentPage > 1 && this.paginatedAccounts.length === 0) {
            this.currentPage--;
          }
          this.loading = false;
          this.cdr.detectChanges();

          setTimeout(() => {
            this.loadPending();
          }, 200);
        },
        error: (err) => {
          this.form.setError(this, err, 'Approval failed');
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
  }

  reject(id: number): void {

    if (this.loading) return;

    this.message = '';
    this.messageType = '';
    this.loading = true;

    this.http.post(API.ADMIN.REJECT(id), {})
      .subscribe({
        next: () => {
          this.form.setSuccess(this, `Account ID ${id} rejected`);

          this.accounts = this.accounts.filter(a => a.id !== id);
          if (this.currentPage > 1 && this.paginatedAccounts.length === 0) {
            this.currentPage--;
          }
          this.loading = false;
          this.cdr.detectChanges();

          setTimeout(() => {
            this.loadPending();
          }, 200);
        },
        error: (err) => {
          this.form.setError(this, err, 'Reject failed');
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
  }

  pageSize = 10;
  currentPage = 1;

  get paginatedAccounts() {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.accounts.slice(start, start + this.pageSize);
  }

  onPageChange(page: number) {
    this.currentPage = page;
  }
}
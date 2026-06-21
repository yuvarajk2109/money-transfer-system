import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';
import { FormsModule } from '@angular/forms';
import { CustomDatePipe } from '../../shared/pipes/custom-date.pipe';
import { Dropdown, DropdownOption } from '../../shared/dropdown/dropdown';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule, FormsModule, CustomDatePipe, Dropdown],
  templateUrl: './rewards.html',
  styleUrl: './rewards.css'
})
export class Rewards implements OnInit {

  rewards: any[] = [];
  filteredRewards: any[] = [];
  summary: any = { totalPoints: 0, totalRewards: 0, revokedPoints: 0, usedPoints: 0, totalPointsLifetime: 0 };

  filters = {
    status: '',
    points: ''
  };

  statusOptions: DropdownOption[] = [
    { label: 'All', value: '' },
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Revoked', value: 'REVOKED' }
  ];

  pointsOptions: DropdownOption[] = [
    { label: 'All', value: '' },
    { label: 'Rewarded', value: 'REWARDED' },
    { label: 'Used', value: 'USED' }
  ];

  linkedAccounts: any[] = [];
  linkableAccounts: any[] = [];

  loading = true;
  linkMessage = '';
  linkMessageType = '';

  // Pagination
  currentPage = 1;
  pageSize = 10;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadRewards();
    this.loadSummary();
    this.loadLinkedAccounts();
    this.loadLinkableAccounts();
  }

  loadRewards(): void {
    this.loading = true;
    this.http.get<any[]>(API.REWARDS.GROUP).subscribe({
      next: (data) => {
        this.rewards = data;
        this.applyFilters();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadSummary(): void {
    this.http.get<any>(API.REWARDS.GROUP_TOTAL).subscribe({
      next: (data) => {
        this.summary = data;
        this.cdr.detectChanges();
      }
    });
  }

  loadLinkedAccounts(): void {
    this.http.get<any[]>(API.LINKED.LIST).subscribe({
      next: (data) => {
        this.linkedAccounts = data;
        this.cdr.detectChanges();
      }
    });
  }

  loadLinkableAccounts(): void {
    this.http.get<any[]>(API.LINKED.LINKABLE).subscribe({
      next: (data) => {
        this.linkableAccounts = data;
        this.cdr.detectChanges();
      }
    });
  }

  linkAccount(targetId: number): void {
    this.linkMessage = '';
    this.http.post<any>(API.LINKED.LINK(targetId), {}).subscribe({
      next: (res) => {
        this.linkMessage = res.message || 'Accounts linked successfully';
        this.linkMessageType = 'success';
        this.loadLinkedAccounts();
        this.loadLinkableAccounts();
        this.loadRewards();
        this.loadSummary();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.linkMessage = err.error?.message || 'Failed to link accounts';
        this.linkMessageType = 'error';
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    this.filteredRewards = this.rewards.filter(r => {

      if (this.filters.status === 'ACTIVE' && r.revoked) return false;
      if (this.filters.status === 'REVOKED' && !r.revoked) return false;

      if (this.filters.points === 'REWARDED' && r.points <= 0) return false;
      if (this.filters.points === 'USED' && r.points >= 0) return false;

      return true;
    });

    this.currentPage = 1;
  }

  resetFilters(): void {
    this.filters.status = '';
    this.filters.points = '';
    this.applyFilters();
  }

  // Pagination helpers
  get pagedRewards(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredRewards.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredRewards.length / this.pageSize);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
}

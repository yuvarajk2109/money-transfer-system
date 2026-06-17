import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rewards.html',
  styleUrl: './rewards.css'
})
export class Rewards implements OnInit {

  rewards: any[] = [];
  summary: any = { totalPoints: 0, totalRewards: 0, revokedPoints: 0 };

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

  // Pagination helpers
  get pagedRewards(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.rewards.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.rewards.length / this.pageSize);
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

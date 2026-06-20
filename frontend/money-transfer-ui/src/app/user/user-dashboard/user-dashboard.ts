import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { API } from '../../core/api';
import { AuthService } from '../../core/services/auth.service';
import { BalanceService } from '../../core/services/balance.service';
import { Profile } from '../profile/profile';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, Profile],
  templateUrl: './user-dashboard.html',
  styleUrl: './user-dashboard.css',
})
export class UserDashboard {

  profile: any = null;
  showProfileMenu = false;

  holderName = '';
  accountId = '';
  balance = '0.00';
  totalPoints = 0;
  
  singleAccountType = '';
  singleMinBalance = '0.00';

  myAccounts: any[] = [];
  hasMultipleAccounts = false;
  totalBalance = '0.00';
  accountCount = 0;


  constructor(
    private http: HttpClient,
    private router: Router,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
    private balanceService: BalanceService
  ) {}

  ngOnInit(): void {

    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    this.holderName = localStorage.getItem('holderName') || '';
    this.accountId = localStorage.getItem('accountId') || '';

    this.loadBalance();
    this.loadRewardPoints();
    this.loadMyAccounts();

    this.balanceService.balanceChanged$.subscribe(() => {
      this.loadBalance();
      this.loadMyAccounts();
    });
  }

  toggleProfileMenu() {
    this.showProfileMenu = !this.showProfileMenu;

    if (this.showProfileMenu && !this.profile) {
      this.loadProfile();
    }
  }

  loadProfile(): void {
    this.http.get<any>(
      API.ACCOUNTS.DETAILS(Number(this.accountId))
    ).subscribe({
      next: (data) => {
        this.profile = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load profile', err);
      }
    });
  }


  loadBalance(): void {
  this.http.get<any>(
    API.ACCOUNTS.BALANCE(Number(this.accountId))
  ).subscribe({
      next: (data) => {
        this.balance = Number(data.balance).toFixed(2);
        console.log('Balance loaded:', this.balance);
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        console.log(err);
      }
    });
  }


  loadRewardPoints(): void {
    this.http.get<any>(API.REWARDS.TOTAL).subscribe({
      next: (data) => {
        this.totalPoints = data.totalPoints || 0;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.log('Failed to load reward points', err);
      }
    });
  }


  loadMyAccounts(): void {
    this.http.get<any[]>(API.ACCOUNTS.MY_ACCOUNTS).subscribe({
      next: (data) => {
        this.myAccounts = data;
        this.accountCount = data.length;
        this.hasMultipleAccounts = data.length > 1;
        if (this.hasMultipleAccounts) {
          const total = data.reduce((sum: number, acc: any) => sum + Number(acc.balance), 0);
          this.totalBalance = total.toFixed(2);
        } else if (data.length === 1) {
          this.singleAccountType = data[0].accountType;
          this.singleMinBalance = Number(data[0].minBalance).toFixed(2);
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load accounts', err);
      }
    });
  }

  logout() {
    this.auth.logout();
  }

}
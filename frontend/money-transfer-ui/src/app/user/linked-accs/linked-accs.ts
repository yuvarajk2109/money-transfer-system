import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API } from '../../core/api';

@Component({
  selector: 'app-linked-accs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './linked-accs.html',
  styleUrl: './linked-accs.css'
})
export class LinkedAccs implements OnInit {

  myAccounts: any[] = [];
  loading = true;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadMyAccounts();
  }

  loadMyAccounts(): void {
    this.http.get<any[]>(API.ACCOUNTS.MY_ACCOUNTS).subscribe({
      next: (data) => {
        this.myAccounts = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load accounts', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}

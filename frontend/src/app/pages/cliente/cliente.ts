import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-cliente',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cliente.html',
  styleUrls: ['./cliente.scss']
})
export class Cliente implements OnInit {
  tarotistas: any[] = [];
  sesiones: any[] = [];
  usuario: any = null;
  error = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private zone: NgZone,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    const token = localStorage.getItem('token');
    if (!token) { this.router.navigate(['/']); return; }
    const headers = new HttpHeaders({ Authorization: 'Bearer ' + token });

    this.http.get('http://localhost:8080/api/tarotistas', { headers }).subscribe({
      next: (res: any) => {
        this.zone.run(() => {
          this.tarotistas = res.data || [];
        });
      },
      error: () => this.error = 'Error al cargar tarotistas'
    });

    this.http.get('http://localhost:8080/api/sesiones/mis-sesiones', { headers }).subscribe({
      next: (res: any) => {
        this.zone.run(() => {
          this.sesiones = res.data || [];
        });
      },
      error: () => {}
    });

    this.http.get('http://localhost:8080/api/auth/perfil', { headers }).subscribe({
      next: (res: any) => {
        this.zone.run(() => {
          this.usuario = res.data || res;
        });
      },
      error: () => {}
    });
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/']);
  }
}

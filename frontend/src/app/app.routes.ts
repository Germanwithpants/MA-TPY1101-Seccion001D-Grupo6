import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Cliente } from './pages/cliente/cliente';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'cliente', component: Cliente },
  { path: '**', redirectTo: '' }
];

import { Routes } from '@angular/router';
import { Muro } from './components/muro/muro';
import { Perfil } from './components/perfil/perfil';
import { MisIntercambios } from './components/mis-intercambios/mis-intercambios';
import { CrearOferta } from './components/crear-oferta/crear-oferta';
import { Login } from './components/login/login';
import { ForgotPassword } from './components/login/forgot-password';
import { Registro } from './components/registro/registro';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'forgot-password', component: ForgotPassword },
  { path: 'registro', component: Registro },
  { path: '', redirectTo: 'muro', pathMatch: 'full' },
  { path: 'muro', component: Muro, canActivate: [authGuard] },
  { path: 'perfil', component: Perfil, canActivate: [authGuard] },
  { path: 'mis-intercambios', component: MisIntercambios, canActivate: [authGuard] },
  { path: 'crear-oferta', component: CrearOferta, canActivate: [authGuard] },
  // ========== NUEVA RUTA PARA EDITAR OFERTA ==========
  { path: 'editar-oferta/:id', component: CrearOferta, canActivate: [authGuard] },
  // ====================================================
  { path: '**', redirectTo: 'muro' }
];
// Layout principal: barra lateral y menu de navegacion
import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterModule, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { NotificacionService } from './services/notificacion.service';
import { ThemeService } from './services/theme';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, RouterModule, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('proyecto-equipo-1');

  protected readonly themeService = inject(ThemeService);

  constructor(
    protected readonly auth: AuthService,
    protected readonly aviso: NotificacionService,
    private router: Router
  ) {}

  get isLoggedIn() {
    return this.auth.isAuthenticatedSignal();
  }

  get initials() {
    const name = this.auth.profile()?.name;
    if (!name) return '';
    return name
      .split(' ')
      .map((part) => part[0])
      .join('');
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}

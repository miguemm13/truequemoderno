// Pantalla de acceso al sistema
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class Login {
  email = '';
  password = '';
  errorMessage = '';

  constructor(private auth: AuthService, private router: Router, private cdr: ChangeDetectorRef) {}

  async onSubmit() {
    this.errorMessage = '';

    const result = await this.auth.login(this.email, this.password);
    if (result.success) {
      this.router.navigate(['/muro']);
    } else {
      this.errorMessage = result.message || 'Correo o contraseña incorrectos.';
      this.cdr.detectChanges();
    }
  }
}

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrls: ['./forgot-password.css']
})
export class ForgotPassword {
  email = '';
  preguntaObtenida = false;
  preguntaSeguridad = '';
  respuestaSeguridad = '';
  newPassword = '';
  confirmPassword = '';
  errorMessage = '';
  successMessage = '';

  constructor(private auth: AuthService, private router: Router, private cdr: ChangeDetectorRef) {}

  async buscarPregunta() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.email.trim()) {
      this.errorMessage = 'Ingresa tu correo para buscar la pregunta de seguridad.';
      this.cdr.detectChanges();
      return;
    }

    const result = await this.auth.getPreguntaSeguridad(this.email);
    if (result.success && result.pregunta) {
      this.preguntaSeguridad = result.pregunta;
      this.preguntaObtenida = true;
    } else {
      this.errorMessage = result.message || 'No se encontró la pregunta de seguridad para este correo.';
    }
    this.cdr.detectChanges();
  }

  async onSubmit() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.respuestaSeguridad.trim() || !this.newPassword.trim() || !this.confirmPassword.trim()) {
      this.errorMessage = 'Completa todos los campos para restablecer la contraseña.';
      this.cdr.detectChanges();
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden.';
      this.cdr.detectChanges();
      return;
    }

    const result = await this.auth.resetPassword(this.email, this.respuestaSeguridad, this.newPassword);
    if (!result.success) {
      this.errorMessage = result.message;
      this.cdr.detectChanges();
      return;
    }

    this.successMessage = result.message;
    this.preguntaObtenida = false;
    this.email = '';
    this.respuestaSeguridad = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.cdr.detectChanges();
  }
}

// Alta de nuevos miembros en la plataforma
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrls: ['./registro.css']
})
export class Registro {
  name = '';
  email = '';
  password = '';
  confirmPassword = '';
  preguntaSeguridad = '';
  respuestaSeguridad = '';
  errorMessage = '';
  telefono = '';

  constructor(private auth: AuthService, private router: Router, private cdr: ChangeDetectorRef) {}

  async onSubmit() {
    this.errorMessage = '';

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.preguntaSeguridad.trim() || !this.respuestaSeguridad.trim()) {
      this.errorMessage = 'La pregunta y respuesta de seguridad son obligatorias.';
      this.cdr.detectChanges();
      return;
    }

    const result = await this.auth.register(this.name, this.email, this.password, this.preguntaSeguridad, this.respuestaSeguridad, this.telefono );
    if (!result.success) {
      this.errorMessage = result.message;
      this.cdr.detectChanges();
      return;
    }

    this.router.navigate(['/login']);
  }
}

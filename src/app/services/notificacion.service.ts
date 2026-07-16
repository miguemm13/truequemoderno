import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  visible = signal(false);
  mensaje = signal('');
  tipo = signal<'ok' | 'error'>('ok');

  mostrar(texto: string, tipo: 'ok' | 'error' = 'ok', duracionMs = 4000) {
    this.mensaje.set(texto);
    this.tipo.set(tipo);
    this.visible.set(true);
    setTimeout(() => this.visible.set(false), duracionMs);
  }
}

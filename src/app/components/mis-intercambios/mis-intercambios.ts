// Bandeja de intercambios: peticiones recibidas y solicitudes hechas
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { normalizarEstado, EstadoTransaccion } from '../../utils/estados';
import { NotificacionService } from '../../services/notificacion.service';

interface Transaccion {
  idTransaccion: string;
  ofertaTitulo: string;
  nombreContraparte: string;
  telefonoContraparte?: string;  // <-- Nueva propiedad
  horasPactadas: number;
  estado: EstadoTransaccion;
  esPrestador: boolean;
  yaCalificado?: boolean;
}

@Component({
  selector: 'app-mis-intercambios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="panel-container">
      <header class="panel-header">
        <h1>Panel de Intercambios</h1>
        <p>Gestiona tus servicios activos, peticiones pendientes y calificaciones del ecosistema.</p>
      </header>

      <!-- Pestañas de Navegación -->
      <div class="tabs-container">
        <nav class="tabs-nav" aria-label="Tabs">
          <button 
            (click)="pestanaActiva = 'recibidos'"
            [class.tab-active]="pestanaActiva === 'recibidos'"
            class="tab-btn">
            Peticiones Recibidas (Como Prestador)
          </button>
          <button 
            (click)="pestanaActiva = 'realizados'"
            [class.tab-active]="pestanaActiva === 'realizados'"
            class="tab-btn">
            Solicitudes Realizadas (Como Receptor)
          </button>
        </nav>
      </div>

      <!-- BANDEJA 1: PETICIONES RECIBIDAS -->
      <div *ngIf="pestanaActiva === 'recibidos'">
        <div class="list-wrapper">
          <ul class="intercambios-list">
            <li *ngFor="let t of getRecibidos()" class="tarjeta-intercambio">
              <div class="info-intercambio">
                <h3>{{ t.ofertaTitulo }}</h3>
                <p class="meta-user">Solicitado por: <strong>{{ t.nombreContraparte }}</strong></p>
                <p class="meta-time">Tiempo estimado: <span class="destaque-horas">{{ t.horasPactadas }} horas</span></p>
                <!-- Mostrar teléfono solo si existe y el estado es Iniciada o Finalizado con éxito -->
                <p class="meta-contact" *ngIf="t.telefonoContraparte && (t.estado === 'Iniciada' || t.estado === 'Finalizado con éxito')">
                  📞 Contacto: <strong>{{ t.telefonoContraparte }}</strong>
                </p>
              </div>
              
              <div class="acciones-intercambio">
                <span class="badge" [ngClass]="{
                  'badge-solicitada': t.estado === 'Solicitada',
                  'badge-iniciada': t.estado === 'Iniciada',
                  'badge-exito': t.estado === 'Finalizado con éxito',
                  'badge-cancelada': t.estado === 'Cancelada'
                }">
                  {{ t.estado }}
                </span>

                <div *ngIf="t.estado === 'Solicitada'" class="btn-group">
                  <button (click)="gestionarPeticion(t.idTransaccion, 'aceptar')" class="btn-panel btn-accept">
                    Aceptar
                  </button>
                  <button (click)="gestionarPeticion(t.idTransaccion, 'rechazar')" class="btn-panel btn-reject">
                    Rechazar
                  </button>
                </div>

                <div *ngIf="t.estado === 'Finalizado con éxito' && !t.yaCalificado">
                  <button (click)="abrirModalCalificar(t)" class="btn-panel btn-rating">
                    Calificar Miembro
                  </button>
                </div>
              </div>
            </li>
            <li *ngIf="getRecibidos().length === 0" class="lista-vacia">
              No tienes propuestas recibidas en este momento.
            </li>
          </ul>
        </div>
      </div>

      <!-- BANDEJA 2: SOLICITUDES REALIZADAS -->
      <div *ngIf="pestanaActiva === 'realizados'">
        <div class="list-wrapper">
          <ul class="intercambios-list">
            <li *ngFor="let t of getRealizados()" class="tarjeta-intercambio">
              <div class="info-intercambio">
                <h3>{{ t.ofertaTitulo }}</h3>
                <p class="meta-user">Prestador: <strong>{{ t.nombreContraparte }}</strong></p>
                <p class="meta-time">Costo contable: <span class="destaque-horas">{{ t.horasPactadas }} horas crédito</span></p>
                <!-- Mostrar teléfono solo si existe y el estado es Iniciada o Finalizado con éxito -->
                <p class="meta-contact" *ngIf="t.telefonoContraparte && (t.estado === 'Iniciada' || t.estado === 'Finalizado con éxito')">
                  📞 Contacto: <strong>{{ t.telefonoContraparte }}</strong>
                </p>
              </div>
              
              <div class="acciones-intercambio">
                <span class="badge" [ngClass]="{
                  'badge-solicitada': t.estado === 'Solicitada',
                  'badge-iniciada': t.estado === 'Iniciada',
                  'badge-exito': t.estado === 'Finalizado con éxito',
                  'badge-cancelada': t.estado === 'Cancelada'
                }">
                  {{ t.estado }}
                </span>

                <div *ngIf="t.estado === 'Iniciada'" class="finalizar-block">
                  <button (click)="abrirMiniCalificacion(t)" class="btn-panel btn-success">
                    Terminado
                  </button>
                  <div *ngIf="finalizandoId === t.idTransaccion" class="mini-rating">
                    <p class="mini-rating-label"><strong>{{ t.ofertaTitulo }}</strong></p>
                    <p class="mini-rating-sub">Elige de 1 a 5 estrellas</p>
                    <div class="stars-row">
                      <button *ngFor="let estrella of [1,2,3,4,5]" (click)="notaCalificacion = estrella" type="button" class="star-btn">
                        <svg [class.star-filled]="estrella <= notaCalificacion" class="star-icon" viewBox="0 0 24 24">
                          <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
                        </svg>
                      </button>
                    </div>
                    <div class="mini-rating-actions">
                      <button (click)="cancelarMiniCalificacion()" class="btn-panel btn-reject">Cancelar</button>
                      <button (click)="confirmarFinalizacionConEstrellas()" [disabled]="notaCalificacion === 0" class="btn-panel btn-success">
                        Confirmar
                      </button>
                    </div>
                  </div>
                </div>

                <div *ngIf="t.estado === 'Finalizado con éxito' && !t.yaCalificado">
                  <button (click)="abrirModalCalificar(t)" class="btn-panel btn-rating">
                    Calificar Miembro
                  </button>
                </div>
              </div>
            </li>
            <li *ngIf="getRealizados().length === 0" class="lista-vacia">
              No has realizado solicitudes de trueque aún.
            </li>
          </ul>
        </div>
      </div>

      <!-- MODAL DE CALIFICACIÓN -->
      <div *ngIf="modalAbierto" class="modal-overlay">
        <div class="modal-card">
          <h3>¿Cómo calificas este trueque?</h3>
          <p class="modal-description" *ngIf="transaccionSeleccionada">
            <strong>{{ transaccionSeleccionada.ofertaTitulo }}</strong> — {{ transaccionSeleccionada.nombreContraparte }}
          </p>
          <div class="stars-row">
            <button *ngFor="let estrella of [1,2,3,4,5]" (click)="notaCalificacion = estrella" type="button" class="star-btn">
              <svg [class.star-filled]="estrella <= notaCalificacion" class="star-icon" viewBox="0 0 24 24">
                <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
              </svg>
            </button>
          </div>
          <div class="modal-actions">
            <button (click)="cerrarModal()" class="btn-panel btn-cancel">Cancelar</button>
            <button (click)="enviarCalificacion()" [disabled]="notaCalificacion === 0" class="btn-panel btn-save">
              Confirmar
            </button>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .panel-container {
      width: 100%;
      font-family: inherit;
    }
    .panel-header h1 {
      font-size: 1.8rem;
      font-weight: 800;
      color: #111827;
      margin: 0 0 4px 0;
    }
    .panel-header p {
      color: #6b7280;
      font-size: 0.95rem;
      margin: 0 0 24px 0;
    }
    .tabs-container {
      border-b: 1px solid #e5e7eb;
      margin-bottom: 24px;
    }
    .tabs-nav {
      display: flex;
      gap: 24px;
      border-bottom: 2px solid #e5e7eb;
    }
    .tab-btn {
      background: none;
      border: none;
      padding: 12px 4px;
      font-size: 0.9rem;
      font-weight: 600;
      color: #6b7280;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      margin-bottom: -2px;
      transition: all 0.2s;
    }
    .tab-btn:hover {
      color: #374151;
    }
    .tab-active {
      color: #4f46e5;
      border-bottom-color: #4f46e5;
    }
    .list-wrapper {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.05);
      overflow: hidden;
    }
    .intercambios-list {
      list-style: none;
      padding: 0;
      margin: 0;
    }
    .tarjeta-intercambio {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid #e5e7eb;
      flex-wrap: wrap;
      gap: 16px;
    }
    .tarjeta-intercambio:last-child {
      border-bottom: none;
    }
    .info-intercambio h3 {
      font-size: 1.1rem;
      font-weight: 600;
      color: #111827;
      margin: 0 0 6px 0;
    }
    .meta-user {
      font-size: 0.875rem;
      color: #4b5563;
      margin: 0 0 4px 0;
    }
    .meta-time {
      font-size: 0.85rem;
      color: #6b7280;
      margin: 0;
    }
    .meta-contact {
      font-size: 0.85rem;
      color: #4b5563;
      margin: 4px 0 0 0;
    }
    .destaque-horas {
      font-weight: 600;
      color: #4f46e5;
    }
    .acciones-intercambio {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .btn-group {
      display: flex;
      gap: 8px;
    }
    .btn-panel {
      padding: 8px 14px;
      font-size: 0.825rem;
      font-weight: 600;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      border: none;
    }
    .btn-accept {
      background: #4f46e5;
      color: #ffffff;
    }
    .btn-accept:hover { background: #4338ca; }
    .btn-reject {
      background: #ffffff;
      border: 1px solid #d1d5db;
      color: #374151;
    }
    .btn-reject:hover { background: #f9fafb; }
    .btn-success {
      background: #10b981;
      color: #ffffff;
    }
    .btn-success:hover { background: #059669; }
    .btn-rating {
      background: #f59e0b;
      color: #ffffff;
    }
    .btn-rating:hover { background: #d97706; }
    
    /* Badges de Estado */
    .badge {
      padding: 4px 10px;
      font-size: 0.75rem;
      font-weight: 600;
      border-radius: 9999px;
    }
    .badge-solicitada { background: #fef3c7; color: #d97706; }
    .badge-iniciada { background: #dbeafe; color: #2563eb; }
    .badge-exito { background: #d1fae5; color: #059669; }
    .badge-cancelada { background: #fee2e2; color: #dc2626; }
    
    .lista-vacia {
      padding: 40px;
      text-align: center;
      color: #9ca3af;
      font-size: 0.95rem;
    }

    /* Modal Styles */
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 16px;
    }
    .modal-card {
      background: #ffffff;
      border-radius: 12px;
      padding: 24px;
      max-w: 440px;
      width: 100%;
      box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1);
    }
    .modal-card h3 { margin: 0 0 8px 0; font-size: 1.25rem; font-weight: 700; }
    .modal-description { color: #6b7280; font-size: 0.875rem; margin: 0 0 20px 0; }
    .form-group { margin-bottom: 16px; }
    .form-group label { display: block; font-size: 0.875rem; font-weight: 600; color: #374151; margin-bottom: 6px; }
    .stars-row { display: flex; gap: 4px; }
    .star-btn { background: none; border: none; cursor: pointer; padding: 0; }
    .star-icon { width: 32px; height: 32px; fill: #e5e7eb; transition: fill 0.2s; }
    .star-filled { fill: #fbbf24; }
    .modal-textarea {
      width: 100%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 8px;
      padding: 10px; font-size: 0.875rem; outline: none; resize: vertical;
    }
    .modal-textarea:focus { border-color: #4f46e5; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
    .btn-cancel { background: #ffffff; border: 1px solid #d1d5db; color: #374151; }
    .btn-cancel:hover { background: #f9fafb; }
    .btn-save { background: #4f46e5; color: #ffffff; }
    .btn-save:hover { background: #4338ca; }
    .btn-save:disabled { background: #e5e7eb; color: #9ca3af; cursor: not-allowed; }

    .finalizar-block { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
    .mini-rating {
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 10px 12px;
      text-align: center;
    }
    .mini-rating-label { margin: 0 0 4px 0; font-size: 0.9rem; color: #111827; }
    .mini-rating-sub { margin: 0 0 8px 0; font-size: 0.75rem; color: #6b7280; }
    .mini-rating-actions { display: flex; gap: 8px; margin-top: 8px; justify-content: center; }
  `]
})
export class MisIntercambios implements OnInit {
  pestanaActiva: 'recibidos' | 'realizados' = 'recibidos';
  
  transacciones: Transaccion[] = [];
  finalizandoId: string | null = null;

  modalAbierto = false;
  transaccionSeleccionada: Transaccion | null = null;
  notaCalificacion = 0;
  constructor(
    private auth: AuthService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private aviso: NotificacionService
  ) {}

  async ngOnInit() {
    await this.cargarTransacciones();
  }

  async cargarTransacciones() {
    const currentUser = this.auth.userProfile;
    if (!currentUser) return;

    try {
      // Fetch user's transactions
      const txs = await firstValueFrom(this.http.get<any[]>(`http://localhost:8080/api/transacciones/usuario?idUsuario=${currentUser.id}`));
      
      // Fetch all offers to map titles
      const ofertas = await firstValueFrom(this.http.get<any[]>('http://localhost:8080/api/ofertas/listar'));
      const ofertasMap = new Map<string, string>();
      ofertas.forEach(o => ofertasMap.set(o.id_oferta, o.titulo));

      // Fetch all members to map names and phone numbers
      const miembros = await firstValueFrom(this.http.get<any[]>('http://localhost:8080/api/miembros/listar'));
      const miembrosMap = new Map<string, { nombre: string, telefono: string }>();
      miembros.forEach(m => miembrosMap.set(m.id, { nombre: m.nombre, telefono: m.telefono || '' }));

      // Fetch user's submitted evaluations to mark yaCalificado
      let califTxIds = new Set<string>();
      try {
        const calificaciones = await firstValueFrom(
          this.http.get<any[]>(`http://localhost:8080/api/reputacion/usuario?idEvaluador=${currentUser.id}`)
        );
        if (calificaciones && Array.isArray(calificaciones)) {
          calificaciones.forEach(c => {
            if (c.id_transaccion) {
              califTxIds.add(c.id_transaccion);
            }
          });
        }
      } catch (err) {
        console.error('Error fetching user evaluations:', err);
      }

      this.transacciones = txs.map(t => {
        const soyPrestador = t.id_prestador === currentUser.id;
        const idContraparte = soyPrestador ? t.id_receptor : t.id_prestador;
        const info = miembrosMap.get(idContraparte) || { nombre: idContraparte, telefono: '' };
        return {
          idTransaccion: t.id_transaccion,
          ofertaTitulo: ofertasMap.get(t.id_oferta) || 'Oferta Desconocida',
          nombreContraparte: info.nombre,
          telefonoContraparte: info.telefono,  // <-- Asignar teléfono
          horasPactadas: t.horas,
          estado: normalizarEstado(t.estado),
          esPrestador: soyPrestador,
          yaCalificado: califTxIds.has(t.id_transaccion)
        };
      });

      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error cargando transacciones:', error);
    }
  }

  getRecibidos(): Transaccion[] {
    return this.transacciones.filter(t => t.esPrestador);
  }

  getRealizados(): Transaccion[] {
    return this.transacciones.filter(t => !t.esPrestador);
  }

  async gestionarPeticion(id: string, accion: 'aceptar' | 'rechazar') {
    const currentUser = this.auth.userProfile;
    if (!currentUser) return;

    try {
      const payload = {
        idPrestador: currentUser.id,
        idTrans: id,
        decision: accion === 'aceptar' ? 'Aceptar' : 'Rechazar'
      };

      const txActualizada = await firstValueFrom(this.http.post<any>('http://localhost:8080/api/transacciones/gestionar', payload));
      
      // Update locally
      const tx = this.transacciones.find(t => t.idTransaccion === id);
      if (tx) {
        tx.estado = normalizarEstado(txActualizada.estado);
      }
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error gestionando peticion', error);
      this.aviso.mostrar('No se pudo procesar la petición.', 'error');
    }
  }

  abrirMiniCalificacion(tx: Transaccion) {
    this.finalizandoId = tx.idTransaccion;
    this.transaccionSeleccionada = tx;
    this.notaCalificacion = 0;
    this.cdr.detectChanges();
  }

  cancelarMiniCalificacion() {
    this.finalizandoId = null;
    this.transaccionSeleccionada = null;
    this.notaCalificacion = 0;
    this.cdr.detectChanges();
  }

  async confirmarFinalizacionConEstrellas() {
    const id = this.finalizandoId;
    if (!id || this.notaCalificacion < 1) return;

    const currentUser = this.auth.userProfile;
    if (!currentUser) return;

    try {
      const payload = { idMiembroSesion: currentUser.id, idTrans: id };
      const confirmRes = await firstValueFrom(
        this.http.post<{ saldoActualizado?: number; transaccion?: { estado: string } }>(
          'http://localhost:8080/api/transacciones/confirmar',
          payload
        )
      );

      if (confirmRes.saldoActualizado !== undefined) {
        this.auth.actualizarSaldo(confirmRes.saldoActualizado);
      }

      await this.enviarCalificacionAlServidor(id, this.notaCalificacion);

      const tx = this.transacciones.find(t => t.idTransaccion === id);
      if (tx && confirmRes.transaccion?.estado) {
        tx.estado = normalizarEstado(confirmRes.transaccion.estado);
        tx.yaCalificado = true;
      }

      this.cancelarMiniCalificacion();
      this.aviso.mostrar('Calificación guardada. Trueque finalizado.');
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error confirmando finalizacion', error);
      this.aviso.mostrar('No se pudo finalizar. Revisa tu saldo de horas.', 'error');
    }
  }

  abrirModalCalificar(tx: Transaccion) {
    this.transaccionSeleccionada = tx;
    this.notaCalificacion = 0;
    this.modalAbierto = true;
  }

  cerrarModal() {
    this.modalAbierto = false;
    this.transaccionSeleccionada = null;
  }

  async enviarCalificacion() {
    if (!this.transaccionSeleccionada || this.notaCalificacion < 1) return;

    try {
      await this.enviarCalificacionAlServidor(
        this.transaccionSeleccionada.idTransaccion,
        this.notaCalificacion
      );
      this.transaccionSeleccionada.yaCalificado = true;
      this.cerrarModal();
      this.aviso.mostrar('Gracias, tu calificación quedó registrada.');
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error enviando calificacion', error);
      this.aviso.mostrar('No se pudo guardar la calificación.', 'error');
    }
  }

  private async enviarCalificacionAlServidor(idTrans: string, estrellas: number) {
    const currentUser = this.auth.userProfile;
    if (!currentUser) return;

    const payload = {
      idEvaluador: currentUser.id,
      idTrans,
      estrellas,
      comentario: '-'
    };
    await firstValueFrom(this.http.post('http://localhost:8080/api/reputacion/calificar', payload));
  }
}
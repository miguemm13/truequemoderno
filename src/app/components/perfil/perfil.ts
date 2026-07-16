// Panel del usuario: ofertas propias, solicitudes enviadas y peticiones por aceptar
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, UserProfile } from '../../services/auth.service';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ChangeDetectorRef } from '@angular/core';
import { ofertaActiva } from '../../utils/estados';
import { CATEGORIAS_MAP } from '../../utils/categorias';
import { NotificacionService } from '../../services/notificacion.service';

interface Habilidad {
  nombre: string;
  nivel: 'Básico' | 'Intermedio' | 'Avanzado' | 'Experto';
}

interface OfertaPropia {
  id: string;
  titulo: string;
  horas: number;
  categoria: string;
}

interface TransaccionCurso {
  idTransaccion: string;
  ofertaTitulo: string;
  nombreContraparte: string;
  telefonoContraparte?: string;   // <-- NUEVO: teléfono de la contraparte
  horasPactadas: number;
  esReceptor: boolean;
  estado?: string;
  yaCalificado?: boolean;
}

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="panel-container">
      <header class="panel-header">
        <h1>Mi Perfil de Usuario</h1>
        <p>Gestiona tus datos, habilidades ofertadas y configuraciones de la Billetera de Tiempo.</p>
      </header>

      <div class="profile-grid">
        <!-- Información Personal -->
        <section class="profile-section card">
          <h2 (click)="seccionInformacionAbierta = !seccionInformacionAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">👤</span> Información Personal</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionInformacionAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionInformacionAbierta" style="margin-top: 16px;">
            <div class="info-group">
              <label>Nombre Completo</label>
              <div class="value-display">{{ user?.name || 'Cargando...' }}</div>
            </div>
            <div class="info-group">
              <label>Correo</label>
              <div class="value-display">{{ user?.email || 'Cargando...' }}</div>
            </div>
            <div class="info-group">
              <label>Teléfono</label>
              <div class="value-display">{{ user?.telefono || 'No registrado' }}</div>
            </div>
            <div class="info-group">
              <label>Billetera de Horas (Saldo)</label>
              <div class="value-display" style="color: #4338ca; font-weight: 700;">{{ user?.balance | number:'1.2-2' }} h</div>
            </div>
            <div class="info-group">
              <label>Reputación</label>
              <div class="value-display">⭐ {{ user?.reputation | number:'1.1-1' }} / 5.0</div>
            </div>
          </div>
        </section>

        <!-- Habilidades Ofertadas -->
        <section class="profile-section card">
          <h2 (click)="seccionHabilidadesAbierta = !seccionHabilidadesAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">⚡</span> Mis Habilidades Ofertadas</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionHabilidadesAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionHabilidadesAbierta" style="margin-top: 16px;">
            <p class="section-desc">Estas etiquetas definen en qué áreas puedes prestar soporte dentro de la plataforma.</p>
            
            <div class="tags-container">
              <div *ngFor="let hab of habilidades; let i = index" class="skill-tag">
                <span>{{ hab.nombre }} <small>({{ hab.nivel }})</small></span>
                <button (click)="eliminarHabilidad(i)" class="btn-delete-tag" title="Eliminar">&times;</button>
              </div>
              <p *ngIf="habilidades.length === 0" class="text-muted">No has registrado habilidades aún.</p>
            </div>

            <div class="add-skill-form">
              <input 
                type="text" 
                [(ngModel)]="nuevaHabilidadNombre" 
                placeholder="Ej. Angular, Figma, Cálculo..."
                class="form-control">
              
              <select [(ngModel)]="nuevaHabilidadNivel" class="form-control select-control">
                <option value="Básico">Básico</option>
                <option value="Intermedio">Intermedio</option>
                <option value="Avanzado">Avanzado</option>
                <option value="Experto">Experto</option>
              </select>

              <button (click)="agregarHabilidad()" [disabled]="!nuevaHabilidadNombre.trim()" class="btn-panel btn-add">
                Añadir
              </button>
            </div>
          </div>
        </section>

        <!-- Modificación de Perfil (Columna 1) -->
        <section class="profile-section card">
          <h2 (click)="seccionModificarAbierta = !seccionModificarAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">⚙️</span> Modificar Perfil</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionModificarAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionModificarAbierta" style="margin-top: 16px;">
            <p class="section-desc">Actualiza tu contraseña o tu pregunta de seguridad aquí.</p>
            
            <form (ngSubmit)="actualizarPerfil()" class="edit-profile-form">
              <div class="form-group-edit">
                <label for="modNuevaClave">Nueva Contraseña</label>
                <input 
                  id="modNuevaClave"
                  type="password" 
                  name="nuevaClave"
                  [(ngModel)]="nuevaClave" 
                  placeholder="Mínimo 6 caracteres"
                  class="form-control">
              </div>
              
              <div class="form-group-edit">
                <label for="modConfirmarClave">Confirmar Contraseña</label>
                <input 
                  id="modConfirmarClave"
                  type="password" 
                  name="confirmarClave"
                  [(ngModel)]="confirmarClave" 
                  placeholder="Repite la contraseña"
                  class="form-control">
              </div>
              
              <div class="form-group-edit">
                <label for="modPregunta">Pregunta de Seguridad</label>
                <select 
                  id="modPregunta"
                  name="preguntaSeguridad"
                  [(ngModel)]="preguntaSeguridad" 
                  class="form-control">
                  <option value="">Selecciona una pregunta</option>
                  <option value="¿Cuál es el nombre de tu primera mascota?">¿Cuál es el nombre de tu primera mascota?</option>
                  <option value="¿En qué ciudad naciste?">¿En qué ciudad naciste?</option>
                  <option value="¿Cuál es el nombre de tu escuela primaria?">¿Cuál es el nombre de tu escuela primaria?</option>
                  <option value="¿Cuál es tu comida favorita?">¿Cuál es tu comida favorita?</option>
                  <option value="¿Cuál es el nombre de soltera de tu madre?">¿Cuál es el nombre de soltera de tu madre?</option>
                </select>
              </div>
              
              <div class="form-group-edit">
                <label for="modRespuesta">Respuesta de Seguridad</label>
                <input 
                  id="modRespuesta"
                  type="password" 
                  name="respuestaSeguridad"
                  [(ngModel)]="respuestaSeguridad" 
                  placeholder="Tu nueva respuesta"
                  class="form-control">
              </div>
              
              <button type="submit" class="btn-panel btn-add" style="margin-top: 12px; width: 100%;">
                Actualizar Perfil
              </button>
            </form>
          </div>
        </section>

        <!-- Eliminar Cuenta (Columna 2) -->
        <section class="profile-section card">
          <h2 (click)="seccionEliminarAbierta = !seccionEliminarAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">⚠️</span> Eliminar Perfil</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionEliminarAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionEliminarAbierta" style="margin-top: 16px;">
            <p class="section-desc" style="color: #ef4444;">Esta acción es irreversible. Se borrarán tus datos y publicaciones del muro.</p>
            
            <div class="delete-profile-form">
              <div class="form-group-edit">
                <label for="modEliminarConf">Para confirmar, escribe <strong>ELIMINAR</strong> en mayúsculas:</label>
                <input 
                  id="modEliminarConf"
                  type="text" 
                  name="confirmacionEliminar"
                  [(ngModel)]="confirmacionEliminar" 
                  placeholder="Escribe ELIMINAR"
                  class="form-control"
                  style="border-color: #fca5a5;">
              </div>
              
              <button 
                (click)="eliminarPerfil()" 
                [disabled]="confirmacionEliminar !== 'ELIMINAR'" 
                class="btn-panel btn-danger" 
                style="margin-top: 12px; width: 100%;">
                Confirmar Eliminación
              </button>
            </div>
          </div>
        </section>

        <!-- Ofertas Publicadas -->
        <section class="profile-section card full-width">
          <h2 (click)="seccionOfertasAbierta = !seccionOfertasAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">📋</span> Mis Ofertas Publicadas</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionOfertasAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionOfertasAbierta" style="margin-top: 16px;">
            <div class="list-wrapper">
              <div *ngFor="let ofr of misOfertas" class="oferta-item">
                <div class="oferta-info">
                  <h3>{{ ofr.titulo }}</h3>
                  <span class="categoria-badge">{{ ofr.categoria }}</span>
                </div>
                <div class="oferta-meta">
                  <span class="horas-badge">{{ ofr.horas }} h</span>
                  <button (click)="editarOferta(ofr.id)" class="btn-panel btn-secondary">Editar</button>
                  <button (click)="darDeBajaOferta(ofr.id)" class="btn-panel btn-secondary">Dar de baja</button>
                </div>
              </div>
              <div *ngIf="misOfertas.length === 0" class="lista-vacia">
                No tienes ofertas activas en el muro actualmente.
              </div>
            </div>
          </div>
        </section>

        <!-- Intercambios en Curso -->
        <section class="profile-section card full-width">
          <h2 (click)="seccionIntercambiosAbierta = !seccionIntercambiosAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">⏳</span> Mis Intercambios en Curso</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionIntercambiosAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionIntercambiosAbierta" style="margin-top: 16px;">
            <div class="list-wrapper">
              <div *ngFor="let tx of intercambiosEnCurso" class="oferta-item">
                <div class="oferta-info">
                  <h3>{{ tx.ofertaTitulo }}</h3>
                  <span *ngIf="tx.estado !== 'Finalizado con exito'" class="categoria-badge" style="background: #dbeafe; color: #2563eb;">En Progreso</span>
                  <span *ngIf="tx.estado === 'Finalizado con exito'" class="categoria-badge" style="background: #d1fae5; color: #059669;">Finalizado</span>
                  <p style="font-size: 0.85rem; color: #6b7280; margin: 4px 0 0 0;">
                    {{ tx.esReceptor ? 'Prestador' : 'Solicitante' }}: {{ tx.nombreContraparte }}
                  </p>
                  <!-- 👇 NUEVO: Mostrar teléfono si existe -->
                  <p *ngIf="tx.telefonoContraparte" style="font-size: 0.85rem; color: #0b5e7c; margin: 4px 0 0 0; background: #e0f2fe; padding: 2px 8px; border-radius: 4px; display: inline-block;">
                    📞 Contacto: {{ tx.telefonoContraparte }}
                  </p>
                </div>
                <div class="oferta-meta">
                  <span class="horas-badge">{{ tx.horasPactadas.toFixed(1) }} h</span>
                  
                  <ng-container *ngIf="tx.estado !== 'Finalizado con exito'">
                    <button *ngIf="tx.esReceptor" (click)="abrirCalificacionAlCompletar(tx)" class="btn-panel btn-add">Terminado</button>
                    <span *ngIf="!tx.esReceptor" style="font-size: 0.8rem; color: #6b7280;">Esperando que el solicitante confirme</span>
                  </ng-container>

                  <ng-container *ngIf="tx.estado === 'Finalizado con exito' && !tx.yaCalificado">
                    <button (click)="abrirCalificacionAlCompletar(tx)" class="btn-panel btn-add" style="background: #f59e0b; color: white;">Calificar Miembro</button>
                  </ng-container>
                </div>
              </div>
              <div *ngIf="intercambiosEnCurso.length === 0" class="lista-vacia">
                No tienes intercambios en curso.
              </div>
            </div>
          </div>
        </section>

        <!-- Solicitudes Realizadas -->
        <section class="profile-section card full-width">
          <h2 (click)="seccionSolicitudesAbierta = !seccionSolicitudesAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">📨</span> Solicitudes Realizadas (Pendientes)</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionSolicitudesAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionSolicitudesAbierta" style="margin-top: 16px;">
            <div class="list-wrapper">
              <div *ngFor="let tx of solicitudesRealizadas" class="oferta-item">
                <div class="oferta-info">
                  <h3>{{ tx.ofertaTitulo }}</h3>
                  <span class="categoria-badge" style="background: #fef3c7; color: #d97706;">Solicitada</span>
                  <p style="font-size: 0.85rem; color: #6b7280; margin: 4px 0 0 0;">Prestador: {{ tx.nombreContraparte }}</p>
                  <p style="font-size: 0.8rem; color: #9ca3af; margin: 4px 0 0 0;">Debe aceptar o rechazar tu solicitud</p>
                </div>
                <div class="oferta-meta">
                  <span class="horas-badge">{{ tx.horasPactadas.toFixed(1) }} h</span>
                </div>
              </div>
              <div *ngIf="solicitudesRealizadas.length === 0" class="lista-vacia">
                No tienes solicitudes pendientes.
              </div>
            </div>
          </div>
        </section>

        <!-- Peticiones Recibidas -->
        <section class="profile-section card full-width">
          <h2 (click)="seccionPeticionesAbierta = !seccionPeticionesAbierta" style="cursor: pointer; display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <span><span class="icon">🔔</span> Peticiones Recibidas (Como Prestador)</span>
            <span style="font-size: 0.95rem; color: #4f46e5;">{{ seccionPeticionesAbierta ? '▲' : '▼' }}</span>
          </h2>
          <div *ngIf="seccionPeticionesAbierta" style="margin-top: 16px;">
            <div class="list-wrapper">
              <div *ngFor="let tx of peticionesRecibidas" class="oferta-item">
                <div class="oferta-info">
                  <h3>{{ tx.ofertaTitulo }}</h3>
                  <span class="categoria-badge" style="background: #fce7f3; color: #be185d;">Requiere Atención</span>
                  <p style="font-size: 0.85rem; color: #6b7280; margin: 4px 0 0 0;">Solicitado por: {{ tx.nombreContraparte }}</p>
                </div>
                <div class="oferta-meta">
                  <span class="horas-badge">{{ tx.horasPactadas.toFixed(1) }} h</span>
                  <div style="display: flex; gap: 8px;">
                    <button (click)="gestionarPeticion(tx.idTransaccion, 'aceptar')" class="btn-panel btn-add">Aceptar</button>
                    <button (click)="gestionarPeticion(tx.idTransaccion, 'rechazar')" class="btn-panel btn-secondary">Rechazar</button>
                  </div>
                </div>
              </div>
              <div *ngIf="peticionesRecibidas.length === 0" class="lista-vacia">
                No tienes peticiones nuevas por aceptar.
              </div>
            </div>
          </div>
        </section>

      </div>

      <div *ngIf="mostrarCalificacion" class="modal-overlay">
        <div class="modal-card">
          <h3>¿Cómo calificas este trueque?</h3>
          <p class="modal-desc" *ngIf="txParaFinalizar">
            <strong>{{ txParaFinalizar.ofertaTitulo }}</strong> — {{ txParaFinalizar.nombreContraparte }}
          </p>
          <div class="stars-row">
            <button *ngFor="let n of [1,2,3,4,5]" type="button" class="star-btn" (click)="notaEstrellas = n">
              <span class="star-char" [class.star-on]="n <= notaEstrellas">★</span>
            </button>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-panel btn-secondary" (click)="cerrarCalificacion()">Cancelar</button>
            <button type="button" class="btn-panel btn-add" [disabled]="notaEstrellas === 0" (click)="confirmarConCalificacion()">
              Confirmar y cerrar
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
    .profile-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 24px;
    }
    .full-width {
      grid-column: span 2;
    }
    .card {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.05);
      padding: 24px;
    }
    .profile-section h2 {
      font-size: 1.2rem;
      font-weight: 700;
      color: #1f2937;
      margin: 0 0 16px 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .section-desc {
      font-size: 0.85rem;
      color: #6b7280;
      margin: -8px 0 16px 0;
    }
    .info-group {
      margin-bottom: 14px;
    }
    .info-group:last-child {
      margin-bottom: 0;
    }
    .info-group label {
      display: block;
      font-size: 0.75rem;
      font-weight: 700;
      color: #9ca3af;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 4px;
    }
    .value-display {
      font-size: 0.95rem;
      color: #374151;
      font-weight: 500;
      background: #f9fafb;
      padding: 10px 12px;
      border-radius: 6px;
      border: 1px solid #f3f4f6;
    }
    
    /* Manejo de Habilidades (Tags) */
    .tags-container {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 20px;
      min-height: 40px;
      align-content: flex-start;
    }
    .skill-tag {
      background: #e0e7ff;
      color: #4338ca;
      font-size: 0.85rem;
      font-weight: 600;
      padding: 6px 12px;
      border-radius: 9999px;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    .skill-tag small {
      color: #6366f1;
      font-weight: 400;
    }
    .btn-delete-tag {
      background: none;
      border: none;
      color: #4338ca;
      font-size: 1.1rem;
      cursor: pointer;
      line-height: 1;
      padding: 0;
    }
    .btn-delete-tag:hover {
      color: #ef4444;
    }

    /* Formulario de habilidades */
    .add-skill-form {
      display: flex;
      gap: 8px;
      border-top: 1px solid #f3f4f6;
      padding-top: 16px;
    }
    .form-control {
      flex: 1;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      padding: 8px 12px;
      font-size: 0.875rem;
      outline: none;
      font-family: inherit;
    }
    .form-control:focus {
      border-color: #4f46e5;
    }
    .select-control {
      max-width: 120px;
      background: #ffffff;
    }

    /* Listado de Ofertas */
    .list-wrapper {
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      overflow: hidden;
    }
    .oferta-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 20px;
      border-bottom: 1px solid #e5e7eb;
      background: #ffffff;
    }
    .oferta-item:last-child {
      border-bottom: none;
    }
    .oferta-info h3 {
      font-size: 0.95rem;
      font-weight: 600;
      color: #111827;
      margin: 0 0 4px 0;
    }
    .categoria-badge {
      font-size: 0.75rem;
      color: #6b7280;
      background: #f3f4f6;
      padding: 2px 8px;
      border-radius: 4px;
    }
    .oferta-meta {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .horas-badge {
      font-size: 0.9rem;
      font-weight: 700;
      color: #4f46e5;
      background: #f5f3ff;
      padding: 4px 10px;
      border-radius: 6px;
    }

    /* Botones generales del panel */
    .btn-panel {
      padding: 8px 14px;
      font-size: 0.825rem;
      font-weight: 600;
      border-radius: 6px;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
    }
    .btn-add {
      background: #4f46e5;
      color: #ffffff;
    }
    .btn-add:hover:not(:disabled) { background: #4338ca; }
    .btn-add:disabled { background: #e5e7eb; color: #9ca3af; cursor: not-allowed; }
    .btn-secondary {
      background: #ffffff;
      border: 1px solid #d1d5db;
      color: #4b5563;
    }
    .btn-secondary:hover { background: #f9fafb; }
    .btn-secondary:hover:not(:disabled) { background: #f9fafb; }

    .text-muted { color: #9ca3af; font-size: 0.875rem; }
    .lista-vacia { padding: 24px; text-align: center; color: #9ca3af; font-size: 0.9rem; }

    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(17, 24, 39, 0.45);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 50;
      padding: 16px;
    }
    .modal-card {
      background: #fff;
      border-radius: 12px;
      padding: 22px;
      max-width: 380px;
      width: 100%;
      box-shadow: 0 10px 25px rgba(0,0,0,0.12);
    }
    .modal-card h3 { margin: 0 0 8px 0; font-size: 1.1rem; }
    .modal-desc { margin: 0 0 14px 0; font-size: 0.85rem; color: #6b7280; }
    .stars-row { display: flex; gap: 6px; justify-content: center; margin-bottom: 12px; }
    .star-btn { background: none; border: none; cursor: pointer; padding: 0; }
    .star-char { font-size: 1.75rem; color: #d1d5db; }
    .star-on { color: #f59e0b; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 14px; }

    .form-group-edit {
      margin-bottom: 12px;
    }
    .form-group-edit label {
      display: block;
      font-size: 0.8rem;
      font-weight: 600;
      color: #4b5563;
      margin-bottom: 4px;
    }
    .btn-danger {
      background: #ef4444;
      color: #ffffff;
    }
    .btn-danger:hover:not(:disabled) {
      background: #dc2626;
    }
    .btn-danger:disabled {
      background: #fca5a5;
      color: #ffffff;
      cursor: not-allowed;
    }

    /* Efecto hover para indicar que los encabezados son clickeables */
    .profile-section h2:hover {
      opacity: 0.8;
      color: #4f46e5;
    }

    @media (max-width: 768px) {
      .profile-grid { grid-template-columns: 1fr; }
      .full-width { grid-column: span 1; }
      .add-skill-form { flex-direction: column; }
      .select-control { max-width: 100%; }
    }
  `]
})
export class Perfil implements OnInit {
  user: UserProfile | null = null;
  // Datos reactivos iniciales
  habilidades: Habilidad[] = [];

  private readonly categoriasMap = CATEGORIAS_MAP;

  misOfertas: OfertaPropia[] = [];
  intercambiosEnCurso: TransaccionCurso[] = [];
  solicitudesRealizadas: TransaccionCurso[] = [];
  peticionesRecibidas: TransaccionCurso[] = [];

  nuevaHabilidadNombre: string = '';
  nuevaHabilidadNivel: 'Básico' | 'Intermedio' | 'Avanzado' | 'Experto' = 'Intermedio';

  mostrarCalificacion = false;
  txParaFinalizar: TransaccionCurso | null = null;
  notaEstrellas = 0;

  // Variables para la modificacion del perfil (en espanol)
  nuevaClave: string = '';
  confirmarClave: string = '';
  preguntaSeguridad: string = '';
  respuestaSeguridad: string = '';

  // Variables para la eliminacion de cuenta (en espanol)
  confirmacionEliminar: string = '';

  // Variables para controlar el despliegue (acordeon) de cada seccion (en espanol)
  seccionInformacionAbierta = false;
  seccionHabilidadesAbierta = false;
  seccionModificarAbierta = false;
  seccionEliminarAbierta = false;
  seccionOfertasAbierta = false;
  seccionIntercambiosAbierta = false;
  seccionSolicitudesAbierta = false;
  seccionPeticionesAbierta = false;

  constructor(
    private auth: AuthService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private aviso: NotificacionService,
    private router: Router
  ) { }

  async ngOnInit(): Promise<void> {
    await this.auth.refrescarBilletera();
    this.user = this.auth.userProfile;
    if (this.user?.skills?.length) {
      this.habilidades = this.user.skills
        .filter(s => s && s !== 'Sin definir')
        .map(s => ({ nombre: s, nivel: 'Intermedio' as const }));
    }
    this.cargarMisOfertas();
    this.cargarIntercambiosEnCurso();
  }

  async cargarIntercambiosEnCurso() {
    if (!this.user) return;
    try {
      // Obtener transacciones del usuario
      const txs = await firstValueFrom(this.http.get<any[]>(`http://localhost:8080/api/transacciones/usuario?idUsuario=${this.user.id}`));

      // Obtener todas las ofertas para mapear títulos
      const ofertas = await firstValueFrom(this.http.get<any[]>('http://localhost:8080/api/ofertas/listar'));
      const ofertasMap = new Map<string, string>();
      ofertas.forEach(o => ofertasMap.set(o.id_oferta, o.titulo));

      // Obtener todos los miembros con nombre y teléfono
      const miembros = await firstValueFrom(this.http.get<any[]>('http://localhost:8080/api/miembros/listar'));
      const miembrosMap = new Map<string, { nombre: string, telefono: string }>();
      miembros.forEach(m => {
        miembrosMap.set(m.id, { 
          nombre: m.nombre,
          telefono: m.telefono || ''
        });
      });

      // Fetch user's submitted evaluations to mark yaCalificado
      let califTxIds = new Set<string>();
      if (this.user) {
        try {
          const calificaciones = await firstValueFrom(
            this.http.get<any[]>(`http://localhost:8080/api/reputacion/usuario?idEvaluador=${this.user.id}`)
          );
          if (calificaciones && Array.isArray(calificaciones)) {
            calificaciones.forEach(c => {
              if (c.id_transaccion) {
                califTxIds.add(c.id_transaccion);
              }
            });
          }
        } catch (err) {
          console.error('Error fetching user evaluations in profile:', err);
        }
      }

      // Filtrar transacciones "Iniciada" o "Finalizado con exito" (si falta calificar) donde el usuario es receptor o prestador
      const iniciadas = txs.filter(t => {
        const participa = t.id_receptor === this.user?.id || t.id_prestador === this.user?.id;
        if (!participa) return false;

        if (t.estado === 'Iniciada') return true;

        if (t.estado === 'Finalizado con exito') {
          // Si el usuario no ha calificado este trueque todavía, lo mantenemos en la lista como pendiente por calificar
          return !califTxIds.has(t.id_transaccion);
        }
        return false;
      });

      // Filtrar "Solicitada" donde el usuario es receptor (solicitudes realizadas por él)
      const solicitadas = txs.filter(t =>
        t.estado === 'Solicitada' &&
        t.id_receptor === this.user?.id &&
        t.id_receptor !== t.id_prestador
      );

      // Filtrar "Solicitada" donde el usuario es prestador (peticiones recibidas)
      const recibidas = txs.filter(t =>
        t.estado === 'Solicitada' &&
        t.id_prestador === this.user?.id &&
        t.id_receptor !== t.id_prestador
      );

      // Mapear intercambios en curso (Iniciada / Finalizado con exito)
      this.intercambiosEnCurso = iniciadas.map(t => {
        const esRec = t.id_receptor === this.user?.id;
        const idContraparte = esRec ? t.id_prestador : t.id_receptor;
        const info = miembrosMap.get(idContraparte) || { nombre: idContraparte, telefono: '' };
        return {
          idTransaccion: t.id_transaccion,
          ofertaTitulo: ofertasMap.get(t.id_oferta) || 'Oferta Desconocida',
          nombreContraparte: info.nombre,
          telefonoContraparte: info.telefono,   // <-- Asignamos el teléfono
          horasPactadas: t.horas,
          esReceptor: esRec,
          estado: t.estado,
          yaCalificado: califTxIds.has(t.id_transaccion)
        };
      });

      // Mapear solicitudes realizadas (Solicitada como receptor)
      this.solicitudesRealizadas = solicitadas.map(t => {
        const info = miembrosMap.get(t.id_prestador) || { nombre: t.id_prestador, telefono: '' };
        return {
          idTransaccion: t.id_transaccion,
          ofertaTitulo: ofertasMap.get(t.id_oferta) || 'Oferta Desconocida',
          nombreContraparte: info.nombre,
          telefonoContraparte: info.telefono,   // <-- Asignamos el teléfono (por si se usa)
          horasPactadas: t.horas,
          esReceptor: true
        };
      });

      // Mapear peticiones recibidas (Solicitada como prestador)
      this.peticionesRecibidas = recibidas.map(t => {
        const info = miembrosMap.get(t.id_receptor) || { nombre: t.id_receptor, telefono: '' };
        return {
          idTransaccion: t.id_transaccion,
          ofertaTitulo: ofertasMap.get(t.id_oferta) || 'Oferta Desconocida',
          nombreContraparte: info.nombre,
          telefonoContraparte: info.telefono,   // <-- Asignamos el teléfono (por si se usa)
          horasPactadas: t.horas,
          esReceptor: false
        };
      });

      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error cargando intercambios en curso:', error);
    }
  }

  async cargarMisOfertas() {
    if (!this.user) return;
    try {
      const response = await firstValueFrom(
        this.http.get<any[]>(`http://localhost:8080/api/ofertas/por-creador?idCreador=${this.user.id}`)
      );

      this.misOfertas = response
        .filter(o => ofertaActiva(o.estado) && o.id_creador === this.user?.id)
        .map(o => ({
          id: o.id_oferta,
          categoria: this.categoriasMap[o.categoria] || o.categoria || 'Servicio',
          titulo: o.titulo,
          horas: o.horas_estimadas
        }));
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error cargando mis ofertas', error);
    }
  }

  agregarHabilidad(): void {
    if (!this.nuevaHabilidadNombre.trim()) return;

    this.habilidades.push({
      nombre: this.nuevaHabilidadNombre.trim(),
      nivel: this.nuevaHabilidadNivel
    });

    this.nuevaHabilidadNombre = '';
    this.nuevaHabilidadNivel = 'Intermedio';
  }

  eliminarHabilidad(index: number): void {
    this.habilidades.splice(index, 1);
  }

  async darDeBajaOferta(id: string): Promise<void> {
    if (!this.user) return;
    try {
      // 1. Chequear estado de la oferta en relación a transacciones
      const checkRes = await firstValueFrom(
        this.http.get<{ tieneEnCurso: boolean; pendientesCount: number }>(
          `http://localhost:8080/api/ofertas/chequear-eliminacion?idOferta=${id}`
        )
      );

      if (checkRes.tieneEnCurso) {
        this.aviso.mostrar(
          'No puedes eliminar una oferta con intercambios en curso. Finaliza el proceso antes de retirar el servicio.',
          'error'
        );
        return;
      }

      let mensajeConfirmacion = '¿Estás seguro de que deseas dar de baja esta oferta?';
      if (checkRes.pendientesCount > 0) {
        mensajeConfirmacion = `Tienes ${checkRes.pendientesCount} solicitud(es) pendiente(s) para esta oferta que serán rechazada(s) automáticamente al retirarla. ¿Estás seguro de que deseas continuar?`;
      }

      const confirmar = confirm(mensajeConfirmacion);
      if (!confirmar) return;

      // 2. Enviar solicitud de eliminación al backend
      const payload = { idOferta: id, idUsuario: this.user.id };
      await firstValueFrom(
        this.http.post('http://localhost:8080/api/ofertas/eliminar', payload)
      );

      this.aviso.mostrar('Publicación eliminada con éxito.');
      this.cargarMisOfertas();
      this.cdr.detectChanges();
    } catch (error: any) {
      console.error('Error al dar de baja la oferta:', error);
      let errorMsg = 'No se pudo retirar la oferta. Reintente.';
      if (error?.error?.validationError) {
        errorMsg = error.error.validationError;
      } else if (error?.error?.error) {
        errorMsg = error.error.error;
      }
      this.aviso.mostrar(errorMsg, 'error');
    }
  }

  editarOferta(id: string): void {
    this.router.navigate(['/editar-oferta', id]);
  }

  abrirCalificacionAlCompletar(tx: TransaccionCurso) {
    this.txParaFinalizar = tx;
    this.notaEstrellas = 0;
    this.mostrarCalificacion = true;
    this.cdr.detectChanges();
  }

  cerrarCalificacion() {
    this.mostrarCalificacion = false;
    this.txParaFinalizar = null;
    this.notaEstrellas = 0;
    this.cdr.detectChanges();
  }

  async confirmarConCalificacion() {
    if (!this.user || !this.txParaFinalizar || this.notaEstrellas < 1) return;
    const id = this.txParaFinalizar.idTransaccion;
    const userId = this.user.id;

    try {
      if (this.txParaFinalizar.estado !== 'Finalizado con exito') {
        const payload = { idMiembroSesion: userId, idTrans: id };
        const confirmRes = await firstValueFrom(
          this.http.post<{ saldoActualizado?: number }>('http://localhost:8080/api/transacciones/confirmar', payload)
        );

        if (confirmRes.saldoActualizado !== undefined) {
          this.auth.actualizarSaldo(confirmRes.saldoActualizado);
          this.user = this.auth.userProfile;
        }
      }

      await firstValueFrom(this.http.post('http://localhost:8080/api/reputacion/calificar', {
        idEvaluador: userId,
        idTrans: id,
        estrellas: this.notaEstrellas,
        comentario: '-'
      }));

      this.intercambiosEnCurso = this.intercambiosEnCurso.filter(t => t.idTransaccion !== id);
      this.cerrarCalificacion();
      this.aviso.mostrar('Calificación registrada.');
      this.cargarMisOfertas();
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error confirmando finalizacion', error);
      this.aviso.mostrar('No se pudo cerrar el intercambio. Revisa tu saldo de horas.', 'error');
    }
  }

  async gestionarPeticion(id: string, accion: 'aceptar' | 'rechazar') {
    if (!this.user) return;
    try {
      const payload = {
        idPrestador: this.user.id,
        idTrans: id,
        decision: accion === 'aceptar' ? 'Aceptar' : 'Rechazar'
      };

      const result = await firstValueFrom(this.http.post<any>('http://localhost:8080/api/transacciones/gestionar', payload));

      // Remover de las peticiones pendientes
      this.peticionesRecibidas = this.peticionesRecibidas.filter(t => t.idTransaccion !== id);

      if (accion === 'aceptar') {
        // Mostrar un mensaje con el teléfono del solicitante si está disponible
        const txAceptada = this.peticionesRecibidas.find(t => t.idTransaccion === id);
        let mensaje = 'Solicitud aceptada. La oferta ya no aparece en el muro.';
        if (txAceptada?.telefonoContraparte) {
          mensaje += ` Contacta al solicitante: 📞 ${txAceptada.telefonoContraparte}`;
        }
        this.aviso.mostrar(mensaje);
        this.cargarIntercambiosEnCurso();
        this.cargarMisOfertas();
      } else {
        this.aviso.mostrar('Solicitud rechazada.');
      }
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error gestionando peticion', error);
      this.aviso.mostrar('No se pudo procesar la petición.', 'error');
    }
  }

  async actualizarPerfil(): Promise<void> {
    if (!this.user) return;

    if (this.nuevaClave || this.confirmarClave) {
      if (this.nuevaClave !== this.confirmarClave) {
        this.aviso.mostrar('Las contraseñas no coinciden.', 'error');
        return;
      }
      if (this.nuevaClave.length < 6) {
        this.aviso.mostrar('La nueva contraseña debe tener al menos 6 caracteres.', 'error');
        return;
      }
    }

    if (this.preguntaSeguridad && !this.respuestaSeguridad.trim()) {
      this.aviso.mostrar('Debes ingresar la respuesta de seguridad para la pregunta elegida.', 'error');
      return;
    }

    if (!this.nuevaClave && !this.preguntaSeguridad) {
      this.aviso.mostrar('Ingresa una nueva contraseña o selecciona una pregunta de seguridad para actualizar.', 'error');
      return;
    }

    try {
      const resultado = await this.auth.actualizarPerfilUsuario(
        this.user.id,
        this.nuevaClave || undefined,
        this.preguntaSeguridad || undefined,
        this.respuestaSeguridad.trim() || undefined
      );

      if (resultado.success) {
        this.aviso.mostrar('Datos cambiados exitosamente');
        this.nuevaClave = '';
        this.confirmarClave = '';
        this.preguntaSeguridad = '';
        this.respuestaSeguridad = '';
        this.cdr.detectChanges();
      } else {
        this.aviso.mostrar(resultado.message, 'error');
      }
    } catch (error) {
      console.error('Error al actualizar el perfil:', error);
      this.aviso.mostrar('Error al actualizar el perfil.', 'error');
    }
  }

  async eliminarPerfil(): Promise<void> {
    if (!this.user) return;

    if (this.confirmacionEliminar !== 'ELIMINAR') {
      this.aviso.mostrar('Debes escribir exactamente la palabra ELIMINAR en mayúsculas.', 'error');
      return;
    }

    try {
      const resultado = await this.auth.eliminarPerfilUsuario(this.user.id, this.confirmacionEliminar);

      if (resultado.success) {
        this.aviso.mostrar('Perfil eliminado');
        setTimeout(() => {
          window.location.href = '/login';
        }, 1500);
      } else {
        this.aviso.mostrar(resultado.message, 'error');
      }
    } catch (error) {
      console.error('Error al eliminar el perfil:', error);
      this.aviso.mostrar('Error al eliminar el perfil.', 'error');
    }
  }
}
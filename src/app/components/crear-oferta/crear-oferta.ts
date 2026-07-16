// Formulario para publicar una oferta en el muro
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ChangeDetectorRef } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CATEGORIAS } from '../../utils/categorias';

@Component({
  selector: 'app-crear-oferta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="panel-container">
      <header class="panel-header">
        <h1>{{ idOfertaEdit ? 'Editar Oferta' : 'Publicar Nueva Oferta' }}</h1>
        <p>{{ idOfertaEdit ? 'Actualiza los datos de tu publicación.' : 'Comparte tus habilidades informáticas, académicas o técnicas con la comunidad y acumula horas en tu billetera.' }}</p>
      </header>

      <div class="form-wrapper">
        <form [formGroup]="ofertaForm" (ngSubmit)="onSubmit()">
          
          <div class="form-group">
            <label for="titulo">Título del Servicio / Habilidad</label>
            <input 
              type="text" 
              id="titulo" 
              formControlName="titulo" 
              placeholder="Ej. Asesoría en Cálculo Multivariable o Desarrollo de Componentes Angular"
              [class.input-error]="isValidField('titulo')">
            <span *ngIf="isValidField('titulo')" class="error-msg">
              El título es obligatorio (mínimo 10 caracteres).
            </span>
          </div>

          <div class="form-group">
            <label for="categoria">Categoría</label>
            <select id="categoria" formControlName="categoria">
              <option *ngFor="let cat of categorias" [value]="cat.id">{{ cat.nombre }}</option>
            </select>
          </div>

          <div class="form-group">
            <label for="horas">Costo Estimado (Horas de Tiempo)</label>
            <div class="input-hours-wrapper">
              <input 
                type="number" 
                id="horas" 
                formControlName="horas" 
                placeholder="2"
                min="0.5" 
                step="0.5"
                [class.input-error]="isValidField('horas')">
              <span class="hours-suffix">Horas</span>
            </div>
            <p class="input-help">Define cuántas horas de dedicación real costará prestar este servicio.</p>
            <span *ngIf="isValidField('horas')" class="error-msg">
              Ingresa un tiempo válido (mínimo 0.5 horas).
            </span>
          </div>

          <div class="form-group">
            <label for="descripcion">Descripción del Servicio</label>
            <textarea 
              id="descripcion" 
              formControlName="descripcion" 
              rows="5" 
              placeholder="Describe detalladamente qué incluye tu oferta, tus conocimientos en el área y cómo organizarás las sesiones o entregas..."
              [class.input-error]="isValidField('descripcion')"></textarea>
            <span *ngIf="isValidField('descripcion')" class="error-msg">
              Por favor, ofrece una descripción clara (mínimo 20 caracteres).
            </span>
          </div>

          <div class="form-actions">
            <button type="button" class="btn-panel btn-cancel" (click)="cancelar()">
              Cancelar
            </button>
            <button type="submit" class="btn-panel btn-save" [disabled]="ofertaForm.invalid">
              {{ idOfertaEdit ? 'Actualizar Oferta' : 'Publicar Oferta' }}
            </button>
          </div>

        </form>
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
      color: var(--texto-principal);
      margin: 0 0 4px 0;
    }
    .panel-header p {
      color: var(--texto-secundario);
      font-size: 0.95rem;
      margin: 0 0 24px 0;
    }
    .form-wrapper {
      background: var(--bg-principal);
      border: 1px solid var(--borde-color);
      border-radius: var(--radius-lg, 12px);
      box-shadow: var(--shadow);
      padding: 28px;
      transition: background-color 0.3s ease, border-color 0.3s ease;
    }
    .form-group {
      margin-bottom: 20px;
    }
    .form-group label {
      display: block;
      font-size: 0.9rem;
      font-weight: 600;
      color: var(--texto-principal);
      margin-bottom: 6px;
    }
    .form-group input[type="text"],
    .form-group input[type="number"],
    .form-group select,
    .form-group textarea {
      width: 100%;
      box-sizing: border-box;
      border: 1px solid var(--borde-color);
      background-color: var(--bg-fondo);
      color: var(--texto-principal);
      border-radius: var(--radius-md, 8px);
      padding: 10px 12px;
      font-size: 0.9rem;
      outline: none;
      font-family: inherit;
      transition: border-color 0.2s, box-shadow 0.2s, background-color 0.3s ease;
    }
    .form-group select option {
      background-color: var(--bg-principal);
      color: var(--texto-principal);
    }
    .form-group input:focus,
    .form-group select:focus,
    .form-group textarea:focus {
      border-color: var(--brand-color, #4f46e5);
      box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
    }
    .input-hours-wrapper {
      display: flex;
      align-items: center;
      position: relative;
      max-width: 200px;
    }
    .input-hours-wrapper input {
      width: 100%;
      box-sizing: border-box;
      border: 1px solid var(--borde-color);
      background-color: var(--bg-fondo);
      color: var(--texto-principal);
      border-radius: var(--radius-md, 8px);
      padding: 10px 65px 10px 12px;
      font-size: 0.9rem;
      outline: none;
    }
    .hours-suffix {
      position: absolute;
      right: 12px;
      color: var(--texto-secundario);
      font-size: 0.85rem;
      font-weight: 500;
      pointer-events: none;
    }
    .input-help {
      font-size: 0.8rem;
      color: var(--texto-secundario);
      margin: 4px 0 0 0;
    }
    .input-error {
      border-color: #ef4444 !important;
      box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1) !important;
    }
    .error-msg {
      display: block;
      color: #ef4444;
      font-size: 0.8rem;
      margin-top: 4px;
      font-weight: 500;
    }
    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 32px;
      border-top: 1px solid var(--borde-color);
      padding-top: 20px;
    }
    .btn-panel {
      padding: 10px 18px;
      font-size: 0.875rem;
      font-weight: 600;
      border-radius: var(--radius-md, 8px);
      cursor: pointer;
      transition: all 0.2s;
      border: none;
    }
    .btn-cancel {
      background: var(--bg-fondo);
      border: 1px solid var(--borde-color);
      color: var(--texto-principal);
    }
    .btn-cancel:hover {
      background: var(--borde-color);
    }
    .btn-save {
      background: var(--brand-color, #4f46e5);
      color: #ffffff;
    }
    .btn-save:hover {
      filter: brightness(1.1);
    }
    .btn-save:disabled {
      background: var(--borde-color);
      color: var(--texto-secundario);
      cursor: not-allowed;
      box-shadow: none;
    }
  `]
})
export class CrearOferta implements OnInit {
  readonly categorias = CATEGORIAS;
  ofertaForm!: FormGroup;
  idOfertaEdit: string | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.idOfertaEdit = id;
        this.cargarOferta(id);
      } else {
        this.idOfertaEdit = null;
      }
    });
  }

  private initForm(): void {
    this.ofertaForm = this.fb.group({
      titulo: ['', [Validators.required, Validators.minLength(10)]],
      categoria: ['CAT-001', [Validators.required]],
      horas: [2, [Validators.required, Validators.min(0.5)]],
      descripcion: ['', [Validators.required, Validators.minLength(20)]]
    });
  }

  private cargarOferta(id: string): void {
    console.log('🔍 Cargando oferta ID:', id);
    this.http.get<any>(`http://localhost:8080/api/ofertas/${id}`).subscribe({
      next: (oferta) => {
        console.log('✅ Oferta cargada:', oferta);
        
        // Mapeo defensivo: el backend puede devolver 'idCategoria' o 'categoria'
        const categoriaId = oferta.idCategoria || oferta.categoria || 'CAT-001';
        // Lo mismo para las horas
        const horasEstimadas = oferta.horas_estimadas !== undefined ? oferta.horas_estimadas : (oferta.horas || 2);

        this.ofertaForm.patchValue({
          titulo: oferta.titulo,
          categoria: oferta.categoria,
          horas: oferta.horas_estimadas,
          descripcion: oferta.descripcion
        });
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Error cargando oferta', err);
        alert('No se pudo cargar la oferta. Verifica el ID.');
      }
    });
  }

  isValidField(field: string): boolean {
    const formField = this.ofertaForm.get(field);
    return !!(formField && formField.invalid && (formField.dirty || formField.touched));
  }

  async onSubmit() {
    console.log('🚀 onSubmit ejecutado');
    console.log('  idOfertaEdit =', this.idOfertaEdit);
    console.log('  Formulario válido?', this.ofertaForm.valid);
    
    if (this.ofertaForm.invalid) {
      console.warn('⚠️ Formulario inválido. Errores:', this.ofertaForm.errors);
      alert('El formulario no es válido. Revisa los campos.');
      return;
    }

    const currentUser = this.auth.userProfile;
    if (!currentUser) {
      alert('No has iniciado sesión.');
      return;
    }
    console.log('👤 Usuario actual:', currentUser.id);

    const formValues = this.ofertaForm.value;
    const payload: any = {
      titulo: formValues.titulo,
      descripcion: formValues.descripcion,
      horas: formValues.horas,
      idCategoria: formValues.categoria
    };

    let url: string;
    if (this.idOfertaEdit) {
      url = 'http://localhost:8080/api/ofertas/actualizar';
      payload.idOferta = this.idOfertaEdit;
      payload.idUsuario = currentUser.id;
      console.log('📝 EDITANDO oferta, payload:', payload);
    } else {
      url = 'http://localhost:8080/api/ofertas/publicar';
      payload.idCreador = currentUser.id;
      console.log('📌 CREANDO oferta, payload:', payload);
    }

    try {
      console.log('📡 Enviando petición a:', url);
      const response = await firstValueFrom(this.http.post(url, payload));
      console.log('✅ Respuesta OK:', response);
      await this.router.navigate(['/muro'], { queryParams: { editado: '1' } });
    } catch (error) {
      console.error('❌ Error HTTP:', error);
      let mensajeError = 'Error al guardar la oferta.';
      if (error instanceof HttpErrorResponse) {
        const cuerpo = error.error;
        if (cuerpo?.validationError) {
          mensajeError = cuerpo.validationError;
        } else if (cuerpo?.error) {
          mensajeError = cuerpo.error;
        } else {
          mensajeError = `Error ${error.status}: ${error.statusText}`;
        }
      }
      alert('❌ ' + mensajeError);
      this.cdr.detectChanges();
    }
  }

  cancelar(): void {
    console.log('Cancelar');
    this.ofertaForm.reset();
    this.router.navigate(['/muro']);
  }
}
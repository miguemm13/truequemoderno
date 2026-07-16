// Feed publico: lista ofertas activas y permite pedir un trueque
import { Component, OnInit, OnDestroy, ChangeDetectorRef, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { firstValueFrom, Subscription, filter } from 'rxjs';
import { ofertaActiva } from '../../utils/estados';
import { CATEGORIAS_MAP } from '../../utils/categorias';

interface Oferta {
  id: string;
  categoria: string;
  titulo: string;
  descripcion: string;
  autorId: string;
  autor: string;
  reputacion: number;
  horas: number;
  esSugerenciaMatch?: boolean;
  matchDetalle?: string;
}

@Component({
  selector: 'app-muro',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './muro.html',
  styleUrl: './muro.css',
})
export class Muro implements OnInit, OnDestroy {
  ofertas: Oferta[] = [];
  ofertasSolicitadas = new Set<string>();
  errorCarga = '';

  showToast = false;
  toastMessage = '';
  toastSubMessage = '';
  toastType: 'success' | 'error' = 'success';

  // Táctica de Disponibilidad: Ping/Echo
  matchingServiceOnline = true;
  faultDetectionTime = 0;
  simulationMode = false;
  private pingIntervalId?: any;

  private routerSub?: Subscription;

  private readonly categoriasMap = CATEGORIAS_MAP;

  get miUsuarioId(): string | undefined {
    return this.auth.userProfile?.id;
  }

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    effect(() => {
      if (this.auth.userProfile?.id) {
        this.refrescarMuro();
      }
    });
  }

  ngOnInit() {
    this.refrescarMuro();
    this.iniciarMonitorDisponibilidad();
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        if (e.urlAfterRedirects.startsWith('/muro')) {
          this.refrescarMuro();
        }
      });
  }

  ngOnDestroy() {
    this.routerSub?.unsubscribe();
    if (this.pingIntervalId) {
      clearInterval(this.pingIntervalId);
    }
  }

  private async refrescarMuro() {
    await this.cargarMisSolicitudes();
    await this.cargarOfertas();
    this.cdr.detectChanges();
  }

  async cargarMisSolicitudes() {
    try {
      const currentUser = this.auth.userProfile;
      if (!currentUser) return;

      const response = await firstValueFrom(
        this.http.get<any[]>(`http://localhost:8080/api/transacciones/usuario?idUsuario=${currentUser.id}`)
      );
      const ids = response
        .filter(t => t.id_receptor === currentUser.id && t.estado !== 'Cancelada por Prestador')
        .map(t => t.id_oferta);
      this.ofertasSolicitadas = new Set(ids);
    } catch (error) {
      console.error('Error cargando solicitudes previas', error);
    }
  }

  async cargarOfertas() {
    this.errorCarga = '';
    try {
      const currentUser = this.auth.userProfile;
      let suggestions: any[] = [];
      if (currentUser && this.matchingServiceOnline) {
        try {
          suggestions = await firstValueFrom(
            this.http.get<any[]>(`http://localhost:8080/api/matching/sugerencias?idUsuario=${currentUser.id}`)
          );
        } catch (e) {
          console.warn('No se pudieron obtener sugerencias de matching', e);
          // If the matching suggestions query fails, immediately mark as offline and calculate detection time
          this.matchingServiceOnline = false;
        }
      }

      const suggestionsMap = new Map<string, any>();
      if (suggestions && Array.isArray(suggestions)) {
        suggestions.forEach(s => {
          if (s.idSocioSugerido) {
            suggestionsMap.set(s.idSocioSugerido, s);
          }
        });
      }

      const response = await firstValueFrom(
        this.http.get<any[]>(`http://localhost:8080/api/ofertas/muro?_=${Date.now()}`)
      );
      const miembrosMap = await this.obtenerMapaNombres();
      const activas = (response ?? [])
        .filter(o => ofertaActiva(o.estado))
        .sort((a, b) => (b.fecha_publicacion || '').localeCompare(a.fecha_publicacion || ''));

      this.ofertas = activas
        .map(o => {
          const match = suggestionsMap.get(o.id_creador);
          return {
            id: o.id_oferta,
            categoria: this.categoriasMap[o.categoria] || o.categoria || 'Servicio',
            titulo: o.titulo,
            descripcion: o.descripcion,
            autorId: o.id_creador,
            autor: miembrosMap.get(o.id_creador) || ('Miembro ' + o.id_creador),
            horas: o.horas_estimadas,
            reputacion: 5.0,
            esSugerenciaMatch: !!match,
            matchDetalle: match ? `Sugerencia de trueque cruzado: Ofreces "${match.habilidadOfrecida}" y recibes a cambio "${match.habilidadDemandada}".` : undefined
          };
        }); // Removing the filter so all offers are displayed

      // Ya no es necesario ordenar puesto que todas son sugerencias de match, pero se mantiene la estructura
      this.ofertas.sort((a, b) => {
        if (a.esSugerenciaMatch && !b.esSugerenciaMatch) return -1;
        if (!a.esSugerenciaMatch && b.esSugerenciaMatch) return 1;
        return 0;
      });

    } catch (error) {
      console.error('Error cargando muro de ofertas', error);
      this.errorCarga = 'No se pudo cargar el muro. ¿Está encendido el backend en el puerto 8080?';
      this.ofertas = [];
    }
    this.cdr.detectChanges();
  }

  private async obtenerMapaNombres(): Promise<Map<string, string>> {
    const mapa = new Map<string, string>();
    try {
      const miembros = await firstValueFrom(
        this.http.get<{ id: string; nombre: string }[]>(`http://localhost:8080/api/miembros/listar`)
      );
      miembros.forEach(m => mapa.set(m.id, m.nombre));
    } catch (error) {
      console.warn('No se cargaron nombres de miembros', error);
    }
    return mapa;
  }

  async solicitarIntercambio(oferta: Oferta) {
    if (this.ofertasSolicitadas.has(oferta.id) || oferta.autorId === this.miUsuarioId) return;

    const currentUser = this.auth.userProfile;
    if (!currentUser) return;

    try {
      await firstValueFrom(
        this.http.post('http://localhost:8080/api/transacciones/solicitar', {
          idReceptor: currentUser.id,
          idOferta: oferta.id,
          horas: oferta.horas
        })
      );

      this.ofertasSolicitadas.add(oferta.id);
      this.toastMessage = 'Solicitud enviada';
      this.toastSubMessage = `El prestador de "${oferta.titulo}" debe aceptarla desde su perfil.`;
      this.toastType = 'success';
      this.showToast = true;
      this.cdr.detectChanges();

      setTimeout(() => {
        this.showToast = false;
        this.cdr.detectChanges();
      }, 5000);
    } catch (error) {
      console.error('Error al solicitar intercambio', error);
      this.toastMessage = 'No se pudo enviar la solicitud';
      this.toastSubMessage = this.mensajeErrorApi(error);
      this.toastType = 'error';
      this.showToast = true;
      setTimeout(() => {
        this.showToast = false;
        this.cdr.detectChanges();
      }, 5000);
    }
  }

  private mensajeErrorApi(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const cuerpo = error.error;
      if (cuerpo?.validationError) return cuerpo.validationError;
      if (cuerpo?.error) return cuerpo.error;
    }
    return 'Revisa tu saldo (maximo -2.00 h) o que la oferta siga activa.';
  }

  iniciarMonitorDisponibilidad() {
    // Ping/Echo every 3 seconds to check matching engine status
    this.pingIntervalId = setInterval(() => {
      const startTime = Date.now();
      this.http.get<any>('http://localhost:8080/api/matching/ping').subscribe({
        next: () => {
          this.matchingServiceOnline = true;
          this.cdr.detectChanges();
        },
        error: () => {
          const endTime = Date.now();
          this.faultDetectionTime = endTime - startTime;
          this.matchingServiceOnline = false;
          this.cdr.detectChanges();
        }
      });
    }, 3000);
  }

  async toggleSimulacionFalla() {
    this.simulationMode = !this.simulationMode;
    try {
      await firstValueFrom(
        this.http.get(`http://localhost:8080/api/matching/simular-falla?activar=${this.simulationMode}`)
      );
      this.refrescarMuro();
    } catch (e) {
      console.error('Error al conmutar simulador de falla', e);
    }
  }

  // ================== NUEVO MÉTODO PARA EDITAR ==================
  editarOferta(id: string): void {
    this.router.navigate(['/editar-oferta', id]);
  }
  // ================== FIN NUEVO MÉTODO ==================
}
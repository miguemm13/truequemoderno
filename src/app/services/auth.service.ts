// Sesion local: login, registro y perfil en memoria + localStorage
import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  reputation: number;
  trades: number;
  balance: number;
  skills: string[];
  activeOffers: number;
  telefono?: string;
}

const AUTH_STORAGE_KEY = 'trueque-moderno-auth';
const API_URL = 'http://localhost:8080/api/miembros';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private loggedIn = signal(false);
  private profileData = signal<UserProfile | null>(null);

  constructor(private http: HttpClient) {
    this.loadFromStorage();
  }

  get isAuthenticated() {
    return this.loggedIn();
  }

  get isAuthenticatedSignal() {
    return this.loggedIn;
  }

  readonly profile = this.profileData.asReadonly();

  get userProfile() {
    return this.profileData();
  }

  actualizarSaldo(nuevoSaldo: number) {
    const actual = this.profileData();
    if (!actual) return;
    this.profileData.set({ ...actual, balance: nuevoSaldo });
    this.saveToStorage();
  }

  async refrescarBilletera(): Promise<void> {
    const actual = this.profileData();
    if (!actual) return;
    try {
      const res = await firstValueFrom(
        this.http.get<{ saldo: number; reputacion?: number }>(
          `${API_URL}/saldo?idUsuario=${actual.id}`
        )
      );
      this.profileData.set({
        ...actual,
        balance: res.saldo,
        reputation: res.reputacion ?? actual.reputation
      });
      this.saveToStorage();
    } catch (e) {
      console.error('No se pudo refrescar la billetera', e);
    }
  }

  async login(correo: string, contrasena: string): Promise<{ success: boolean; message?: string }> {
    try {
      const payload = { correo, contrasena };
      const response = await firstValueFrom(this.http.post<any>(`${API_URL}/login`, payload));
      
      this.loggedIn.set(true);
      // Asignar datos reales del backend
      let skillsArray: string[] = [];
      try {
        if (response.habilidades && response.habilidades !== '[]') {
          skillsArray = response.habilidades
            .replace('[', '')
            .replace(']', '')
            .split(',')
            .map((s: string) => s.trim())
            .filter((s: string) => s.length > 0 && s !== 'Sin definir');
        }
      } catch (e) {}

      this.profileData.set({
        id: response.id,
        name: response.nombre,
        email: response.correo,
        reputation: 0, 
        trades: 0,
        balance: response.billetera.saldo,
        skills: skillsArray,
        activeOffers: 0,
        telefono: response.telefono || '' 
      });
      this.saveToStorage();
      return { success: true };
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error && error.error.error) {
        return { success: false, message: error.error.error };
      }
      return { success: false, message: 'Error de conexión con el servidor.' };
    }
  }

  async register(nombre: string, correo: string, contrasena: string, preguntaSeguridad: string, respuestaSeguridad: string,  telefono?: string): Promise<{ success: boolean; message: string }> {
    try {
      if (contrasena.length < 6) {
        return { success: false, message: 'La contraseña debe tener al menos 6 caracteres.' };
      }
      
      const payload = {
        nombre,
        correo,
        contrasena,
        preguntaSeguridad,
        respuestaSeguridad,
        habilidades: [],
        telefono: telefono || '' 
      };
      
      const response = await firstValueFrom(this.http.post<any>(`${API_URL}/registro`, payload));
      return { success: true, message: 'Registro exitoso. Ahora puedes iniciar sesión.' };
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error && error.error.validationError) {
        return { success: false, message: error.error.validationError };
      }
      if (error instanceof HttpErrorResponse && error.error && error.error.error) {
        return { success: false, message: error.error.error };
      }
      return { success: false, message: 'Error al registrar el usuario.' };
    }
  }

  async getPreguntaSeguridad(correo: string): Promise<{ success: boolean; pregunta?: string; message?: string }> {
    try {
      const payload = { correo };
      const response = await firstValueFrom(this.http.post<any>(`${API_URL}/recuperar/pregunta`, payload));
      return { success: true, pregunta: response.preguntaSeguridad };
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error && error.error.error) {
        return { success: false, message: error.error.error };
      }
      return { success: false, message: 'Error al buscar el correo.' };
    }
  }

  async resetPassword(correo: string, respuestaSeguridad: string, nuevaContrasena: string): Promise<{ success: boolean; message: string }> {
    try {
      if (nuevaContrasena.length < 6) {
        return { success: false, message: 'La nueva contraseña debe tener al menos 6 caracteres.' };
      }

      const payload = { correo, respuestaSeguridad, nuevaContrasena };
      const response = await firstValueFrom(this.http.post<any>(`${API_URL}/recuperar/verificar`, payload));
      return { success: true, message: response.mensaje || 'Contraseña actualizada. Ahora puedes iniciar sesión.' };
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error && error.error.validationError) {
        return { success: false, message: error.error.validationError };
      }
      if (error instanceof HttpErrorResponse && error.error && error.error.error) {
        return { success: false, message: error.error.error };
      }
      return { success: false, message: 'Error al restablecer la contraseña.' };
    }
  }

  async actualizarPerfilUsuario(idUsuario: string, nuevaClave?: string, preguntaSeguridad?: string, respuestaSeguridad?: string): Promise<{ success: boolean; message: string }> {
    try {
      const payload: any = { idUsuario };
      if (nuevaClave) {
        if (nuevaClave.length < 6) {
          return { success: false, message: 'La nueva contraseña debe tener al menos 6 caracteres.' };
        }
        payload.nuevaClave = nuevaClave;
      }
      if (preguntaSeguridad) {
        payload.preguntaSeguridad = preguntaSeguridad;
        payload.respuestaSeguridad = respuestaSeguridad || '';
      }
      
      const respuesta = await firstValueFrom(this.http.post<any>(`${API_URL}/actualizar-perfil`, payload));
      return { success: true, message: respuesta.mensaje || 'Perfil actualizado exitosamente.' };
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error && error.error.validationError) {
        return { success: false, message: error.error.validationError };
      }
      if (error instanceof HttpErrorResponse && error.error && error.error.error) {
        return { success: false, message: error.error.error };
      }
      return { success: false, message: 'Error al actualizar el perfil.' };
    }
  }

  async eliminarPerfilUsuario(idUsuario: string, confirmacion: string): Promise<{ success: boolean; message: string }> {
    try {
      const payload = { idUsuario, confirmacion };
      const respuesta = await firstValueFrom(this.http.post<any>(`${API_URL}/eliminar-perfil`, payload));
      
      // Cerrar sesión local en éxito
      this.logout();
      return { success: true, message: respuesta.mensaje || 'Perfil eliminado exitosamente.' };
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.error && error.error.validationError) {
        return { success: false, message: error.error.validationError };
      }
      if (error instanceof HttpErrorResponse && error.error && error.error.error) {
        return { success: false, message: error.error.error };
      }
      return { success: false, message: 'Error al eliminar el perfil.' };
    }
  }

  logout() {
    this.loggedIn.set(false);
    this.profileData.set(null);
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }

  private saveToStorage() {
    const payload = {
      loggedIn: this.loggedIn(),
      profile: this.profileData()
    };
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(payload));
  }

  private loadFromStorage() {
    try {
      const stored = localStorage.getItem(AUTH_STORAGE_KEY);
      if (!stored) {
        return;
      }
      const payload = JSON.parse(stored) as { loggedIn: boolean; profile: UserProfile };
      if (payload?.loggedIn) {
        this.loggedIn.set(true);
        this.profileData.set(payload.profile);
      }
    } catch {
      // Ignorar si el almacenamiento no es válido
    }
  }
}

import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService { // <-- Asegúrate de que la clase se llame así
  // La propiedad que busca el HTML debe estar aquí escrita exactamente igual
  isDarkMode = signal<boolean>(false); 

  toggleTheme() {
    if (this.isDarkMode()) {
      this.isDarkMode.set(false);
      document.documentElement.classList.remove('dark-mode');
    } else {
      this.isDarkMode.set(true);
      document.documentElement.classList.add('dark-mode');
    }
  }
}
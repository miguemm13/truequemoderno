// Normaliza textos de estado que vienen del API Java
export type EstadoTransaccion = 'Solicitada' | 'Iniciada' | 'Finalizado con éxito' | 'Cancelada';

export function normalizarEstado(estado: string): EstadoTransaccion {
  if (!estado) return 'Solicitada';
  const e = estado.toLowerCase();
  if (e.includes('finalizado')) return 'Finalizado con éxito';
  if (e.includes('cancelada')) return 'Cancelada';
  if (e.includes('iniciada')) return 'Iniciada';
  return 'Solicitada';
}

export function ofertaActiva(estado: string): boolean {
  return (estado || '').toLowerCase() === 'activa';
}

export function intercambioFinalizado(estado: string): boolean {
  return (estado || '').toLowerCase().includes('finalizado');
}

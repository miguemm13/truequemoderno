export const CATEGORIAS = [
  { id: 'CAT-001', nombre: 'Hogar y Bricolaje' },
  { id: 'CAT-002', nombre: 'Clases y Tutorías' },
  { id: 'CAT-003', nombre: 'Tecnología y Soporte' },
  { id: 'CAT-004', nombre: 'Idiomas y Traducción' },
  { id: 'CAT-005', nombre: 'Salud y Cuidado' },
  { id: 'CAT-006', nombre: 'Cocina y Gastronomía' },
  { id: 'CAT-007', nombre: 'Programación y Software' },
  { id: 'CAT-008', nombre: 'Diseño y Creatividad' },
  { id: 'CAT-009', nombre: 'Música y Arte' },
  { id: 'CAT-010', nombre: 'Deportes y Bienestar' },
  { id: 'CAT-011', nombre: 'Mascotas y Jardinería' },
  { id: 'CAT-012', nombre: 'Otros' }
] as const;

export const CATEGORIAS_MAP: Record<string, string> = Object.fromEntries(
  CATEGORIAS.map(c => [c.id, c.nombre])
);

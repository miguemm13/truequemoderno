package model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class OfertaTalento {
    private String id_oferta;
    private String id_creador;
    private String titulo;
    private String descripcion;
    private String categoria;
    private double horas_estimadas;
    private String fecha_publicacion;
    private String estado; // e.g. "Activa" or "DISPONIBLE"

    public OfertaTalento() {
    }

    public OfertaTalento(String id_oferta, String id_creador, String titulo, String descripcion, 
                         String categoria, double horas_estimadas, String estado) {
        this.id_oferta = id_oferta;
        this.id_creador = id_creador;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.horas_estimadas = horas_estimadas;
        this.estado = estado;
        
        // Auto stamp current timestamp in ISO 8601
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getDefault());
        this.fecha_publicacion = sdf.format(new Date());
    }

    // Getters and Setters
    public String getId_oferta() {
        return id_oferta;
    }

    public void setId_oferta(String id_oferta) {
        this.id_oferta = id_oferta;
    }

    public String getId_creador() {
        return id_creador;
    }

    public void setId_creador(String id_creador) {
        this.id_creador = id_creador;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getHoras_estimadas() {
        return horas_estimadas;
    }

    public void setHoras_estimadas(double horas_estimadas) {
        this.horas_estimadas = horas_estimadas;
    }

    public String getFecha_publicacion() {
        return fecha_publicacion;
    }

    public void setFecha_publicacion(String fecha_publicacion) {
        this.fecha_publicacion = fecha_publicacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}

package model;

import java.time.OffsetDateTime;

public class Calificacion {
    private String id_calificacion;
    private String id_transaccion;
    private String id_evaluador;
    private String id_calificado;
    private int estrellas;
    private String comentario;
    private String fecha_creacion;

    public Calificacion() {
    }

    public Calificacion(String id_calificacion, String id_transaccion, String id_evaluador, 
                        String id_calificado, int estrellas, String comentario) {
        this.id_calificacion = id_calificacion;
        this.id_transaccion = id_transaccion;
        this.id_evaluador = id_evaluador;
        this.id_calificado = id_calificado;
        this.estrellas = estrellas;
        this.comentario = comentario;
        this.fecha_creacion = OffsetDateTime.now().toString();
    }

    // Getters and Setters
    public String getId_calificacion() {
        return id_calificacion;
    }

    public void setId_calificacion(String id_calificacion) {
        this.id_calificacion = id_calificacion;
    }

    public String getId_transaccion() {
        return id_transaccion;
    }

    public void setId_transaccion(String id_transaccion) {
        this.id_transaccion = id_transaccion;
    }

    public String getId_evaluador() {
        return id_evaluador;
    }

    public void setId_evaluador(String id_evaluador) {
        this.id_evaluador = id_evaluador;
    }

    public String getId_calificado() {
        return id_calificado;
    }

    public void setId_calificado(String id_calificado) {
        this.id_calificado = id_calificado;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getFecha_creacion() {
        return fecha_creacion;
    }

    public void setFecha_creacion(String fecha_creacion) {
        this.fecha_creacion = fecha_creacion;
    }
}

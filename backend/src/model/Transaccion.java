package model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Transaccion {
    private String id_transaccion;
    private String id_oferta;
    private String id_receptor;
    private String id_prestador;
    private double horas;
    private String estado; // "Iniciada"
    private String fecha_creacion;

    public Transaccion() {
    }

    public Transaccion(String id_transaccion, double horas, String estado) {
        this.id_transaccion = id_transaccion;
        this.horas = horas;
        this.estado = estado;

        // Auto stamp creation date in ISO 8601
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getDefault());
        this.fecha_creacion = sdf.format(new Date());
    }

    // Sequence Diagram methods
    public void asignarReceptor(String idReceptor) {
        this.id_receptor = idReceptor;
    }

    public void asignarPrestador(String idPrestador) {
        this.id_prestador = idPrestador;
    }

    // Getters and Setters
    public String getId_transaccion() {
        return id_transaccion;
    }

    public void setId_transaccion(String id_transaccion) {
        this.id_transaccion = id_transaccion;
    }

    public String getId_oferta() {
        return id_oferta;
    }

    public void setId_oferta(String id_oferta) {
        this.id_oferta = id_oferta;
    }

    public String getId_receptor() {
        return id_receptor;
    }

    public String getId_prestador() {
        return id_prestador;
    }

    public double getHoras() {
        return horas;
    }

    public void setHoras(double horas) {
        this.horas = horas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFecha_creacion() {
        return fecha_creacion;
    }

    public void setFecha_creacion(String fecha_creacion) {
        this.fecha_creacion = fecha_creacion;
    }

    /**
     * HU-04: Updates the state of the transaction.
     */
    public void actualizarEstado(String nuevoEstado) {
        this.estado = nuevoEstado;
    }

    /**
     * HU-04: Returns the agreed hours.
     */
    public double getHorasPactadas() {
        return this.horas;
    }
}

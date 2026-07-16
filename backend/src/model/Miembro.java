package model;

import java.util.List;

public class Miembro {
    private String id;
    private String nombre; // encrypted via AES-256
    private String correo; // plain text
    private String contrasena; // SHA-256 hashed
    private List<String> habilidades;
    private List<String> demandas;
    private String estado;
    private String telefono; // e.g. "ACTIVO"
    private String preguntaSeguridad;
    private String respuestaSeguridad; // encrypted via AES-256
    private Billetera billetera;
    private double reputacion = 0.0;
    private int totalResenas = 0;

    public Miembro() {
        this.demandas = new java.util.ArrayList<>();
    }

    public Miembro(String id, String nombre, String correo, String contrasena, 
                   List<String> habilidades, String estado, String preguntaSeguridad, 
                   String respuestaSeguridad) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.habilidades = habilidades;
        this.demandas = new java.util.ArrayList<>();
        this.estado = estado;
        this.preguntaSeguridad = preguntaSeguridad;
        this.respuestaSeguridad = respuestaSeguridad;
    }

    public void asignarBilletera(Billetera refBilletera) {
        this.billetera = refBilletera;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public List<String> getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(List<String> habilidades) {
        this.habilidades = habilidades;
    }

    public List<String> getDemandas() {
        if (this.demandas == null) {
            this.demandas = new java.util.ArrayList<>();
        }
        return demandas;
    }

    public void setDemandas(List<String> demandas) {
        this.demandas = demandas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }


            public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
            }

    public String getPreguntaSeguridad() {
        return preguntaSeguridad;
    }

    public void setPreguntaSeguridad(String preguntaSeguridad) {
        this.preguntaSeguridad = preguntaSeguridad;
    }

    public String getRespuestaSeguridad() {
        return respuestaSeguridad;
    }

    public void setRespuestaSeguridad(String respuestaSeguridad) {
        this.respuestaSeguridad = respuestaSeguridad;
    }

    public Billetera getBilletera() {
        return billetera;
    }

    public void setBilletera(Billetera billetera) {
        this.billetera = billetera;
    }

    public double getReputacion() {
        return reputacion;
    }

    public void setReputacion(double reputacion) {
        this.reputacion = reputacion;
    }

    public int getTotalResenas() {
        return totalResenas;
    }

    public void setTotalResenas(int totalResenas) {
        this.totalResenas = totalResenas;
    }

    public double recalcularPromedio(int estrellas) {
        double sumaTotal = (this.reputacion * this.totalResenas) + estrellas;
        this.totalResenas += 1;
        double promedio = sumaTotal / this.totalResenas;
        this.reputacion = Math.round(promedio * 10.0) / 10.0;
        return this.reputacion;
    }
}

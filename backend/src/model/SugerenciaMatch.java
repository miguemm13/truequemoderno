package model;

public class SugerenciaMatch {
    private String idSugerencia;
    private String idSocioSugeridoCifrado;
    private String habilidadOfrecida;
    private String habilidadDemandada;
    private double prioridadPuntaje;

    private String idSocioSugerido;

    public SugerenciaMatch(String id, String socioCifrado, String ofrece, String demanda, double score) {
        this.idSugerencia = id;
        this.idSocioSugeridoCifrado = socioCifrado;
        this.habilidadOfrecida = ofrece;
        this.habilidadDemandada = demanda;
        this.prioridadPuntaje = score;
        try {
            this.idSocioSugerido = service.SeguridadService.descifrarDatos(socioCifrado);
        } catch (Exception e) {
            this.idSocioSugerido = null;
        }
    }

    // Getters and Setters
    public String getIdSugerencia() {
        return idSugerencia;
    }

    public void setIdSugerencia(String idSugerencia) {
        this.idSugerencia = idSugerencia;
    }

    public String getIdSocioSugerido() {
        return idSocioSugerido;
    }

    public void setIdSocioSugerido(String idSocioSugerido) {
        this.idSocioSugerido = idSocioSugerido;
    }

    public String getIdSocioSugeridoCifrado() {
        return idSocioSugeridoCifrado;
    }

    public void setIdSocioSugeridoCifrado(String idSocioSugeridoCifrado) {
        this.idSocioSugeridoCifrado = idSocioSugeridoCifrado;
    }

    public String getHabilidadOfrecida() {
        return habilidadOfrecida;
    }

    public void setHabilidadOfrecida(String habilidadOfrecida) {
        this.habilidadOfrecida = habilidadOfrecida;
    }

    public String getHabilidadDemandada() {
        return habilidadDemandada;
    }

    public void setHabilidadDemandada(String habilidadDemandada) {
        this.habilidadDemandada = habilidadDemandada;
    }

    public double getPrioridadPuntaje() {
        return prioridadPuntaje;
    }

    public void setPrioridadPuntaje(double prioridadPuntaje) {
        this.prioridadPuntaje = prioridadPuntaje;
    }
}

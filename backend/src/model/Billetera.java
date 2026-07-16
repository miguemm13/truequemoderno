package model;

// Saldo de horas de vida y tope de deuda (-2.00 por defecto)
public class Billetera {
    private double saldo;
    private double limite;

    public Billetera() {
    }

    public Billetera(double saldo, double limite) {
        this.saldo = saldo;
        this.limite = limite;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public double getLimite() {
        return limite;
    }

    public void setLimite(double limite) {
        this.limite = limite;
    }

    public boolean verificarSolvencia(double horas) {
        return (this.saldo - horas) >= this.limite;
    }

    public void liberarSaldoPreventivo(double horas) {
        System.out.println("SIVC Billetera: liberadas " + horas + "h de reserva preventiva.");
    }

    public void debitarHoras(double horas) {
        this.saldo -= horas;
    }

    public void acreditarHoras(double horas) {
        this.saldo += horas;
    }
}

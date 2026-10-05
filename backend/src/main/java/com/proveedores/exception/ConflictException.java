package com.proveedores.exception;

public class ConflictException extends RuntimeException {

    private final String code;
    private final Long depositanteId;
    private final String tipoIdentificacion;

    public ConflictException(String message) {
        this(message, null, null, null);
    }

    public ConflictException(String message, String code, Long depositanteId, String tipoIdentificacion) {
        super(message);
        this.code = code;
        this.depositanteId = depositanteId;
        this.tipoIdentificacion = tipoIdentificacion;
    }

    public String getCode() { return code; }
    public Long getDepositanteId() { return depositanteId; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
}

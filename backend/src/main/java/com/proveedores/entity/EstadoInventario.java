package com.proveedores.entity;

public enum EstadoInventario {
    DISPONIBLE,
    EN_EXHIBICION,
    EN_RESTAURACION,
    PRESTADO,
    // Baja patrimonial u operativa; el borrado logico se modela con EntidadBase.
    BAJA
}

package com.proveedores.testfixture;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.EstadoConservacion;
import java.util.Set;

public final class ObjetoMuseoTestFixture {

    private ObjetoMuseoTestFixture() {
    }

    public static ObjetoMuseoRequestDTO valido(String numeroInventario, String denominacion) {
        return valido(numeroInventario, denominacion, 1L);
    }

    public static ObjetoMuseoRequestDTO valido(String numeroInventario, String denominacion, Long depositanteId) {
        return new ObjetoMuseoRequestDTO(
                numeroInventario,
                denominacion,
                "Descripcion de prueba",
                "Descripcion tecnica de prueba",
                "Material de prueba",
                "10 cm",
                EstadoConservacion.BUENO,
                Set.of(1L),
                null,
                depositanteId,
                CaracterRecepcionObjeto.DONACION,
                null
        );
    }

    public static ObjetoMuseoRequestDTO completar(ObjetoMuseoRequestDTO base, Long depositanteId) {
        return new ObjetoMuseoRequestDTO(
                base.numeroInventario(),
                base.denominacionObjeto(),
                base.descripcion(),
                valorO(base.descripcionTecnica(), "Descripcion tecnica de prueba"),
                valorO(base.materiales(), "Material de prueba"),
                valorO(base.dimensiones(), "10 cm"),
                base.estadoConservacion() == null ? EstadoConservacion.BUENO : base.estadoConservacion(),
                base.categoriaIds() == null || base.categoriaIds().isEmpty() ? Set.of(1L) : base.categoriaIds(),
                base.ubicacionId(),
                depositanteId,
                base.caracterRecepcion() == null ? CaracterRecepcionObjeto.DONACION : base.caracterRecepcion(),
                base.fechaVencimiento()
        );
    }

    private static String valorO(String valor, String predeterminado) {
        return valor == null || valor.isBlank() ? predeterminado : valor;
    }
}

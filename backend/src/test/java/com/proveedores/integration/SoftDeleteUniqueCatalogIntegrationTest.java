package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.DetalleConservacionRequestDTO;
import com.proveedores.dto.RangoMilitarRequestDTO;
import com.proveedores.dto.UbicacionRequestDTO;
import com.proveedores.dto.UnidadMilitarRequestDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.exception.ConflictException;
import com.proveedores.service.CategoriaObjetoService;
import com.proveedores.service.DetalleConservacionService;
import com.proveedores.service.RangoMilitarService;
import com.proveedores.service.UbicacionService;
import com.proveedores.service.UnidadMilitarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SoftDeleteUniqueCatalogIntegrationTest extends IntegrationTestBase {

    @Autowired CategoriaObjetoService categoriaObjetoService;
    @Autowired UbicacionService ubicacionService;
    @Autowired RangoMilitarService rangoMilitarService;
    @Autowired UnidadMilitarService unidadMilitarService;
    @Autowired DetalleConservacionService detalleConservacionService;

    @Test
    void categoriaSeReactivaConElMismoIdYDuplicadoActivoEsConflicto() {
        var request = new CategoriaObjetoRequestDTO("IT Categoria reactivable", "Inicial");
        var creada = categoriaObjetoService.crear(request);
        assertThatThrownBy(() -> categoriaObjetoService.crear(request)).isInstanceOf(ConflictException.class);

        categoriaObjetoService.bajaLogica(creada.id());
        var reactivada = categoriaObjetoService.crear(new CategoriaObjetoRequestDTO(request.nombre(), "Actualizada"));

        assertThat(reactivada.id()).isEqualTo(creada.id());
        assertThat(reactivada.descripcion()).isEqualTo("Actualizada");
    }

    @Test
    void ubicacionSeReactivaConElMismoIdYDuplicadoActivoEsConflicto() {
        var request = new UbicacionRequestDTO("IT Ubicacion reactivable", "Inicial");
        var creada = ubicacionService.crear(request);
        assertThatThrownBy(() -> ubicacionService.crear(request)).isInstanceOf(ConflictException.class);

        ubicacionService.bajaLogica(creada.id());
        var reactivada = ubicacionService.crear(new UbicacionRequestDTO(request.nombre(), "Actualizada"));

        assertThat(reactivada.id()).isEqualTo(creada.id());
        assertThat(reactivada.descripcion()).isEqualTo("Actualizada");
    }

    @Test
    void rangoSeReactivaConElMismoIdYDuplicadoActivoEsConflicto() {
        var request = new RangoMilitarRequestDTO(Fuerza.CIVIL, "IT Rango reactivable", 999);
        var creado = rangoMilitarService.crear(request);
        assertThatThrownBy(() -> rangoMilitarService.crear(request)).isInstanceOf(ConflictException.class);

        rangoMilitarService.bajaLogica(creado.id());
        var reactivado = rangoMilitarService.crear(new RangoMilitarRequestDTO(request.fuerza(), request.nombre(), 1000));

        assertThat(reactivado.id()).isEqualTo(creado.id());
        assertThat(reactivado.ordenJerarquico()).isEqualTo(1000);
    }

    @Test
    void unidadSeReactivaConElMismoIdYDuplicadoActivoEsConflicto() {
        var request = new UnidadMilitarRequestDTO(Fuerza.CIVIL, "IT Unidad reactivable", "ITUR", "Test", "Inicial");
        var creada = unidadMilitarService.crear(request);
        assertThatThrownBy(() -> unidadMilitarService.crear(request)).isInstanceOf(ConflictException.class);

        unidadMilitarService.bajaLogica(creada.id());
        var reactivada = unidadMilitarService.crear(new UnidadMilitarRequestDTO(request.fuerza(), request.nombre(), "ITUR2", "Test", "Actualizada"));

        assertThat(reactivada.id()).isEqualTo(creada.id());
        assertThat(reactivada.sigla()).isEqualTo("ITUR2");
    }

    @Test
    void detalleConservacionSeReactivaConElMismoIdYDuplicadoActivoEsConflicto() {
        var request = new DetalleConservacionRequestDTO("IT Detalle reactivable", "IT_DETALLE_REACTIVABLE", "Inicial");
        var creado = detalleConservacionService.crear(request);
        assertThatThrownBy(() -> detalleConservacionService.crear(request)).isInstanceOf(ConflictException.class);

        detalleConservacionService.bajaLogica(creado.id());
        var reactivado = detalleConservacionService.crear(new DetalleConservacionRequestDTO("Nombre actualizado", request.codigo(), "Actualizada"));

        assertThat(reactivado.id()).isEqualTo(creado.id());
        assertThat(reactivado.nombre()).isEqualTo("Nombre actualizado");
    }
}

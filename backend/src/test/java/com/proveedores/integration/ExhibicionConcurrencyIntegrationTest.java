package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.TipoExhibicion;
import com.proveedores.exception.ConflictException;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.service.ExhibicionService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.testfixture.ObjetoMuseoTestFixture;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExhibicionConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ExhibicionService exhibicionService;

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private ExhibicionObjetoRepository exhibicionObjetoRepository;

    @Test
    void dosReservasConcurrentesIncompatiblesPersistenUnaSolaRelacion() throws Exception {
        for (int intento = 1; intento <= 5; intento++) {
            var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido(
                    "IT-EXH-RACE-" + intento,
                    "Objeto concurrencia " + intento
            ));
            CountDownLatch preparados = new CountDownLatch(2);
            CountDownLatch largada = new CountDownLatch(1);
            AtomicInteger exitos = new AtomicInteger();
            AtomicInteger conflictos = new AtomicInteger();

            CompletableFuture<Void> primera = reservarEnParalelo(
                    "Reserva concurrente A-" + intento, objeto.id(), preparados, largada, exitos, conflictos
            );
            CompletableFuture<Void> segunda = reservarEnParalelo(
                    "Reserva concurrente B-" + intento, objeto.id(), preparados, largada, exitos, conflictos
            );

            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            largada.countDown();
            CompletableFuture.allOf(primera, segunda).get(20, TimeUnit.SECONDS);

            assertThat(exitos.get()).isEqualTo(1);
            assertThat(conflictos.get()).isEqualTo(1);
            assertThat(exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.id())).hasSize(1);
        }
    }

    private CompletableFuture<Void> reservarEnParalelo(
            String nombre,
            Long objetoId,
            CountDownLatch preparados,
            CountDownLatch largada,
            AtomicInteger exitos,
            AtomicInteger conflictos
    ) {
        return CompletableFuture.runAsync(() -> {
            preparados.countDown();
            try {
                if (!largada.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("La carrera concurrente no pudo iniciar");
                }
                exhibicionService.crear(new ExhibicionRequestDTO(
                        nombre,
                        "Prueba de exclusión concurrente",
                        TipoExhibicion.TEMPORAL,
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(20),
                        EstadoExhibicion.PLANIFICADA,
                        Set.of(objetoId)
                ));
                exitos.incrementAndGet();
            } catch (ConflictException expected) {
                conflictos.incrementAndGet();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
        });
    }
}

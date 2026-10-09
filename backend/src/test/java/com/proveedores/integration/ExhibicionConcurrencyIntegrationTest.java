package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.TipoExhibicion;
import com.proveedores.exception.ConflictException;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.service.ExhibicionService;
import com.proveedores.service.ExhibicionObjetoService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.testfixture.ObjetoMuseoTestFixture;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

    @Autowired
    private ExhibicionObjetoService exhibicionObjetoService;

    @Autowired
    private com.proveedores.repository.ExhibicionRepository exhibicionRepository;

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

    @Test
    void asociacionConcurrenteConCancelacionNuncaQuedaActiva() throws Exception {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-CANCEL-RACE", "Objeto carrera cancelación"));
        LocalDate inicio = LocalDate.now().plusDays(10);
        var exhibicion = exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Cancelación concurrente", null, TipoExhibicion.TEMPORAL,
                inicio, inicio.plusDays(10), EstadoExhibicion.PLANIFICADA, Set.of()
        ));

        ejecutarConcurrentes(
                () -> exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(exhibicion.id(), objeto.id())),
                () -> exhibicionService.cancelar(exhibicion.id())
        );

        assertThat(exhibicionRepository.findById(exhibicion.id())).get()
                .satisfies(actual -> assertThat(actual.getEstado()).isEqualTo(EstadoExhibicion.CANCELADA));
        assertThat(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.id())).isEmpty();
    }

    @Test
    void asociacionConcurrenteConFinalizacionNuncaQuedaEnExhibicion() throws Exception {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-FINISH-RACE", "Objeto carrera finalización"));
        var exhibicion = exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Finalización concurrente", null, TipoExhibicion.TEMPORAL,
                LocalDate.now(), LocalDate.now().plusDays(10), EstadoExhibicion.ACTIVA, Set.of()
        ));

        ejecutarConcurrentes(
                () -> exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(exhibicion.id(), objeto.id())),
                () -> exhibicionService.finalizar(exhibicion.id())
        );

        assertThat(exhibicionRepository.findById(exhibicion.id())).get()
                .satisfies(actual -> assertThat(actual.getEstado()).isEqualTo(EstadoExhibicion.FINALIZADA));
        assertThat(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.id()))
                .allSatisfy(relacion -> assertThat(relacion.getEstado()).isNotEqualTo(EstadoExhibicionObjeto.EN_EXHIBICION));
    }

    @Test
    void edicionConcurrenteConFinalizacionNoSobrescribeEstadoFinal() throws Exception {
        var exhibicion = exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Edición concurrente", "original", TipoExhibicion.TEMPORAL,
                LocalDate.now(), LocalDate.now().plusDays(10), EstadoExhibicion.ACTIVA, Set.of()
        ));

        ejecutarConcurrentes(
                () -> exhibicionService.actualizar(exhibicion.id(), new ExhibicionRequestDTO(
                        "IT Edición concurrente modificada", "editada", TipoExhibicion.TEMPORAL,
                        LocalDate.now(), LocalDate.now().plusDays(15), EstadoExhibicion.ACTIVA, Set.of()
                )),
                () -> exhibicionService.finalizar(exhibicion.id())
        );

        assertThat(exhibicionRepository.findById(exhibicion.id())).get()
                .satisfies(actual -> assertThat(actual.getEstado()).isEqualTo(EstadoExhibicion.FINALIZADA));
    }

    private void ejecutarConcurrentes(Runnable primeraOperacion, Runnable segundaOperacion) throws Exception {
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicReference<Throwable> errorInesperado = new AtomicReference<>();
        CompletableFuture<Void> primera = ejecutarConBarrera(primeraOperacion, preparados, largada, errorInesperado);
        CompletableFuture<Void> segunda = ejecutarConBarrera(segundaOperacion, preparados, largada, errorInesperado);
        assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
        largada.countDown();
        CompletableFuture.allOf(primera, segunda).get(20, TimeUnit.SECONDS);
        assertThat(errorInesperado.get()).isNull();
    }

    private CompletableFuture<Void> ejecutarConBarrera(
            Runnable operacion,
            CountDownLatch preparados,
            CountDownLatch largada,
            AtomicReference<Throwable> errorInesperado
    ) {
        return CompletableFuture.runAsync(() -> {
            preparados.countDown();
            try {
                if (!largada.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("La carrera concurrente no pudo iniciar");
                }
                operacion.run();
            } catch (ConflictException | com.proveedores.exception.BusinessException expected) {
                // Una de las operaciones puede perder legítimamente la carrera.
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                errorInesperado.compareAndSet(null, interrupted);
            } catch (Throwable unexpected) {
                errorInesperado.compareAndSet(null, unexpected);
            }
        });
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

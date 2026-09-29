package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.proveedores.repository.NumeroInventarioRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumeroInventarioRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private NumeroInventarioRepository repository;

    @Test
    void reservaCorrelativosUnicosEnConcurrenciaYReiniciaPorAnio() throws Exception {
        int anio = 2098;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tareas.add(() -> repository.siguienteCorrelativo(anio));
            }

            List<Integer> correlativos = new ArrayList<>();
            for (Future<Integer> future : executor.invokeAll(tareas)) {
                correlativos.add(future.get());
            }

            assertThat(new HashSet<>(correlativos)).hasSize(20);
            assertThat(correlativos).containsExactlyInAnyOrderElementsOf(
                    java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList());
            assertThat(repository.siguienteCorrelativo(2099)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}

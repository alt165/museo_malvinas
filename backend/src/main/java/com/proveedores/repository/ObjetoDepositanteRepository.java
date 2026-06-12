package com.proveedores.repository;

import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.ObjetoDepositante;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ObjetoDepositanteRepository extends JpaRepository<ObjetoDepositante, Long> {

    List<ObjetoDepositante> findByObjetoMuseoIdAndEliminadoFalse(Long objetoMuseoId);

    Optional<ObjetoDepositante> findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(Long objetoMuseoId);

    List<ObjetoDepositante> findByDepositanteIdAndEliminadoFalse(Long depositanteId);

    @Query("""
            select relacion
            from ObjetoDepositante relacion
            join fetch relacion.objetoMuseo objeto
            join fetch relacion.depositante depositante
            where depositante.id = :depositanteId
              and relacion.activo = true
              and relacion.eliminado = false
              and objeto.activo = true
              and objeto.eliminado = false
            order by objeto.numeroInventario asc
            """)
    List<ObjetoDepositante> findObjetosActivosPorDepositante(Long depositanteId);

    boolean existsByObjetoMuseoIdAndDepositanteIdAndEliminadoFalse(Long objetoMuseoId, Long depositanteId);

    List<ObjetoDepositante> findByTipoDepositoInAndFechaVencimientoBetweenAndActivoTrueAndEliminadoFalseAndObjetoMuseoActivoTrueAndObjetoMuseoEliminadoFalseOrderByFechaVencimientoAsc(
            Collection<CaracterRecepcionObjeto> tiposDeposito,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    );

    @Query("""
            select relacion
            from ObjetoDepositante relacion
            join fetch relacion.objetoMuseo objeto
            join fetch relacion.depositante depositante
            where relacion.tipoDeposito in :tiposDeposito
              and relacion.activo = true
              and relacion.eliminado = false
              and objeto.activo = true
              and objeto.eliminado = false
            order by
              case when relacion.fechaVencimiento is null then 1 else 0 end,
              relacion.fechaVencimiento asc,
              objeto.numeroInventario asc
            """)
    List<ObjetoDepositante> findComodatosPrestamosActivosOrdenados(Collection<CaracterRecepcionObjeto> tiposDeposito);

    @Query("""
            select relacion
            from ObjetoDepositante relacion
            join fetch relacion.objetoMuseo objeto
            join fetch relacion.depositante depositante
            where objeto.id = :objetoMuseoId
              and relacion.activo = true
              and relacion.eliminado = false
              and objeto.activo = true
              and objeto.eliminado = false
            """)
    Optional<ObjetoDepositante> findRelacionActivaPorObjeto(Long objetoMuseoId);
}

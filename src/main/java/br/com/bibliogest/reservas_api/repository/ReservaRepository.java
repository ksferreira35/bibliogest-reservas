package br.com.bibliogest.reservas_api.repository;

import br.com.bibliogest.reservas_api.entities.EstadoReserva;
import br.com.bibliogest.reservas_api.entities.Reserva;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {
    List<Reserva> findByLeitorIdOrderByCriadaEmDesc(String leitorId, Pageable pageable);

    List<Reserva> findByEstadoOrderByCriadaEmDesc(EstadoReserva estado, Pageable pageable);

    List<Reserva> findAllByOrderByCriadaEmDesc(Pageable pageable);

    @Query("select r.exemplarId from Reserva r where r.exemplarId in :ids and r.estado in :estados")
    List<String> findExemplaresBloqueados(@Param("ids") Collection<String> ids,
                                          @Param("estados") Collection<EstadoReserva> estados);
}

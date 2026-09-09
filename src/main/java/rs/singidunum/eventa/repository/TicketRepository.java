package rs.singidunum.eventa.repository;

import rs.singidunum.eventa.domain.Ticket;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByOrderIdOrderByIdAsc(Long orderId);
    Optional<Ticket> findByCode(String code);
    long countByTicketTypeIdAndStatusNot(Long typeId, String status);
    boolean existsByTicketTypeId(Long id);
}

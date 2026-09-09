package rs.singidunum.eventa.repository;

import rs.singidunum.eventa.domain.TicketType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    List<TicketType> findByEventIdOrderByPriceAsc(Long eventId);
    boolean existsByEventId(Long id);
}

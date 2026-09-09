package rs.singidunum.eventa.repository;

import rs.singidunum.eventa.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByUserEmailOrderByCreatedAtDesc(String email);
    List<PurchaseOrder> findByEventId(Long eventId);
    boolean existsByUserId(Long id);
    boolean existsByEventId(Long id);
}

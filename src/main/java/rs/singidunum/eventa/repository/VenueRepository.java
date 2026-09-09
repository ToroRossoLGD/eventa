package rs.singidunum.eventa.repository;

import rs.singidunum.eventa.domain.Venue;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    
}

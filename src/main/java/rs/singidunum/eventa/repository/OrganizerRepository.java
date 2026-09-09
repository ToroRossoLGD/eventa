package rs.singidunum.eventa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.singidunum.eventa.domain.Organizer;
import java.util.List;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    List<Organizer> findAllByOrderByNameAscIdAsc();
    List<Organizer> findByEventsIdOrderByNameAscIdAsc(Long eventId);
}

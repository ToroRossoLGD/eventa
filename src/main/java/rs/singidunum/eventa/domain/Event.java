package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity @Table(name="events")
@Getter @Setter @NoArgsConstructor
public class Event {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=150)
    private String title;
    @Column(nullable=false, length=4000)
    private String description;
    @Column(nullable=false)
    private LocalDateTime startsAt;
    @ManyToOne(optional=false)
    private Venue venue;
    @ManyToOne(optional=false)
    private Category category;
    @ManyToMany
    @JoinTable(name="event_organizers",
        joinColumns=@JoinColumn(name="event_id"),
        inverseJoinColumns=@JoinColumn(name="organizer_id"))
    @OrderBy("name ASC, id ASC")
    private Set<Organizer> organizers = new LinkedHashSet<>();
    @Column(nullable=false, length=20)
    private String status;
    @Column(nullable=false, length=20)
    private String theme;
}

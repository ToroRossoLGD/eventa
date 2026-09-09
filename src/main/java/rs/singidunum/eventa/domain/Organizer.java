package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity @Table(name="organizers")
@Getter @Setter @NoArgsConstructor
public class Organizer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=100)
    private String name;
    @Column(nullable=false, length=180)
    private String email;
    @ManyToMany(mappedBy="organizers")
    private Set<Event> events = new LinkedHashSet<>();
}

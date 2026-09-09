package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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
    @Column(nullable=false, length=20)
    private String status;
    @Column(nullable=false, length=20)
    private String theme;
}

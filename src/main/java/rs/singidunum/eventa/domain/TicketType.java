package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name="ticket_types")
@Getter @Setter @NoArgsConstructor
public class TicketType {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false)
    private Event event;
    @Column(nullable=false, length=100)
    private String name;
    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal price;
    @Column(nullable=false)
    private int quantity;
}

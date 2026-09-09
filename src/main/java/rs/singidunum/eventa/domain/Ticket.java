package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name="tickets")
@Getter @Setter @NoArgsConstructor
public class Ticket {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false)
    private PurchaseOrder order;
    @ManyToOne(optional=false)
    private TicketType ticketType;
    @Column(nullable=false, unique=true, length=36)
    private String code;
    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal unitPrice;
    @Column(nullable=false, length=20)
    private String status;
}

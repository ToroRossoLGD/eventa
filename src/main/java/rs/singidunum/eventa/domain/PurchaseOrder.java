package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name="orders")
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false)
    private AppUser user;
    @ManyToOne(optional=false)
    private Event event;
    @Column(nullable=false)
    private LocalDateTime createdAt;
    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal total;
    @Column(nullable=false, length=20)
    private String status;
}

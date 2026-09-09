package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name="venues")
@Getter @Setter @NoArgsConstructor
public class Venue {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=100)
    private String name;
    @Column(nullable=false, length=200)
    private String address;
    @Column(nullable=false, length=100)
    private String city;
    @Column(nullable=false)
    private int capacity;
}

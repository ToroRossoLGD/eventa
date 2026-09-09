package rs.singidunum.eventa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name="app_users")
@Getter @Setter @NoArgsConstructor
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=100)
    private String name;
    @Column(nullable=false, unique=true, length=180)
    private String email;
    @Column(nullable=false, length=100)
    private String password;
    @Column(nullable=false, length=20)
    private String role;
}

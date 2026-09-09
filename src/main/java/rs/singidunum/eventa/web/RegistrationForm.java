package rs.singidunum.eventa.web;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistrationForm {
    @NotBlank(message="Unesite ime i prezime.") @Size(max=100)
    private String name;
    @NotBlank(message="Unesite email adresu.") @Email(message="Unesite ispravnu email adresu.") @Size(max=180)
    private String email;
    @NotBlank @Size(min=8,max=60,message="Lozinka mora imati između 8 i 60 znakova.")
    private String password;
}

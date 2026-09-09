package rs.singidunum.eventa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.singidunum.eventa.repository.AppUserRepository;
import rs.singidunum.eventa.domain.AppUser;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class AccountService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    @Transactional
    public AppUser register(String name, String email, String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("Lozinka je predugačka u UTF-8 zapisu.");
        email = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new BusinessException("Nalog sa ovom email adresom već postoji.");
        AppUser user = new AppUser();
        user.setName(name.trim()); user.setEmail(email); user.setPassword(encoder.encode(password)); user.setRole("USER");
        return users.save(user);
    }
}

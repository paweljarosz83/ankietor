package pl.ankietor.security.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.ankietor.security.models.Role;
import pl.ankietor.security.models.User;
import pl.ankietor.security.repos.RoleRepository;
import pl.ankietor.security.repos.UserRepository;

import java.util.UUID;

/**
 * Zaklada konto administratora przy pierwszym uruchomieniu.
 *
 * Haslo pochodzi ze zmiennej ANKIETOR_ADMIN_PASSWORD. Jesli jej nie ma,
 * generowane jest losowe i wypisane do logu dokladnie raz - tak samo, jak robi
 * to domyslna konfiguracja Spring Security. Zadne haslo nie jest zapisane
 * w kodzie ani w plikach konfiguracyjnych.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final String ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }

        String fromEnv = System.getenv("ANKIETOR_ADMIN_PASSWORD");
        boolean generated = fromEnv == null || fromEnv.isBlank();
        String password = generated ? UUID.randomUUID().toString() : fromEnv;

        User admin = new User(ADMIN_USERNAME, passwordEncoder.encode(password));
        roleRepository.findByName(Role.ADMIN).ifPresent(admin::addRole);
        roleRepository.findByName(Role.USER).ifPresent(admin::addRole);
        userRepository.save(admin);

        if (generated) {
            log.warn("""

                    Utworzono konto administratora.
                      login  : {}
                      haslo  : {}

                    Haslo wygenerowane losowo, bo brak zmiennej ANKIETOR_ADMIN_PASSWORD.
                    Zapisz je teraz - nie zostanie wypisane ponownie.
                    """, ADMIN_USERNAME, password);
        } else {
            log.info("Utworzono konto administratora '{}' z haslem ze zmiennej "
                    + "ANKIETOR_ADMIN_PASSWORD.", ADMIN_USERNAME);
        }
    }
}

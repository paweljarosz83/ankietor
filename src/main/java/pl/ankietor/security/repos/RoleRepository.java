package pl.ankietor.security.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.ankietor.security.models.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}

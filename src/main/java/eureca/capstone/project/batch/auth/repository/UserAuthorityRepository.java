package eureca.capstone.project.batch.auth.repository;

import eureca.capstone.project.batch.auth.entity.UserAuthority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthorityRepository extends JpaRepository<UserAuthority, Long> {
}

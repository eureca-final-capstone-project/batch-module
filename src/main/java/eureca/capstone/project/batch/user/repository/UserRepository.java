package eureca.capstone.project.batch.user.repository;

import eureca.capstone.project.batch.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

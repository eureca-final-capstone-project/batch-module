package eureca.capstone.project.batch.user.repository;

import eureca.capstone.project.batch.user.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDataRepository extends JpaRepository<UserData, Long> {

}

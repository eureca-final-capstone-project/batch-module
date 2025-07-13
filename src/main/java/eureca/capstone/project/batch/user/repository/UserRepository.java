package eureca.capstone.project.batch.user.repository;

import eureca.capstone.project.batch.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u.id from User u where u.resetDate = :resetDate")
    List<Long> findByResetDate(@Param("resetDate") Integer resetDate);

    List<User> findByIdIn(List<Long> ids);
}


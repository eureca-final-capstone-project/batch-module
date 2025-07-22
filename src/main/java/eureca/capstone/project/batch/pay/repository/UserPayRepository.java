package eureca.capstone.project.batch.pay.repository;

import eureca.capstone.project.batch.pay.entity.UserPay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPayRepository extends JpaRepository<UserPay, Long> {
}

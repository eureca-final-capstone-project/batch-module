package eureca.capstone.project.batch.transaction_feed.repository;


import eureca.capstone.project.batch.transaction_feed.domain.UserDataCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDataCouponRepository extends JpaRepository<UserDataCoupon, Long> {
}

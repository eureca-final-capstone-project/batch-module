package eureca.capstone.project.batch.transaction_feed.repository;

import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.transaction_feed.domain.DataCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DataCouponRepository extends JpaRepository<DataCoupon, Long> {
    Optional<DataCoupon> findByDataAmountAndTelecomCompany(Long dataAmount, TelecomCompany telecomCompany);
}

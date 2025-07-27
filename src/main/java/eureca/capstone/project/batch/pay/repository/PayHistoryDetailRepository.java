package eureca.capstone.project.batch.pay.repository;

import eureca.capstone.project.batch.pay.entity.PayHistoryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayHistoryDetailRepository extends JpaRepository<PayHistoryDetail, Long> {
}

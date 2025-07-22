package eureca.capstone.project.batch.pay.repository;

import eureca.capstone.project.batch.pay.entity.PayHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayHistoryRepository extends JpaRepository<PayHistory, Long> {
}

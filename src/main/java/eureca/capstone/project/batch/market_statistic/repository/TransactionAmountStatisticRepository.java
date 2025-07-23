package eureca.capstone.project.batch.market_statistic.repository;

import eureca.capstone.project.batch.market_statistic.domain.TransactionAmountStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TransactionAmountStatisticRepository extends JpaRepository<TransactionAmountStatistic, Long> {
    boolean existsByStaticsTime(LocalDateTime staticsTime);
}

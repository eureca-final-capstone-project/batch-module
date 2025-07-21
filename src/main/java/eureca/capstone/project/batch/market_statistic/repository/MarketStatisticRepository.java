package eureca.capstone.project.batch.market_statistic.repository;

import eureca.capstone.project.batch.market_statistic.domain.MarketStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketStatisticRepository extends JpaRepository<MarketStatistic, Long> {
}

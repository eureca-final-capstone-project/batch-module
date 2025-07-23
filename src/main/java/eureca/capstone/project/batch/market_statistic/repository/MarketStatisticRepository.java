package eureca.capstone.project.batch.market_statistic.repository;

import eureca.capstone.project.batch.market_statistic.domain.MarketStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Set;

public interface MarketStatisticRepository extends JpaRepository<MarketStatistic, Long> {
    @Query("select ms.telecomCompany.telecomCompanyId " +
            "from MarketStatistic ms where ms.staticsTime = :time")
    Set<Long> findTelecomIdsByStaticsTime(@Param("time") LocalDateTime time);
}

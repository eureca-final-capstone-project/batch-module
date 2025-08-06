package eureca.capstone.project.batch.market_statistic.repository;

import eureca.capstone.project.batch.market_statistic.domain.MarketStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface MarketStatisticRepository extends JpaRepository<MarketStatistic, Long> {
    @Query("select ms.telecomCompany.telecomCompanyId " +
            "from MarketStatistic ms where ms.staticsTime = :time")
    Set<Long> findTelecomIdsByStaticsTime(@Param("time") LocalDateTime time);

    /**
     * 특정 통신사의 특정 시간 이전에 기록된 가장 최근의 유효한(averagePrice is not null) 통계를 찾습니다.
     * @param telecomCompanyId 통신사 ID
     * @param statisticsTime 기준 시간
     * @return 가장 최근의 MarketStatistic (Optional)
     */
    Optional<MarketStatistic> findFirstByTelecomCompanyTelecomCompanyIdAndStaticsTimeBeforeAndAveragePriceIsNotNullOrderByStaticsTimeDesc(Long telecomCompanyId, LocalDateTime statisticsTime);
}

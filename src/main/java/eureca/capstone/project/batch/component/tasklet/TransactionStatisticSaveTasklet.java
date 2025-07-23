package eureca.capstone.project.batch.component.tasklet;

import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.common.repository.TelecomCompanyRepository;
import eureca.capstone.project.batch.component.writer.TransactionStatisticWriter;
import eureca.capstone.project.batch.market_statistic.dto.StatisticResponseDto;
import eureca.capstone.project.batch.market_statistic.domain.MarketStatistic;
import eureca.capstone.project.batch.market_statistic.domain.TransactionAmountStatistic;
import eureca.capstone.project.batch.market_statistic.repository.MarketStatisticRepository;
import eureca.capstone.project.batch.market_statistic.repository.TransactionAmountStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class TransactionStatisticSaveTasklet implements Tasklet {
    private static final int PRICE_PER_MB = 100; // 100MB 단위
    private final TelecomCompanyRepository telecomCompanyRepository;
    private final MarketStatisticRepository marketStatisticRepository;
    private final TransactionAmountStatisticRepository transactionAmountStatisticRepository;

    @Value("#{jobParameters['currentTime']}")
    private String currentTimeStr;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getExecutionContext();

        LocalDateTime statisticsTime = LocalDateTime.parse(currentTimeStr)
                .minusHours(1)
                .truncatedTo(ChronoUnit.HOURS);

        Map<Long, StatisticResponseDto> statistics =
                (Map<Long, StatisticResponseDto>) executionContext.get(TransactionStatisticWriter.STEP_STATISTIC_KEY);

        if (statistics == null) {
            statistics = new HashMap<>();
            log.info("[TransactionStatisticSaveTasklet execute] 거래내역 0건으로 누적통계 map 없음. 빈 map 생성");
        }


        // 전체 통신사 조회 후 각각 저장
        List<TelecomCompany> telecoms = telecomCompanyRepository.findAll();

        if (telecoms.isEmpty()) {
            throw new IllegalArgumentException("TelecomCompany Not Found");
        }

        List<MarketStatistic> marketStats = new ArrayList<>();
        Set<Long> existsIds = marketStatisticRepository.findTelecomIdsByStaticsTime(statisticsTime);
        long totalCount = 0L;

        for (TelecomCompany telecom : telecoms) {
            if (existsIds.contains(telecom.getTelecomCompanyId())) {
                log.warn("[SaveTasklet] 이미 존재하는 통계데이터. {}", telecom.getName());
                continue;
            }
            StatisticResponseDto dto = statistics.getOrDefault(
                    telecom.getTelecomCompanyId(),
                    StatisticResponseDto.builder()
                            .totalPrice(0L)
                            .totalDataAmount(0L)
                            .transactionCount(0L)
                            .build());

            Long avgPrice = dto.getTotalDataAmount() == 0 ? null : Math.round((double) dto.getTotalPrice() / dto.getTotalDataAmount() * PRICE_PER_MB);

            marketStats.add(MarketStatistic.builder()
                    .telecomCompany(telecom)
                    .averagePrice(avgPrice)
                    .transactionAmount(dto.getTransactionCount())
                    .staticsTime(statisticsTime)
                    .build());

            totalCount += dto.getTransactionCount();
        }

        try {
            // 시세 통계 저장
            if (!marketStats.isEmpty()) {
                marketStatisticRepository.saveAll(marketStats);
            }

            // 거래량 통계 저장
            if (!transactionAmountStatisticRepository.existsByStaticsTime(statisticsTime)) {
                transactionAmountStatisticRepository.save(TransactionAmountStatistic.builder()
                        .transactionAmount(totalCount)
                        .staticsTime(statisticsTime)
                        .build());
            }

        } catch (DataIntegrityViolationException e) {
            log.warn("[SaveTasklet] 중복 통계 데이터 발생. time={}, {}", statisticsTime, e.getMessage());
        } catch (Exception e) {
            log.error("[SaveTasklet] 통계 데이터 DB 저장 중 예상치 못한 오류 발생. time={}. {}", statisticsTime, e.getMessage(), e);
            throw e;
        }
        log.info("[SaveTasklet] 저장 완료. time={}, marketStats={}, totalCnt={}", statisticsTime, marketStats.size(), totalCount);
        return RepeatStatus.FINISHED;
    }
}

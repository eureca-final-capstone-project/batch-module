package eureca.capstone.project.batch.component.tasklet;

import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.common.repository.TelecomCompanyRepository;
import eureca.capstone.project.batch.component.writer.TransactionStatisticWriter;
import eureca.capstone.project.batch.dto.StatisticResponseDto;
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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Map<Long, StatisticResponseDto> statistics =
                (Map<Long, StatisticResponseDto>) executionContext.get(TransactionStatisticWriter.STEP_STATISTIC_KEY);

        if (statistics == null) {
            statistics = new HashMap<>();
            log.info("[TransactionStatisticSaveTasklet execute] 거래내역 0건으로 누적통계 map 없음. 빈 map 생성");
        }


        // 3) 전체 통신사 조회 후 각각 저장 (없으면 0)
        List<TelecomCompany> telecoms = telecomCompanyRepository.findAll();

        if (telecoms.isEmpty()) {
            throw new IllegalArgumentException("TelecomCompany Not Found");
        }

        List<MarketStatistic> marketStats = new ArrayList<>();
        long totalCount = 0L;

        for (TelecomCompany telecom : telecoms) {
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

        // 시세 통계 저장
        marketStatisticRepository.saveAll(marketStats);

        // 거래량 통계 저장
        transactionAmountStatisticRepository.save(TransactionAmountStatistic.builder()
                .transactionAmount(totalCount)
                .staticsTime(statisticsTime)
                .build());

        log.info("[SaveTasklet] 저장 완료. time={}, marketStats={}, totalCnt={}", statisticsTime, marketStats.size(), totalCount);

        return RepeatStatus.FINISHED;
    }
}

package eureca.capstone.project.batch.component.tasklet;

import eureca.capstone.project.batch.component.writer.BidVolumeStatisticWriter;
import eureca.capstone.project.batch.market_statistic.domain.TransactionAmountStatistic;
import eureca.capstone.project.batch.market_statistic.repository.TransactionAmountStatisticRepository;
import eureca.capstone.project.batch.transaction_feed.entity.SalesType;
import eureca.capstone.project.batch.transaction_feed.repository.SalesTypeRepository;
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

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class BidStatisticSaveTasklet implements Tasklet {
    private final TransactionAmountStatisticRepository transactionAmountStatisticRepository;
    private final SalesTypeRepository salesTypeRepository;

    @Value("#{jobParameters['currentTime']}")
    private String currentTimeStr;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getExecutionContext();

        LocalDateTime statisticsTime = LocalDateTime.parse(currentTimeStr)
                .minusDays(1)
                .truncatedTo(ChronoUnit.DAYS);

        Long totalVolumeCount = executionContext.getLong(BidVolumeStatisticWriter.VOLUME_STATISTIC_KEY, 0L);
        log.info("[NormalStatisticSaveTasklet] volume 총합 읽기: {}", totalVolumeCount);

        SalesType salesType = salesTypeRepository.findByName("입찰 판매")
                .orElseThrow(()->new IllegalArgumentException("SalesType Not Found"));
        String statisticType = "DAY";

        try {
            // 거래량 통계 저장
            if (!transactionAmountStatisticRepository.existsByStaticsTimeAndSalesType(statisticsTime, salesType)) {
                transactionAmountStatisticRepository.save(TransactionAmountStatistic.builder()
                        .transactionAmount(totalVolumeCount)
                        .staticsTime(statisticsTime)
                        .salesType(salesType)
                        .statisticType(statisticType)
                        .build());

                log.info("[NormalStatisticSaveTasklet] 저장 완료. time={}, totalCnt={}", statisticsTime, totalVolumeCount);
            }

        } catch (DataIntegrityViolationException e) {
            log.warn("[NormalStatisticSaveTasklet] 중복 통계 데이터 발생. time={}, {}", statisticsTime, e.getMessage());
        } catch (Exception e) {
            log.error("[NormalStatisticSaveTasklet] 통계 데이터 DB 저장 중 예상치 못한 오류 발생. time={}. {}", statisticsTime, e.getMessage(), e);
            throw e;
        }
        return RepeatStatus.FINISHED;
    }
}

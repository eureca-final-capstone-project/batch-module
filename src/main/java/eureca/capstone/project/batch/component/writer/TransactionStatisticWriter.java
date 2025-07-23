package eureca.capstone.project.batch.component.writer;

import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.market_statistic.dto.StatisticResponseDto;
import eureca.capstone.project.batch.transaction_feed.entity.DataTransactionHistory;
import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class TransactionStatisticWriter implements ItemWriter<DataTransactionHistory> {
    public static final String STEP_STATISTIC_KEY = "telecomStatistic";
    private StepExecution stepExecution;
    private Map<Long, StatisticResponseDto> statistics; // (통신사 id,계산)

    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        ExecutionContext executionContext = stepExecution.getExecutionContext();

        this.statistics = (Map<Long, StatisticResponseDto>) executionContext.get(STEP_STATISTIC_KEY);

        if (this.statistics == null) {
            this.statistics = new HashMap<>();
            executionContext.put(STEP_STATISTIC_KEY, this.statistics);
            log.info("[saveStepExecution] 기존 누적 map 없음. 새 누적 map 생성.");
        } else {
            log.info("[saveStepExecution] 기존 누적 map 로드 (재시작)");
        }
    }

    @Override
    public void write(Chunk<? extends DataTransactionHistory> chunk) {

        for (DataTransactionHistory history : chunk.getItems()) {
            TransactionFeed feed = history.getTransactionFeed();
            if(feed == null){
                log.warn("[write] {}번 거래내역의 TransactionFeed가 null", history.getTransactionHistoryId());
                continue;
            }
            TelecomCompany telecom = feed.getTelecomCompany();
            if(telecom == null){
                log.warn("[write] {}번 거래내역의 TelecomCompany가 null", history.getTransactionHistoryId());
                continue;
            }

            Long telecomId = history.getTransactionFeed().getTelecomCompany().getTelecomCompanyId();

            StatisticResponseDto statisticDto = statistics.getOrDefault(
                    telecomId,
                    StatisticResponseDto.builder()
                        .totalPrice(0L)
                        .totalDataAmount(0L)
                        .transactionCount(0L)
                        .build()
            );

            Long price = history.getTransactionFinalPrice() == null ? 0L : history.getTransactionFinalPrice();
            Long dataAmount = history.getTransactionFeed().getSalesDataAmount() == null ? 0L : history.getTransactionFeed().getSalesDataAmount();

            statisticDto = StatisticResponseDto.builder()
                    .totalPrice(statisticDto.getTotalPrice() + price)
                    .totalDataAmount(statisticDto.getTotalDataAmount() + dataAmount)
                    .transactionCount(statisticDto.getTransactionCount() + 1)
                    .build();

            log.info("[write] 통신사: {}, price: {}, amount: {}", telecom.getName(), history.getTransactionFinalPrice(), feed.getSalesDataAmount());

            statistics.put(telecomId, statisticDto);
        }

        stepExecution.getExecutionContext().put(STEP_STATISTIC_KEY, statistics);
        log.info("[write] 누적 집계 완료. 거래내역: {}건, 실 집계: {}개 통신사", chunk.getItems().size(), statistics.size());
    }
}

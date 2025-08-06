package eureca.capstone.project.batch.component.writer;

import eureca.capstone.project.batch.market_statistic.dto.StatisticResponseDto;
import eureca.capstone.project.batch.market_statistic.dto.TransactionHistoryStatisticDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
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
public class NormalStatisticWriter
        implements ItemWriter<TransactionHistoryStatisticDto> {

    public static final String NORMAL_STATISTIC_KEY = "telecomStatistic";

    private StepExecution stepExecution;
    private Map<Long, StatisticResponseDto> statistics;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        ExecutionContext ctx = stepExecution.getExecutionContext();

        // 재시작 시 기존 맵 로드
        this.statistics = (Map<Long, StatisticResponseDto>) ctx.get(NORMAL_STATISTIC_KEY);
        if (this.statistics == null) {
            this.statistics = new HashMap<>();
            log.info("[NormalStatisticWriter] 새 누적 map 생성");
        } else {
            log.info("[NormalStatisticWriter] 재시작: 기존 누적 map 로드");
        }
    }

    @Override
    public void write(Chunk<? extends TransactionHistoryStatisticDto> chunk) {
        for (TransactionHistoryStatisticDto dto : chunk.getItems()) {
            if (dto == null || dto.getTelecomCompanyId() == null) {
                log.warn("[NormalStatisticWriter] 유효하지 않은 DTO, 건너뜁니다: {}", dto);
                continue;
            }

            long id = dto.getTelecomCompanyId();
            long price = dto.getTransactionFinalPrice()  == null ? 0L : dto.getTransactionFinalPrice();
            long amount = dto.getSalesDataAmount()      == null ? 0L : dto.getSalesDataAmount();

            StatisticResponseDto prev = statistics.getOrDefault(
                    id,
                    StatisticResponseDto.builder()
                            .totalPrice(0L)
                            .totalDataAmount(0L)
                            .transactionCount(0L)
                            .build()
            );

            statistics.put(id,
                    StatisticResponseDto.builder()
                            .totalPrice(prev.getTotalPrice() + price)
                            .totalDataAmount(prev.getTotalDataAmount() + amount)
                            .transactionCount(prev.getTransactionCount() + 1)
                            .build()
            );
        }
        log.debug("[NormalStatisticWriter] 누적 집계 완료: items={}, companies={}",
                chunk.getItems().size(), statistics.size());
         stepExecution.getExecutionContext().put(NORMAL_STATISTIC_KEY, statistics);
    }
}

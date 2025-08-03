package eureca.capstone.project.batch.component.writer;

import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.market_statistic.dto.StatisticResponseDto;
import eureca.capstone.project.batch.market_statistic.dto.TransactionHistoryStatisticDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class NormalStatisticWriter implements ItemWriter<TransactionHistoryStatisticDto> {
    public static final String NORMAL_STATISTIC_KEY = "telecomStatistic";
    private StepExecution stepExecution;
    private Map<Long, StatisticResponseDto> statistics;

    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        ExecutionContext executionContext = stepExecution.getExecutionContext();

        this.statistics = (Map<Long, StatisticResponseDto>) executionContext.get(NORMAL_STATISTIC_KEY);

        if (this.statistics == null) {
            this.statistics = new HashMap<>();
            executionContext.put(NORMAL_STATISTIC_KEY, this.statistics);
            log.info("[NormalStatisticWriter] 기존 누적 map 없음. 새 누적 map 생성.");
        } else {
            log.info("[NormalStatisticWriter] 기존 누적 map 로드 (재시작)");
        }
    }

    @Override
    public void write(Chunk<? extends TransactionHistoryStatisticDto> chunk) {

//        for (DataTransactionHistory history : chunk.getItems()) {
//            TransactionFeed feed = history.getTransactionFeed();
//            if(feed == null){
//                log.warn("[NormalStatisticWriter] {}번 거래내역의 TransactionFeed가 null", history.getTransactionHistoryId());
//                continue;
//            }
//            TelecomCompany telecom = feed.getTelecomCompany();
//            if(telecom == null){
//                log.warn("[NormalStatisticWriter] {}번 거래내역의 TelecomCompany가 null", history.getTransactionHistoryId());
//                continue;
//            }
//
//            Long telecomId = history.getTransactionFeed().getTelecomCompany().getTelecomCompanyId();
//
//            StatisticResponseDto statisticDto = statistics.getOrDefault(
//                    telecomId,
//                    StatisticResponseDto.builder()
//                            .totalPrice(0L)
//                            .totalDataAmount(0L)
//                            .transactionCount(0L)
//                            .build()
//            );
//
//            Long price = history.getTransactionFinalPrice() == null ? 0L : history.getTransactionFinalPrice();
//            Long dataAmount = history.getTransactionFeed().getSalesDataAmount() == null ? 0L : history.getTransactionFeed().getSalesDataAmount();
//
//            statisticDto = StatisticResponseDto.builder()
//                    .totalPrice(statisticDto.getTotalPrice() + price)
//                    .totalDataAmount(statisticDto.getTotalDataAmount() + dataAmount)
//                    .transactionCount(statisticDto.getTransactionCount() + 1)
//                    .build();
//
//            log.info("[NormalStatisticWriter] 통신사: {}, price: {}, amount: {}", telecom.getName(), history.getTransactionFinalPrice(), feed.getSalesDataAmount());
//
//            statistics.put(telecomId, statisticDto);
//        }
//
//        stepExecution.getExecutionContext().put(NORMAL_STATISTIC_KEY, statistics);
//        log.info("[NormalStatisticWriter] 누적 집계 완료. 거래내역: {}건, 실 집계: {}개 통신사", chunk.getItems().size(), statistics.size());
//    }

        for (TransactionHistoryStatisticDto dto : chunk.getItems()) {
//            if(dto.getTelecomCompanyId() == 3){
//                throw new DataIntegrityViolationException("DataIntegrityViolationException 발생: telecomId=" + dto.getTelecomCompanyId());
//            }
            if (dto == null) {
                log.warn("[NormalStatisticWriter] DTO가 null입니다. 건너뜁니다.");
                continue;
            }

            Long telecomId = dto.getTelecomCompanyId();
            if (telecomId == null) {
                log.warn("[NormalStatisticWriter] telecomCompanyId가 null인 거래내역 DTO: {}", dto);
                continue;
            }

            Long price = dto.getTransactionFinalPrice() == null ? 0L : dto.getTransactionFinalPrice();
            Long dataAmount = dto.getSalesDataAmount() == null ? 0L : dto.getSalesDataAmount();

            StatisticResponseDto existing = statistics.getOrDefault(
                    telecomId,
                    StatisticResponseDto.builder()
                            .totalPrice(0L)
                            .totalDataAmount(0L)
                            .transactionCount(0L)
                            .build()
            );

            StatisticResponseDto updated = StatisticResponseDto.builder()
                    .totalPrice(existing.getTotalPrice() + price)
                    .totalDataAmount(existing.getTotalDataAmount() + dataAmount)
                    .transactionCount(existing.getTransactionCount() + 1)
                    .build();

            statistics.put(telecomId, updated);

            log.debug("[NormalStatisticWriter] 통신사 ID: {}, price: {}, amount: {}", telecomId, price, dataAmount);
        }

        stepExecution.getExecutionContext().put(NORMAL_STATISTIC_KEY, statistics);
        log.info("[NormalStatisticWriter] 누적 집계 완료. 처리건수: {}, 통신사 수: {}",
                chunk.getItems().size(), statistics.size());
    }
}

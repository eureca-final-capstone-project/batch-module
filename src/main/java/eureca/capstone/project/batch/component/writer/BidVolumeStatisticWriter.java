package eureca.capstone.project.batch.component.writer;


import eureca.capstone.project.batch.transaction_feed.entity.DataTransactionHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class BidVolumeStatisticWriter implements ItemWriter<DataTransactionHistory> {
    public static final String VOLUME_STATISTIC_KEY = "VOLUME_STATISTIC";
    private StepExecution stepExecution;
    private Long totalCount;


    @BeforeStep
    public void saveVolumeStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        ExecutionContext executionContext = stepExecution.getExecutionContext();

        if (executionContext.containsKey(VOLUME_STATISTIC_KEY)) {
            this.totalCount = executionContext.getLong(VOLUME_STATISTIC_KEY);
        } else {
            this.totalCount = 0L;
            executionContext.putLong(VOLUME_STATISTIC_KEY, 0L);
        }
        log.info("[VolumeStatisticWriter] 초기 거래량 카운트: {}", totalCount);
    }

    @Override
    public void write(Chunk<? extends DataTransactionHistory> chunk) throws Exception {
        // 청크에 담긴 아이템 수만큼 누적
        int items = chunk.getItems().size();
        totalCount += items;

        // ExecutionContext 에 저장
        stepExecution.getExecutionContext().putLong(VOLUME_STATISTIC_KEY, totalCount);
        log.info("[VolumeStatisticWriter] 누적 거래량 +{}건 → 총 {}건", items, totalCount);
    }
}

package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.alarm.service.NotificationService;
import eureca.capstone.project.batch.transaction_feed.entity.UserDataCoupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CouponExpirationSummaryListener implements JobExecutionListener,
        ItemWriteListener<UserDataCoupon> {

    private final NotificationService notificationService;
    private StepExecution stepExecution;

    private static final String PROCESSED_COUNT_KEY = "processedCount";
    private static final String TARGET_USER_ID_KEY = "targetUserId";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getExecutionContext().putLong(PROCESSED_COUNT_KEY, 0L);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            long totalProcessedCount = jobExecution.getExecutionContext().getLong(PROCESSED_COUNT_KEY);
            Long targetUserId = (Long) jobExecution.getExecutionContext().get(TARGET_USER_ID_KEY);

            if (totalProcessedCount > 0 && targetUserId != null) {
                log.info("[CouponExpirationSummaryListener] Job 완료. 총 {}건의 쿠폰 만료 처리. 요약 알림을 전송합니다.", totalProcessedCount);
                notificationService.sendNotification(
                        targetUserId,
                        "쿠폰 만료 요약",
                        "회원님의 데이터 쿠폰 " + totalProcessedCount + "개가 만료되었습니다."
                );
            }
        }
    }

    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void afterWrite(Chunk<? extends UserDataCoupon> chunk) {
        if (chunk == null || chunk.isEmpty()) return;

        ExecutionContext jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();

        long currentCount = jobExecutionContext.getLong(PROCESSED_COUNT_KEY, 0L);
        jobExecutionContext.putLong(PROCESSED_COUNT_KEY, currentCount + chunk.size());

        if (!jobExecutionContext.containsKey(TARGET_USER_ID_KEY)) {
            Long userId = chunk.getItems().get(0).getUser().getUserId();
            jobExecutionContext.put(TARGET_USER_ID_KEY, userId);
        }
    }

    @Override
    public void onWriteError(Exception ex, Chunk<? extends UserDataCoupon> chunk) {
        log.error("[CouponExpirationSummaryListener] 쓰기 오류 발생. size={}",
                chunk == null ? 0 : chunk.size(), ex);
    }
}
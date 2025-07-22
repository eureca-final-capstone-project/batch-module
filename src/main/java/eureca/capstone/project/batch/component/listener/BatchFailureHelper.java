package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.auth.entity.UserAuthority;
import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import eureca.capstone.project.batch.common.service.BatchFailureLogService;
import eureca.capstone.project.batch.component.external.DiscordNotificationService;
import eureca.capstone.project.batch.config.AuctionJobConfig;
import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.lang.reflect.Field;

// 통합 Skip/Retry Listener
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchFailureHelper implements SkipListener<Object, Object>, RetryListener {

    private final BatchFailureLogService batchFailureLogService;
    private final DiscordNotificationService discordNotificationService;

    public void handleFailure(Throwable t, BatchFailureLog.FailureType failureType,
                               String stepPhase, Object item) {
        JobContext jobContext = getCurrentJobContext();
        String fullStepName = jobContext.getStepName() + ":" + stepPhase;
        String failedItemId = extractItemId(item);

        BatchFailureLog failureLog = BatchFailureLog.builder()
                .jobName(jobContext.getJobName())
                .stepName(fullStepName)
                .failedItemType(item != null ? item.getClass().getSimpleName() : "N/A")
                .failedItemId(failedItemId)
                .errorMessage(t.toString())
                .failureType(failureType)
                .build();

        batchFailureLogService.saveFailureLog(failureLog);
        sendDiscordNotification(failureType, jobContext, failedItemId, t);
    }

    public String extractItemId(Object item) {
        if (item == null) return "N/A";

        try {
            if (item instanceof AuctionJobConfig.AuctionResult auctionResult) {
                TransactionFeed feed = auctionResult.getTransactionFeed();
                return (feed != null) ? String.valueOf(feed.getTransactionFeedId()) : "N/A";
            }
            if (item instanceof UserAuthority userAuthority) {
                return String.valueOf(userAuthority.getUserAuthorityId());
            }
            if (item instanceof TransactionFeed transactionFeed) {
                return String.valueOf(transactionFeed.getTransactionFeedId());
            }
            // 리플렉션으로 ID 필드 찾기
            Field[] fields = item.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    Object value = field.get(item);
                    return value != null ? value.toString() : "N/A";
                }
            }
        } catch (Exception e) {
            log.warn("아이템 ID 추출 실패: {}", e.getMessage());
        }

        return "N/A";
    }

    private JobContext getCurrentJobContext() {
        // JobExecutionContext에서 현재 Job/Step 정보 추출
        try {
            StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
            JobExecution jobExecution = stepExecution.getJobExecution();

            return new JobContext(
                    jobExecution.getJobInstance().getJobName(),
                    stepExecution.getStepName()
            );
        } catch (Exception e) {
            log.warn("Job 컨텍스트 추출 실패, 기본값 사용: {}", e.getMessage());
            return new JobContext("UNKNOWN_JOB", "UNKNOWN_STEP");
        }
    }

    private void sendDiscordNotification(BatchFailureLog.FailureType failureType,
                                         JobContext jobContext, String itemId, Throwable t) {
        String emoji = getEmojiForFailureType(failureType);
        String title = emoji + " BATCH-" + failureType.name();
        String description = String.format(
                "**Job**: `%s`\n**Step**: `%s`\n**Type**: `%s`\n**Item ID**: `%s`\n**Error**: ```%s```",
                jobContext.getJobName(), jobContext.getStepName(),
                failureType.getDescription(), itemId, t.getMessage()
        );

        Color color = getColorForFailureType(failureType);
        discordNotificationService.sendMessage(title, description, color);
    }

    private String getEmojiForFailureType(BatchFailureLog.FailureType failureType) {
        return switch (failureType) {
            case SKIP -> "🟡";
            case RETRY_EXHAUSTED -> "🟠";
            case JOB_FAILED -> "🔴";
        };
    }

    private Color getColorForFailureType(BatchFailureLog.FailureType failureType) {
        return switch (failureType) {
            case SKIP -> Color.YELLOW;
            case RETRY_EXHAUSTED -> Color.ORANGE;
            case JOB_FAILED -> Color.RED;
        };
    }

    @Data
    @AllArgsConstructor
    private static class JobContext {
        private String jobName;
        private String stepName;
    }
}

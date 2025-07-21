package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import eureca.capstone.project.batch.common.service.BatchFailureLogService;
import eureca.capstone.project.batch.component.external.DiscordNotificationService;
import eureca.capstone.project.batch.transaction_feed.domain.TransactionFeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.stereotype.Component;

import java.awt.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSkipListener implements SkipListener<TransactionFeed, TransactionFeed> {

    private final BatchFailureLogService batchFailureLogService;
    private final DiscordNotificationService discordNotificationService;



    @Override
    @OnSkipInRead
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP] Reader에서 오류가 발생하여 건너뜁니다. error={}", t.getMessage());
    }

    @Override
    @OnSkipInProcess
    public void onSkipInProcess(TransactionFeed item, Throwable t) {
        log.warn("[SKIP] Processor에서 오류가 발생하여 건너뜁니다. item ID={}, error={}", item.getTransactionFeedId(), t.getMessage());
        handleSkippedItem(t, "PROCESS", item, "expireGeneralSaleFeedJob", "expireGeneralSaleFeedStep");
    }

    @Override
    @OnSkipInWrite
    public void onSkipInWrite(TransactionFeed item, Throwable t) {
        log.warn("[SKIP] Writer에서 오류가 발생하여 건너뜁니다. item ID={}, error={}", item.getTransactionFeedId(), t.getMessage());
        handleSkippedItem(t, "WRITE", item, "expireGeneralSaleFeedJob", "expireGeneralSaleFeedStep");
    }

    private void handleSkippedItem(Throwable t, String stepPhase, TransactionFeed item, String jobName, String stepName) {
        String fullStepName = stepName + ":" + stepPhase;
        String failedItemId = (item != null) ? String.valueOf(item.getTransactionFeedId()) : "N/A";

        // 1. DB에 실패 로그 저장 (별도 트랜잭션으로 실행)
        BatchFailureLog failureLog = BatchFailureLog.builder()
                .jobName(jobName)
                .stepName(fullStepName)
                .failedItemType(item != null ? item.getClass().getSimpleName() : "N/A")
                .failedItemId(failedItemId)
                .errorMessage(t.toString())
                .build();
        batchFailureLogService.saveFailureLog(failureLog);

        String title = "🟡 BATCH-SKIP";
        String description = String.format(
                "**Job**: `%s`\n**Step**: `%s`\n**Item ID**: `%s`\n**Error**: ```%s```",
                jobName, stepName, failedItemId, t.getMessage()
        );
        discordNotificationService.sendMessage(title, description, Color.YELLOW);
    }
}

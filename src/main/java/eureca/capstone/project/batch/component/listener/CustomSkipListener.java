package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.transaction_feed.domain.TransactionFeed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomSkipListener implements SkipListener<TransactionFeed, TransactionFeed> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP] Reader에서 오류가 발생하여 건너뜁니다. error={}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(TransactionFeed item, Throwable t) {
        log.warn("[SKIP] Processor에서 오류가 발생하여 건너뜁니다. item ID={}, error={}", item.getTransactionFeedId(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(TransactionFeed item, Throwable t) {
        log.warn("[SKIP] Writer에서 오류가 발생하여 건너뜁니다. item ID={}, error={}", item.getTransactionFeedId(), t.getMessage());
    }
}

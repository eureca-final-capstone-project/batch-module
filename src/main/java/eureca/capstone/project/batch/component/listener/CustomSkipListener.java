package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSkipListener implements SkipListener<Object, Object> {

    private final BatchFailureHelper failureHelper;

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP] Reader에서 오류가 발생하여 건너뜁니다. error={}", t.getMessage());
        failureHelper.handleFailure(t, BatchFailureLog.FailureType.SKIP, "READ", null);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("[SKIP] Processor에서 오류가 발생하여 건너뜁니다. item ID={}, error={}", failureHelper.extractItemId(item), t.getMessage());
        failureHelper.handleFailure(t, BatchFailureLog.FailureType.SKIP, "PROCESS", item);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.warn("[SKIP] Writer에서 오류가 발생하여 건너뜁니다. item ID={}, error={}", failureHelper.extractItemId(item), t.getMessage());
        failureHelper.handleFailure(t, BatchFailureLog.FailureType.SKIP, "WRITE", item);
    }
}

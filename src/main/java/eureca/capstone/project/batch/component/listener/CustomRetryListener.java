package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomRetryListener implements RetryListener {

    private final BatchFailureHelper failureHelper;

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return true; // 재시도를 계속하려면 true
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 재시도가 최종적으로 실패했을 때 (throwable이 null이 아님)
        if (throwable != null) {
            log.error("[RETRY_EXHAUSTED] 최대 재시도 횟수 도달 후 최종 실패. error={}", throwable.getMessage());
            failureHelper.handleFailure(throwable, BatchFailureLog.FailureType.RETRY_EXHAUSTED, "RETRY", context.getAttribute("item"));
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 각 재시도마다 로그를 남기고 싶다면 여기에 로직 추가
        log.info("retry count={}", context.getRetryCount());
        log.error("실제로 발생한 예외 클래스: {}", throwable.getClass().getName());
    }
}

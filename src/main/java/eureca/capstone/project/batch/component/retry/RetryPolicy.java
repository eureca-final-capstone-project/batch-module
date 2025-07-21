package eureca.capstone.project.batch.component.retry;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLRecoverableException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class RetryPolicy {
    private int maxAttempts = 3;
    private long backoffMs = 2000;

    public SimpleRetryPolicy createRetryPolicy() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(IOException.class, true);
        retryableExceptions.put(TimeoutException.class, true);
        retryableExceptions.put(SQLRecoverableException.class, true);
        retryableExceptions.put(TransientDataAccessException.class, true);

        return new SimpleRetryPolicy(maxAttempts, retryableExceptions);
    }

    public FixedBackOffPolicy createBackoffPolicy() {
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(backoffMs);
        return policy;
    }
}

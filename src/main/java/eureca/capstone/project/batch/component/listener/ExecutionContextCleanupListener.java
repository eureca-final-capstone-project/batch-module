package eureca.capstone.project.batch.component.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

public class ExecutionContextCleanupListener implements StepExecutionListener {

    private final String keysToRemove;

    public ExecutionContextCleanupListener(String keysToRemove) {
        this.keysToRemove = keysToRemove;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus().isUnsuccessful()) {
            // 실패 시에는 그대로 두기
            return stepExecution.getExitStatus();
        }
        ExecutionContext context = stepExecution.getJobExecution().getExecutionContext();
        context.remove(keysToRemove);
        return stepExecution.getExitStatus();
    }
}
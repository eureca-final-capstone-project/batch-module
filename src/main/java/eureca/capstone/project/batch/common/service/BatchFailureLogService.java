package eureca.capstone.project.batch.common.service;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import eureca.capstone.project.batch.common.repository.BatchFailureLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BatchFailureLogService {

    private final BatchFailureLogRepository batchFailureLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailureLog(BatchFailureLog failureLog) {
        batchFailureLogRepository.save(failureLog);
    }
}

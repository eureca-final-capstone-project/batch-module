package eureca.capstone.project.batch.common.service;

import eureca.capstone.project.batch.auth.entity.UserAuthority;
import eureca.capstone.project.batch.auth.repository.UserAuthorityRepository;
import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import eureca.capstone.project.batch.common.repository.BatchFailureLogRepository;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.transaction_feed.domain.TransactionFeed;
import eureca.capstone.project.batch.transaction_feed.repository.TransactionFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchFailureLogService {

    private final BatchFailureLogRepository batchFailureLogRepository;
    private final UserAuthorityRepository userAuthorityRepository;
    private final TransactionFeedRepository transactionFeedRepository;
    private final StatusRepository statusRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailureLog(BatchFailureLog failureLog) {
        batchFailureLogRepository.save(failureLog);
        log.info("배치 실패 로그 저장 완료: ID={}, Job={}, Step={}, Type={}",
                failureLog.getId(), failureLog.getJobName(),
                failureLog.getStepName(), failureLog.getFailureType());
    }

    public List<BatchFailureLog> getFailures(String jobName, String stepName,
                                             Set<BatchFailureLog.FailureType> failureTypes,
                                             Boolean reprocessed) {
        return batchFailureLogRepository.findFailures(
                jobName, stepName, failureTypes,
                reprocessed != null ? reprocessed : false
        );
    }

    @Transactional
    public void reprocessFailedItem(Long failureLogId) {
        BatchFailureLog failureLog = batchFailureLogRepository.findById(failureLogId)
                .orElseThrow(() -> new RuntimeException("실패 로그를 찾을 수 없습니다: " + failureLogId));

        if (failureLog.isReprocessed()) {
            throw new RuntimeException("이미 재처리된 항목입니다: " + failureLogId);
        }

        try {
            switch (failureLog.getJobName()) {
                case "restrictionReleaseJob":
                    reprocessRestrictionReleaseItem(failureLog);
                    break;
                case "expireGeneralSaleFeedJob":
                    reprocessTransactionFeedItem(failureLog);
                    break;
                default:
                    throw new RuntimeException("지원하지 않는 Job 타입: " + failureLog.getJobName());
            }

            failureLog.markAsReprocessed();
            batchFailureLogRepository.save(failureLog);

            log.info("실패 항목 재처리 완료: ID={}, ItemID={}",
                    failureLogId, failureLog.getFailedItemId());

        } catch (Exception e) {
            log.error("실패 항목 재처리 중 오류 발생: ID={}, Error={}", failureLogId, e.getMessage());
            throw new RuntimeException("재처리 실패: " + e.getMessage(), e);
        }
    }

    private void reprocessRestrictionReleaseItem(BatchFailureLog failureLog) {
        String itemId = failureLog.getFailedItemId();
        if ("N/A".equals(itemId)) {
            throw new RuntimeException("재처리할 항목 ID가 없습니다");
        }

        Long userAuthorityId = Long.valueOf(itemId);
        UserAuthority userAuthority = userAuthorityRepository.findById(userAuthorityId)
                .orElseThrow(() -> new RuntimeException("UserAuthority를 찾을 수 없습니다: " + userAuthorityId));

        // 만료된 권한인지 확인하고 삭제
        if (userAuthority.getExpiredAt().isBefore(LocalDateTime.now())) {
            userAuthorityRepository.delete(userAuthority);
            log.info("만료된 사용자 권한 삭제 완료: UserAuthorityId={}", userAuthorityId);
        } else {
            log.warn("아직 만료되지 않은 권한입니다: UserAuthorityId={}, ExpiredAt={}",
                    userAuthorityId, userAuthority.getExpiredAt());
        }
    }

    private void reprocessTransactionFeedItem(BatchFailureLog failureLog) {
        String itemId = failureLog.getFailedItemId();
        if ("N/A".equals(itemId)) {
            throw new RuntimeException("재처리할 항목 ID가 없습니다");
        }

        Long transactionFeedId = Long.valueOf(itemId);
        TransactionFeed transactionFeed = transactionFeedRepository.findById(transactionFeedId)
                .orElseThrow(() -> new RuntimeException("TransactionFeed를 찾을 수 없습니다: " + transactionFeedId));

        if (transactionFeed.getExpiresAt() != null && transactionFeed.getExpiresAt().isBefore(LocalDateTime.now())) {
            transactionFeed.changeStatus(statusRepository.findByDomainAndCode("FEED", "EXPIRED").orElseThrow(() -> new RuntimeException("기간만료 상태를 찾을 수 없습니다.")));
            log.info("판매글 만료 처리 완료: TransactionFeedId={}", transactionFeedId);
        } else {
            log.warn("아직 만료되지 않은 판매글입니다: TransactionFeedId={}, ExpiredAt={}",
                    transactionFeedId, transactionFeed.getExpiresAt());
        }
    }

    @Transactional
    public void reprocessMultipleItems(List<Long> failureLogIds) {
        List<String> successIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();

        for (Long id : failureLogIds) {
            try {
                reprocessFailedItem(id);
                successIds.add(id.toString());
            } catch (Exception e) {
                failedIds.add(id + ": " + e.getMessage());
                log.error("배치 재처리 실패: ID={}, Error={}", id, e.getMessage());
            }
        }

        log.info("배치 재처리 완료 - 성공: {}, 실패: {}", successIds.size(), failedIds.size());
        if (!failedIds.isEmpty()) {
            throw new RuntimeException("일부 항목 재처리 실패: " + String.join(", ", failedIds));
        }
    }
}

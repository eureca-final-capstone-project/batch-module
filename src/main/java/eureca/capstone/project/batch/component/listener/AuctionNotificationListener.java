package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.alarm.service.NotificationService;
import eureca.capstone.project.batch.job.AuctionJobConfig.AuctionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionNotificationListener implements ItemWriteListener<AuctionResult> {

    private final NotificationService notificationService;

    @Override
    public void beforeWrite(Chunk<? extends AuctionResult> chunk) { }

    @Override
    public void afterWrite(Chunk<? extends AuctionResult> chunk) {
        if (chunk == null || chunk.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chunk.getItems().forEach(result -> {
                    if (result.getType() == AuctionResult.Type.WINNING) {
                        // 낙찰자에게 알림 전송
                        if (result.getBuyer() != null) {
                            notificationService.sendNotification(
                                    result.getBuyer().getUserId(),
                                    "구매",
                                    String.format("[%s] 게시글에 낙찰되었습니다! 낙찰 금액: %d", result.getTransactionFeed().getTitle(), result.getFinalBidAmount())
                            );
                        }
                        // 판매자에게 알림 전송
                        if (result.getTransactionFeed().getUser() != null) {
                            notificationService.sendNotification(
                                    result.getTransactionFeed().getUser().getUserId(),
                                    "판매",
                                    String.format("[%s] 게시글이 낙찰되었습니다! 낙찰 금액: %d", result.getTransactionFeed().getTitle(), result.getFinalBidAmount())
                            );
                        }
                    } else if (result.getType() == AuctionResult.Type.FAILED) {
                        // 유찰된 판매글 등록자에게 알림 전송
                        if (result.getTransactionFeed().getUser() != null) {
                            notificationService.sendNotification(
                                    result.getTransactionFeed().getUser().getUserId(),
                                    "게시글 만료",
                                    String.format("[%s] 게시글이 유찰되었습니다.", result.getTransactionFeed().getTitle())
                            );
                        }
                    }
                });
                log.info("[AuctionNotificationListener] {}건 알림 전송 완료", chunk.getItems().size());
            }
        });
    }

    @Override
    public void onWriteError(Exception ex, Chunk<? extends AuctionResult> chunk) {
        log.error("[AuctionNotificationListener] error 발생. size={}",
                chunk == null ? 0 : chunk.size(), ex);
    }
}

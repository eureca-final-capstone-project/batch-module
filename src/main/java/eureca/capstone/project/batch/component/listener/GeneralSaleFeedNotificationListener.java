package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.alarm.service.NotificationService;
import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
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
public class GeneralSaleFeedNotificationListener implements ItemWriteListener<TransactionFeed> {

    private final NotificationService notificationService;

    @Override
    public void beforeWrite(Chunk<? extends TransactionFeed> chunk) { }

    @Override
    public void afterWrite(Chunk<? extends TransactionFeed> chunk) {
        if (chunk == null || chunk.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chunk.getItems().forEach(feed -> {
                    if (feed.getUser() != null) {
                        notificationService.sendNotification(
                                feed.getUser().getUserId(),
                                "게시글 만료",
                                String.format("[%s] 게시글이 만료되었습니다.", feed.getTitle())
                        );
                    }
                });
                log.info("[GeneralSaleFeedNotificationListener] {}건 알림 전송 완료", chunk.getItems().size());
            }
        });
    }

    @Override
    public void onWriteError(Exception ex, Chunk<? extends TransactionFeed> chunk) {
        log.error("[GeneralSaleFeedNotificationListener] error 발생. size={}",
                chunk == null ? 0 : chunk.size(), ex);
    }
}

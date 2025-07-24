package eureca.capstone.project.batch.common.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionStatisticScheduler {

    private final JobLauncher jobLauncher;
    private final Job normalStatisticJob;
    private final Job bidStatisticJob;

    // 시세통계 + 일반판매 거래량 통계
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void runNormalStatisticBatch() {
        try {
            log.info("[runStatisticBatch] 일반판매 거래량/시세 통계 배치 실행");
            jobLauncher.run(normalStatisticJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("currentTime", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[runStatisticBatch] 일반판매 거래량/시세 통계 배치 실행 완료");
        } catch (Exception e) {
            log.error("[runStatisticBatch] 일반판매 거래량/시세 통계 배치 실행 실패", e);
        }
    }


    // 같은 db에 접근하므로 입찰판매는 00:05분에 스케줄링
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    private void runBidStatisticBatch() {
        try {
            log.info("[runBidStatisticBatch] 입찰판매 거래량 통계 배치 실행");
            jobLauncher.run(bidStatisticJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("currentTime", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[runBidStatisticBatch] 입찰판매 거래량 통계 배치 실행 완료");
        } catch (Exception e) {
            log.error("[runBidStatisticBatch] 입찰판매 거래량 통계 배치 실행 실패: ", e);
        }
    }
}



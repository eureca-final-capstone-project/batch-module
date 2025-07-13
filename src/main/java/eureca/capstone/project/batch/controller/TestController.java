package eureca.capstone.project.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class TestController {

    private final JobLauncher jobLauncher;
    private final Job resetUserDataJob;

    @PostMapping("/batch-test")
    public String runManualBatch() {
        try {
            jobLauncher.run(resetUserDataJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters());
            return "batch 수동 초기화 완료";
        } catch (Exception e) {
            log.error("batch 수동 초기화 실패", e);
            return "batch 수동 초기화 실패: " + e.getMessage();
        }
    }


    @GetMapping("/test")
    public String test() {
        log.info("batch test 메서드 실행");
        return "test";
    }
}

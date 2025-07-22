package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class StatusConfig {
    private final StatusRepository statusRepository;

    @Bean
    public Map<String, Status> getStatuses() {
        Status expired = statusRepository.findByDomainAndCode("COUPON", "EXPIRED")
                .orElseThrow(() -> new RuntimeException("EXPIRED status not found"));
        Status used = statusRepository.findByDomainAndCode("COUPON", "USED")
                .orElseThrow(() -> new RuntimeException("USED status not found"));
        Map<String, Status> map = new HashMap<>();
        map.put("EXPIRED", expired);
        map.put("USED", used);
        return map;
    }
}

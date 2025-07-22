package eureca.capstone.project.batch.common.util;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusManager {

    private final StatusRepository statusRepository;
    private Map<String, Map<String, Status>> statusCache;

    @PostConstruct
    public void init() {
        statusCache = statusRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Status::getDomain,
                        Collectors.toMap(Status::getCode, Function.identity())
                ));
        log.info("StatusManager 로드 완료. 전체 status: {}", statusCache.size());
    }

    /**
     * 도메인과 상태 이름을 통해 Status 객체를 반환하는 메서드
     * @param domain 상태가 속한 도메인 (예: "USER", "PAY")
     * @param code 상태 이름 (예: "PENDING", "ACTIVE")
     * @return Status 객체
     */
    public Status getStatus(String domain, String code) {
        return Optional.ofNullable(statusCache.get(domain))
                .map(domainMap -> domainMap.get(code))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 status 입니다."));
    }
}
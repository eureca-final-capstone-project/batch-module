package eureca.capstone.project.batch.common.util;

import eureca.capstone.project.batch.pay.entity.ChangeType;
import eureca.capstone.project.batch.pay.repository.ChangeTypeRepository;
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
public class ChangeTypeManager {
    private final ChangeTypeRepository changeTypeRepository;
    private Map<String, ChangeType> changeTypeMap;

    @PostConstruct
    public void init() {
        changeTypeMap = changeTypeRepository.findAll().stream()
                .collect(Collectors.toMap(ChangeType::getType, Function.identity()));
        log.info("ChangeTypeManager 로드 완료. 전체 changeType: {}", changeTypeMap.size());
    }

    public ChangeType getChangeType(String type) {
        return Optional.ofNullable(changeTypeMap.get(type))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 changeType 입니다."));
    }
}

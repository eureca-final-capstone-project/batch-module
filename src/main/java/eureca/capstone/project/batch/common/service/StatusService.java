package eureca.capstone.project.batch.common.service;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusService {

    private final StatusRepository statusRepository;

    @Cacheable(value = "statusByDomainCode", key = "#domain + ':' + #code")
    public Status getStatus(String domain, String code){
        log.info("[getStatus] domain: {}, code: {}",domain,code);
        return statusRepository.findByDomainAndCode(domain,code)
                .orElseThrow(() -> new IllegalArgumentException(domain + ":" + code + " Status not found"));
    }
}

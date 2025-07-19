package eureca.capstone.project.batch.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 엔티티 상속 시 필드만 상속 (테이블로는 안 만듦)
@EntityListeners(AuditingEntityListener.class) // 감지 리스너 연결
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; // 생성 시점

    @LastModifiedDate
    private LocalDateTime updatedAt; // 수정 시점
}

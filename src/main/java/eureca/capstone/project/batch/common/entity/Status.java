package eureca.capstone.project.batch.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Objects;

@Builder
@Entity
@Table(name = "status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Status extends BaseEntity {

    @Id
    @Column(name = "status_id")
    private Long statusId;

    @Column(nullable = false, length = 50)
    private String domain;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String description;

    // 👇 RowMapper에서 사용할 수 있도록 ID만 받는 생성자를 추가합니다.
    public Status(Long statusId) {
        this.statusId = statusId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Status status = (Status) o;
        return Objects.equals(statusId, status.statusId) &&
                Objects.equals(code, status.code) &&
                Objects.equals(description, status.description) &&
                Objects.equals(domain, status.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusId, code, description, domain);
    }
}

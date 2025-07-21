package eureca.capstone.project.batch.auth.entity;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_authority")
public class UserAuthority extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_authority_id")
    private Long userAuthorityId;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "authority_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Authority authority;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;


    public void updateExpiresAt(LocalDateTime expiresAt) {
        this.expiredAt = expiresAt;
    }
}


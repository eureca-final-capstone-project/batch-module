package eureca.capstone.project.batch.restriction.entity;

import eureca.capstone.project.batch.auth.entity.Authority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restriction_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestrictionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="restriction_type_id")
    private Long restrictionTypeId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer duration;

    @JoinColumn(name = "authority_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Authority authority;

    @Builder
    public RestrictionType(Long restrictionTypeId, String content, Integer duration, Authority authority) {
        this.restrictionTypeId = restrictionTypeId;
        this.content = content;
        this.duration = duration;
        this.authority = authority;
    }
}

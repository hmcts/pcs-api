package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.Instant;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scheduled_eviction")
public class ScheduledEvictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "court_given_permission_to_dispense_with_service", nullable = false)
    private VerticalYesNo courtGivenPermissionToDispenseWithService;

    @Column(name = "scheduled_eviction_date", nullable = false)
    private Instant scheduledEvictionDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enf_warrant_id", nullable = false)
    private WarrantEntity enfWarrant;
}

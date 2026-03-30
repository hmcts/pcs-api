package uk.gov.hmcts.reform.pcs.ccd.entity.confirmeviction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.UnavailableDateEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "enf_eviction")
public class EvictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "enf_case_id", nullable = false)
    private EnforcementOrderEntity enforcementOrder;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo evictionDateConfirmed;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo hasUnavailableDates;

    @OneToMany(mappedBy = "eviction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UnavailableDateEntity> unavailableDates = new ArrayList<>();

    private String beforeEvictionNameOrDepartment;

    private String beforeEvictionTelephoneNumber;

    private String beforeEvictionEmailAddress;

    private String afterEvictionNameOrDepartment;

    private String afterEvictionTelephoneNumber;

    private String afterEvictionEmailAddress;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo difficultToAccess;

    private String difficultToAccessDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo anythingElse;

    private String anythingElseDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo confirmArrangementForAccess;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant created;

}

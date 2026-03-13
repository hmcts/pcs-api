package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.UUID;

@Entity
@Table(name = "reasonable_adjustments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonableAdjustmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private DefendantResponseEntity defendantResponse;

    private String reasonableAdjustmentsRequired;

    private String reasonableAdjustmentDescription;

    private String hearingEnhancementDescription;

    private String signLanguageSupportDescription;

    private String travelSupportDescription;

    private String welshLanguageRequirements;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo languageInterpreter;

    private String languageSupportDescription;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo consideredVulnerable;

    private String vulnerableCharacteristicDescription;
}

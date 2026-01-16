package uk.gov.hmcts.reform.pcs.ccd.entity.claim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "possession_alternatives")
public class PossessionAlternativesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JsonBackReference
    private ClaimEntity claim;

    // sortb = suspension of right to buy
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo sortbRequested;

    @Enumerated(EnumType.STRING)
    private SuspensionOfRightToBuyHousingAct sortbHousingActSection;

    private String sortbReason;


    // dot = demotion of tenancy
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo dotRequested;

    @Enumerated(EnumType.STRING)
    private DemotionOfTenancyHousingAct dotHousingActSection;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo dotStatementServed;

    private String dotStatementDetails;

    private String dotReason;

}

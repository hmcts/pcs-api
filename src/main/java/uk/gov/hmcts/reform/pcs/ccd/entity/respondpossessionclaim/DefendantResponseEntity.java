package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "defendant_response")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefendantResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private ClaimEntity claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pcs_case_id", nullable = false)
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    @OneToOne(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "sot_id")
    @JsonManagedReference
    private StatementOfTruthEntity statementOfTruth;

    @OneToOne(cascade = ALL, mappedBy = "defendantResponse", orphanRemoval = true)
    @JsonManagedReference
    private HouseholdCircumstancesEntity householdCircumstances;

    @OneToOne(cascade = ALL, mappedBy = "defendantResponse", orphanRemoval = true)
    @JsonManagedReference
    private PaymentAgreementEntity paymentAgreement;

    @OneToOne(cascade = ALL, mappedBy = "defendantResponse", orphanRemoval = true)
    @JsonManagedReference
    private ReasonableAdjustmentEntity reasonableAdjustment;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "free_legal_advice")
    private YesNoPreferNotToSay freeLegalAdvice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tenancy_start_date_confirmation")
    private YesNoNotSure tenancyStartDateConfirmation;

    @Column(name = "tenancy_start_date")
    private LocalDate tenancyStartDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tenancy_type_correct")
    private YesNoNotSure tenancyTypeCorrect;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "defendant_name_confirmation")
    private VerticalYesNo defendantNameConfirmation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo correspondenceAddressConfirmation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure possessionNoticeReceived;

    private LocalDate noticeReceivedDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure rentArrearsAmountConfirmation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo disputeClaim;

    private String disputeClaimDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo otherConsiderations;

    private String otherConsiderationsDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "landlord_registered")
    private YesNoNotSure landlordRegistered;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure writtenTerms;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure landlordLicensed;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo makeCounterClaim;

    @Enumerated(EnumType.STRING)
    private DefendantResponseStatus status;

    private LocalDateTime responseSubmittedDate;

    private LocalDateTime responseDeletedDate;

    private LocalDateTime responseReceivedDate;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    private String channel;

    private String ingestionSource;

    public void setHouseholdCircumstances(HouseholdCircumstancesEntity householdCircumstances) {
        if (this.householdCircumstances != null) {
            this.householdCircumstances.setDefendantResponse(null);
        }

        this.householdCircumstances = householdCircumstances;

        if (this.householdCircumstances != null) {
            this.householdCircumstances.setDefendantResponse(this);
        }
    }

    public void setReasonableAdjustment(ReasonableAdjustmentEntity reasonableAdjustment) {
        if (this.reasonableAdjustment != null) {
            this.reasonableAdjustment.setDefendantResponse(null);
        }

        this.reasonableAdjustment = reasonableAdjustment;

        if (this.reasonableAdjustment != null) {
            this.reasonableAdjustment.setDefendantResponse(this);
        }
    }

    public void setPaymentAgreement(PaymentAgreementEntity paymentAgreement) {
        if (this.paymentAgreement != null) {
            this.paymentAgreement.setDefendantResponse(null);
        }

        this.paymentAgreement = paymentAgreement;

        if (this.paymentAgreement != null) {
            this.paymentAgreement.setDefendantResponse(this);
        }
    }

    public void setStatementOfTruth(StatementOfTruthEntity statementOfTruth) {
        if (this.statementOfTruth != null) {
            this.statementOfTruth.setDefendantResponse(null);
        }

        this.statementOfTruth = statementOfTruth;

        if (this.statementOfTruth != null) {
            this.statementOfTruth.setDefendantResponse(this);
        }
    }

}

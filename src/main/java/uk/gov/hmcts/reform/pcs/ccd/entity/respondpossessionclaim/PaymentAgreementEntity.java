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
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_agreement")
public class PaymentAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private DefendantResponseEntity defendantResponse;

    @Enumerated(EnumType.STRING)
    private YesOrNo anyPaymentsMade;

    private String paymentDetails;

    @Enumerated(EnumType.STRING)
    private YesOrNo paidMoneyToHousingOrg;

    @Enumerated(EnumType.STRING)
    private YesOrNo repaymentPlanAgreed;

    private String repaymentAgreedDetails;

    @Enumerated(EnumType.STRING)
    private YesOrNo repayArrearsInstalments;

    private BigDecimal additionalRentContribution;

    private String additionalContributionFrequency;
}

package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationPersonalisationFactory Tests")
class NotificationPersonalisationFactoryTest {

    private final NotificationPersonalisationFactory factory = new NotificationPersonalisationFactory();

    @Nested
    @DisplayName("forDefendant")
    class ForDefendantTests {
        @Test
        @DisplayName("Should build correct base personalisation for defendant")
        void shouldBuildCorrectBasePersonalisation() {
            DefendantResponseEntity response = createDefendantResponse();

            BasePersonalisation result = factory.forDefendant(response);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("firstName", "John")
                .containsEntry("lastName", "Doe")
                .containsEntry("caseNumber", "1234-5678-90")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE");
        }

        @Test
        @DisplayName("Should use organisation name for claimant if present")
        void shouldBuildBasePersonalisationWithOrgName() {
            DefendantResponseEntity response = createDefendantResponse();
            response.getClaim().getClaimantParty().setOrgName("Claimant Corp");

            BasePersonalisation result = factory.forDefendant(response);

            assertThat(result.toMap()).containsEntry("claimantName", "CLAIMANT CORP");
        }

        @Test
        @DisplayName("Should throw PartyNotFoundException when no claimant found")
        void shouldThrowExceptionWhenNoClaimantFound() {
            DefendantResponseEntity response = createDefendantResponse();
            response.getClaim().setClaimParties(new ArrayList<>());

            assertThatThrownBy(() -> factory.forDefendant(response))
                .isInstanceOf(PartyNotFoundException.class)
                .hasMessageContaining("No claimant party found");
        }
    }

    @Nested
    @DisplayName("forClaimant (ClaimEntity, PartyEntity)")
    class ForClaimantEntityTests {
        @Test
        @DisplayName("Should build correct personalisation for claimant")
        void shouldBuildCorrectPersonalisation() {
            ClaimEntity claim = createClaim();
            PartyEntity claimant = claim.getClaimantParty();

            BasePersonalisation result = factory.forClaimant(claim, claimant);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("firstName", "Jane")
                .containsEntry("lastName", "Smith")
                .containsEntry("caseNumber", "1234-5678-90")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE");
        }
    }

    @Nested
    @DisplayName("forClaimant (caseReference, PCSCase)")
    class ForClaimantPcsCaseTests {
        @Test
        @DisplayName("Should build correct claimant base personalisation")
        void shouldBuildCorrectClaimantBasePersonalisation() {
            PCSCase pcsCase = createPcsCase(VerticalYesNo.YES, "Jane Smith", "Override Name");

            ClaimantBasePersonalisation result = factory.forClaimant(1234567890L, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("toLineClaimantName", "Jane Smith")
                .containsEntry("caseNumber", "1234-5678-90")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE");
        }

        @Test
        @DisplayName("Should use overridden claimant name when name flag is NO")
        void shouldUseOverriddenClaimantNameWhenFlagIsNo() {
            PCSCase pcsCase = createPcsCase(VerticalYesNo.NO, "Jane Smith", "Override Name");

            ClaimantBasePersonalisation result = factory.forClaimant(1234567890L, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("toLineClaimantName", "Override Name")
                .containsEntry("claimantName", "OVERRIDE NAME");
        }

        @Test
        @DisplayName("Should default to claimant name when name flag is null")
        void shouldDefaultToClaimantNameWhenFlagIsNull() {
            PCSCase pcsCase = createPcsCase(null, "Jane Smith", "Override Name");

            ClaimantBasePersonalisation result = factory.forClaimant(1234567890L, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("toLineClaimantName", "Jane Smith")
                .containsEntry("claimantName", "JANE SMITH");
        }
    }

    @Nested
    @DisplayName("counterclaimSuccess")
    class CounterclaimSuccessTests {
        @Test
        @DisplayName("Should include base fields and paymentReferenceNumber")
        void shouldIncludePaymentReferenceNumber() {
            DefendantResponseEntity response = createDefendantResponse();
            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .externalReference("PAY-123")
                .build();
            response.getClaim().setFeePayment(feePayment);

            CounterclaimPaymentSuccessPersonalisation result = factory.counterclaimSuccess(response);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("paymentReferenceNumber", "PAY-123")
                .containsEntry("firstName", "John")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE");
        }

        @Test
        @DisplayName("Should throw FeePaymentNotFoundException when no paid fee payment found")
        void shouldThrowExceptionWhenNoPaidFeePaymentFound() {
            DefendantResponseEntity response = createDefendantResponse();
            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.NOT_PAID)
                .externalReference("PAY-123")
                .build();
            response.getClaim().setFeePayment(feePayment);

            assertThatThrownBy(() -> factory.counterclaimSuccess(response))
                .isInstanceOf(FeePaymentNotFoundException.class)
                .hasMessageContaining("Paid fee payment not found");
        }

        @Test
        @DisplayName("Should throw FeePaymentNotFoundException when fee payment is null")
        void shouldThrowExceptionWhenFeePaymentIsNull() {
            DefendantResponseEntity response = createDefendantResponse();
            response.getClaim().setFeePayment(null);

            assertThatThrownBy(() -> factory.counterclaimSuccess(response))
                .isInstanceOf(FeePaymentNotFoundException.class)
                .hasMessageContaining("Paid fee payment not found");
        }
    }

    private PartyEntity createParty(String firstName, String lastName) {
        PartyEntity party = new PartyEntity();
        party.setFirstName(firstName);
        party.setLastName(lastName);
        return party;
    }

    private PcsCaseEntity createCaseEntity() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(1234567890L);
        return pcsCase;
    }

    private ClaimEntity createClaim() {
        ClaimEntity claim = new ClaimEntity();
        claim.setPcsCase(createCaseEntity());

        PartyEntity claimantParty = createParty("Jane", "Smith");
        claim.addParty(claimantParty, PartyRole.CLAIMANT);

        PartyEntity defendantParty = createParty("John", "Doe");
        claim.addParty(defendantParty, PartyRole.DEFENDANT);

        return claim;
    }

    private DefendantResponseEntity createDefendantResponse() {
        DefendantResponseEntity response = new DefendantResponseEntity();
        response.setId(UUID.randomUUID());
        response.setPcsCase(createCaseEntity());
        response.setParty(createParty("John", "Doe"));
        response.setClaim(createClaim());
        return response;
    }

    private PCSCase createPcsCase(VerticalYesNo nameFlag, String claimantName, String overriddenName) {
        ClaimantInformation claimantInformation = new ClaimantInformation();
        claimantInformation.setIsClaimantNameCorrect(nameFlag);
        claimantInformation.setClaimantName(claimantName);
        claimantInformation.setOverriddenClaimantName(overriddenName);

        DefendantDetails defendant = new DefendantDetails();
        defendant.setFirstName("John");
        defendant.setLastName("Doe");

        return PCSCase.builder()
            .claimantInformation(claimantInformation)
            .defendant1(defendant)
            .build();
    }
}

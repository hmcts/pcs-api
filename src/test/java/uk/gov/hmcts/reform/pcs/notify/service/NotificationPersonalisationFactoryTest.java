package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.ClaimantBasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.CounterclaimPaymentSuccessPersonalisation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@DisplayName("NotificationPersonalisationFactory Tests")
@ExtendWith(MockitoExtension.class)
class NotificationPersonalisationFactoryTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock(strictness = LENIENT)
    private PartyService partyService;
    @Mock(strictness = LENIENT)
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private FeePaymentRepository feePaymentRepository;

    private NotificationPersonalisationFactory factory;

    @BeforeEach
    void setUp() {
        when(pcsCaseEntity.getCaseReference()).thenReturn(CASE_REFERENCE);

        factory = new NotificationPersonalisationFactory(partyService, feePaymentRepository);
    }

    @Nested
    @DisplayName("forDefendant")
    class ForDefendantTests {
        @Test
        @DisplayName("Should build correct base personalisation for defendant")
        void shouldBuildCorrectBasePersonalisation() {
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty();
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);

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
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty();
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);

            claimantParty.setFirstName(null);
            claimantParty.setLastName(null);
            claimantParty.setOrgName("Claimant Corp");

            BasePersonalisation result = factory.forDefendant(response);

            assertThat(result.toMap()).containsEntry("claimantName", "CLAIMANT CORP");
        }

        @Test
        @DisplayName("Should use organisation name as firstName when firstName is null for defendant")
        void shouldBuildBasePersonalisationWithDefendantOrgName() {
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty();
            defendantParty.setFirstName(null);
            defendantParty.setLastName(null);
            defendantParty.setOrgName("Defendant Corp");
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);

            BasePersonalisation result = factory.forDefendant(response);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("firstName", "Defendant Corp")
                .containsEntry("lastName", "");
        }

        @Test
        @DisplayName("Should use PERSONS UNKNOWN when defendant name is not known")
        void shouldUsePersonsUnknownWhenDefendantNameNotKnown() {
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty(VerticalYesNo.NO);
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);

            BasePersonalisation result = factory.forDefendant(response);

            assertThat(result.toMap()).containsEntry("primaryDefendantName", "PERSONS UNKNOWN");
        }
    }

    @Nested
    @DisplayName("forClaimant (ClaimEntity)")
    class ForClaimantEntityTests {
        @Test
        @DisplayName("Should build correct personalisation for claimant")
        void shouldBuildCorrectPersonalisation() {
            PartyEntity claimantParty = stubClaimantParty();
            ClaimEntity claim = createClaim(claimantParty);

            BasePersonalisation result = factory.forClaimant(claim);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("firstName", "Jane")
                .containsEntry("lastName", "Smith")
                .containsEntry("caseNumber", "1234-5678-90")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE");
        }

        @Test
        @DisplayName("Should use organisation name as firstName when firstName is null for claimant")
        void shouldBuildBasePersonalisationWithClaimantOrgName() {
            PartyEntity claimantParty = stubClaimantParty();
            claimantParty.setFirstName(null);
            claimantParty.setLastName(null);
            claimantParty.setOrgName("Claimant Corp");
            ClaimEntity claim = createClaim(claimantParty);

            BasePersonalisation result = factory.forClaimant(claim);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("firstName", "Claimant Corp")
                .containsEntry("lastName", "");
        }

        @Test
        @DisplayName("Should use PERSONS UNKNOWN when defendant name is not known")
        void shouldUsePersonsUnknownWhenDefendantNameNotKnown() {
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty(VerticalYesNo.NO);
            ClaimEntity claim = createClaim(claimantParty, defendantParty);

            BasePersonalisation result = factory.forClaimant(claim);

            assertThat(result.toMap()).containsEntry("primaryDefendantName", "PERSONS UNKNOWN");
        }
    }

    @Nested
    @DisplayName("forClaimant (caseReference, PCSCase)")
    class ForClaimantPcsCaseTests {
        @Test
        @DisplayName("Should build correct claimant base personalisation")
        void shouldBuildCorrectClaimantBasePersonalisation() {
            PCSCase pcsCase = createPcsCase(VerticalYesNo.YES, "Jane Smith", "Override Name");

            ClaimantBasePersonalisation result = factory.forClaimant(CASE_REFERENCE, pcsCase);

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

            ClaimantBasePersonalisation result = factory.forClaimant(CASE_REFERENCE, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("toLineClaimantName", "Override Name")
                .containsEntry("claimantName", "OVERRIDE NAME");
        }

        @Test
        @DisplayName("Should default to claimant name when name flag is null")
        void shouldDefaultToClaimantNameWhenFlagIsNull() {
            PCSCase pcsCase = createPcsCase(null, "Jane Smith", "Override Name");

            ClaimantBasePersonalisation result = factory.forClaimant(CASE_REFERENCE, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("toLineClaimantName", "Jane Smith")
                .containsEntry("claimantName", "JANE SMITH");
        }

        @Test
        @DisplayName("Should use PERSONS UNKNOWN when defendant name is not known")
        void shouldUsePersonsUnknownWhenDefendantNameNotKnown() {
            PCSCase pcsCase = createPcsCase(VerticalYesNo.YES, "Jane Smith", "Override Name");
            pcsCase.getDefendant1().setNameKnown(VerticalYesNo.NO);

            ClaimantBasePersonalisation result = factory.forClaimant(CASE_REFERENCE, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("primaryDefendantName", "PERSONS UNKNOWN");
        }

        @Test
        @DisplayName("Should use PERSONS UNKNOWN when defendant name known is null")
        void shouldUsePersonsUnknownWhenDefendantNameKnownIsNull() {
            PCSCase pcsCase = createPcsCase(VerticalYesNo.YES, "Jane Smith", "Override Name");
            pcsCase.getDefendant1().setNameKnown(null);

            ClaimantBasePersonalisation result = factory.forClaimant(CASE_REFERENCE, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("primaryDefendantName", "PERSONS UNKNOWN");
        }

        @Test
        @DisplayName("Should use PERSONS UNKNOWN when defendant first name is null")
        void shouldUsePersonsUnknownWhenDefendantFirstNameIsNull() {
            PCSCase pcsCase = createPcsCase(VerticalYesNo.YES, "Jane Smith", "Override Name");
            pcsCase.getDefendant1().setFirstName(null);

            ClaimantBasePersonalisation result = factory.forClaimant(CASE_REFERENCE, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("primaryDefendantName", "PERSONS UNKNOWN");
        }

        @Test
        @DisplayName("Should use PERSONS UNKNOWN when defendant last name is null")
        void shouldUsePersonsUnknownWhenDefendantLastNameIsNull() {
            PCSCase pcsCase = createPcsCase(VerticalYesNo.YES, "Jane Smith", "Override Name");
            pcsCase.getDefendant1().setLastName(null);

            ClaimantBasePersonalisation result = factory.forClaimant(CASE_REFERENCE, pcsCase);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("primaryDefendantName", "PERSONS UNKNOWN");
        }
    }

    @Nested
    @DisplayName("forParty")
    class ForPartyEntityTests {
        @Test
        @DisplayName("Should build correct personalisation for party")
        void shouldBuildCorrectPersonalisation() {
            PartyEntity partyEntity = createParty("Another", "Party");
            stubClaimantParty();
            stubDefendantParty();

            BasePersonalisation result = factory.forParty(partyEntity, pcsCaseEntity);

            Map<String, Object> map = result.toMap();
            assertThat(map)
                .containsEntry("firstName", "Another")
                .containsEntry("lastName", "Party")
                .containsEntry("caseNumber", "1234-5678-90")
                .containsEntry("claimantName", "JANE SMITH")
                .containsEntry("primaryDefendantName", "JOHN DOE");
        }
    }

    @Nested
    @DisplayName("counterclaimSuccess")
    class CounterclaimSuccessTests {
        @Test
        @DisplayName("Should include base fields and paymentReferenceNumber")
        void shouldIncludePaymentReferenceNumber() {
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty();
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);
            CounterClaimEntity counterClaim = stubCounterClaim(defendantParty);

            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .externalReference("PAY-123")
                .party(defendantParty)
                .build();
            when(feePaymentRepository.findByRelatedEntityId(counterClaim.getId())).thenReturn(Optional.of(feePayment));

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
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty();
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);
            CounterClaimEntity counterClaim = stubCounterClaim(defendantParty);

            FeePaymentEntity feePayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.NOT_PAID)
                .externalReference("PAY-123")
                .party(defendantParty)
                .build();
            when(feePaymentRepository.findByRelatedEntityId(counterClaim.getId())).thenReturn(Optional.of(feePayment));

            assertThatThrownBy(() -> factory.counterclaimSuccess(response))
                .isInstanceOf(FeePaymentNotFoundException.class)
                .hasMessageContaining("Paid fee payment not found");
        }

        @Test
        @DisplayName("Should throw FeePaymentNotFoundException when fee payment is null")
        void shouldThrowExceptionWhenFeePaymentIsNull() {
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty();
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);
            CounterClaimEntity counterClaim = stubCounterClaim(defendantParty);
            when(feePaymentRepository.findByRelatedEntityId(counterClaim.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> factory.counterclaimSuccess(response))
                .isInstanceOf(FeePaymentNotFoundException.class)
                .hasMessageContaining("Paid fee payment not found");
        }

        @Test
        @DisplayName("Should use the latest paid fee payment when multiple counterclaims exist for a party")
        void shouldUseLatestPaidFeePaymentWhenMultipleCounterClaimsExist() {
            PartyEntity claimantParty = stubClaimantParty();
            PartyEntity defendantParty = stubDefendantParty();
            DefendantResponseEntity response = createDefendantResponse(claimantParty, defendantParty);

            CounterClaimEntity firstCounterClaim = stubCounterClaim(defendantParty, UUID.randomUUID());
            CounterClaimEntity secondCounterClaim = stubCounterClaim(defendantParty, UUID.randomUUID());
            when(pcsCaseEntity.getCounterClaims()).thenReturn(List.of(firstCounterClaim, secondCounterClaim));

            FeePaymentEntity firstPaidPayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .externalReference("PAY-OLD")
                .requestDate(LocalDateTime.now().minusDays(1))
                .build();
            FeePaymentEntity secondPaidPayment = FeePaymentEntity.builder()
                .paymentStatus(PaymentStatus.PAID)
                .externalReference("PAY-NEW")
                .requestDate(LocalDateTime.now())
                .build();
            when(feePaymentRepository.findByRelatedEntityId(firstCounterClaim.getId()))
                .thenReturn(Optional.of(firstPaidPayment));
            when(feePaymentRepository.findByRelatedEntityId(secondCounterClaim.getId()))
                .thenReturn(Optional.of(secondPaidPayment));

            CounterclaimPaymentSuccessPersonalisation result = factory.counterclaimSuccess(response);

            assertThat(result.toMap()).containsEntry("paymentReferenceNumber", "PAY-NEW");
        }
    }

    @Nested
    @DisplayName("formatCaseReference")
    class FormatCaseReferenceTests {

        @Test
        @DisplayName("Should return null when case reference is null")
        void shouldReturnNullWhenCaseReferenceIsNull() {
            assertThat(NotificationPersonalisationFactory.formatCaseReference(null)).isNull();
        }

        @Test
        @DisplayName("Should format case reference correctly")
        void shouldFormatCaseReferenceCorrectly() {
            assertThat(NotificationPersonalisationFactory.formatCaseReference("1234567812345678"))
                .isEqualTo("1234-5678-1234-5678");
        }
    }

    private PartyEntity stubClaimantParty() {
        PartyEntity claimantPartyEntity = createParty("Jane", "Smith");
        when(partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity)).thenReturn(claimantPartyEntity);
        return claimantPartyEntity;
    }

    private PartyEntity stubDefendantParty() {
        return stubDefendantParty(VerticalYesNo.YES);
    }

    private PartyEntity stubDefendantParty(VerticalYesNo nameKnown) {
        PartyEntity defendantParty = createParty("John", "Doe");
        defendantParty.setNameKnown(nameKnown);
        when(partyService.getPrimaryDefendantPartyEntity(pcsCaseEntity)).thenReturn(defendantParty);
        return defendantParty;
    }


    private CounterClaimEntity stubCounterClaim(PartyEntity defendantParty) {
        return stubCounterClaim(defendantParty, UUID.randomUUID());
    }

    private CounterClaimEntity stubCounterClaim(PartyEntity defendantParty, UUID counterClaimId) {
        CounterClaimEntity counterClaim = new CounterClaimEntity();
        counterClaim.setId(counterClaimId);
        counterClaim.setParty(defendantParty);
        when(pcsCaseEntity.getCounterClaims()).thenReturn(List.of(counterClaim));
        return counterClaim;
    }

    private PartyEntity createParty(String firstName, String lastName) {
        PartyEntity party = new PartyEntity();
        party.setId(UUID.randomUUID());
        party.setFirstName(firstName);
        party.setLastName(lastName);
        party.setNameKnown(VerticalYesNo.YES);
        return party;
    }

    private ClaimEntity createClaim(PartyEntity claimantParty) {
        return createClaim(claimantParty, stubDefendantParty());
    }

    private ClaimEntity createClaim(PartyEntity claimantParty, PartyEntity defendantParty) {
        ClaimEntity claim = new ClaimEntity();
        claim.setPcsCase(pcsCaseEntity);

        claim.addParty(claimantParty, PartyRole.CLAIMANT);
        claim.addParty(defendantParty, PartyRole.DEFENDANT);

        return claim;
    }

    private DefendantResponseEntity createDefendantResponse(PartyEntity claimantParty, PartyEntity defendantParty) {
        DefendantResponseEntity response = new DefendantResponseEntity();
        response.setId(UUID.randomUUID());
        response.setPcsCase(pcsCaseEntity);
        response.setParty(defendantParty);
        response.setClaim(createClaim(claimantParty, defendantParty));
        return response;
    }

    private PCSCase createPcsCase(VerticalYesNo nameFlag, String claimantName, String overriddenName) {
        ClaimantInformation claimantInformation = new ClaimantInformation();
        claimantInformation.setIsClaimantNameCorrect(nameFlag);
        claimantInformation.setClaimantName(claimantName);
        claimantInformation.setOverriddenClaimantName(overriddenName);

        DefendantDetails defendant = new DefendantDetails();
        defendant.setNameKnown(VerticalYesNo.YES);
        defendant.setFirstName("John");
        defendant.setLastName("Doe");

        return PCSCase.builder()
            .claimantInformation(claimantInformation)
            .defendant1(defendant)
            .build();
    }
}

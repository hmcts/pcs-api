package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CounterClaimFormPayloadBuilderTest {

    private static final long CASE_REFERENCE = 1234567812345678L;

    private CounterClaimFormPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        Clock ukClock = Clock.fixed(
            LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.of("Europe/London"));
        builder = new CounterClaimFormPayloadBuilder(new CaseReferenceFormatter(), new CaseNameFormatter(), ukClock);
    }

    @Test
    void buildsFullPayloadFromCounterClaim() {
        CounterClaimEntity counterClaim = fullyPopulatedCounterClaim();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-1234-5678");
        assertThat(payload.getCaseName()).isEqualTo("Alice Claimant vs Bob Defendant");
        assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, Month.JUNE, 15));
        assertThat(payload.getSubmittedOn()).isEqualTo(LocalDate.of(2026, Month.JUNE, 14));
        assertThat(payload.getClaimingFor()).isEqualTo("A sum of money or compensation");
        assertThat(payload.getClaimingSpecificSum()).isEqualTo("Yes");
        assertThat(payload.getClaimAmount()).isEqualTo("£1,200.00");
        assertThat(payload.getMaximumClaimValue()).isEqualTo("£5,000.00");
        assertThat(payload.getNeedsHelpWithFees()).isEqualTo("No");
        assertThat(payload.getHwfReferenceNumber()).isEqualTo("HWF-123");
        assertThat(payload.getRespondentNames()).isEqualTo("Alice Claimant\nAcme Ltd");
        assertThat(payload.getCounterClaimFor()).isEqualTo("Unpaid deposit refund");
        assertThat(payload.getCounterClaimReasons()).isEqualTo("Landlord kept deposit unlawfully");
        assertThat(payload.getOtherOrderRequestDetails()).isEqualTo("Restore quiet enjoyment");
        assertThat(payload.getOtherOrderRequestFacts()).isEqualTo("Landlord entered without notice");
        assertThat(payload.getStatementOfTruthName()).isEqualTo("Robert J Defendant");
        assertThat(payload.getShowClaimingFor()).isTrue();
        assertThat(payload.getShowClaimingSpecificSum()).isTrue();
        assertThat(payload.getShowClaimAmount()).isTrue();
        assertThat(payload.getShowMaximumClaimValue()).isTrue();
        assertThat(payload.getShowNeedsHelpWithFees()).isTrue();
        assertThat(payload.getShowHwfRef()).isTrue();
        assertThat(payload.getShowRespondentNames()).isTrue();
        assertThat(payload.getShowCounterClaimFor()).isTrue();
        assertThat(payload.getShowCounterClaimReasons()).isTrue();
        assertThat(payload.getShowOtherOrderSection()).isTrue();
        assertThat(payload.getShowStatementOfTruthName()).isTrue();
    }

    @Test
    void showFlagsAreFalseWhenSourceFieldsAreBlank() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(minimalCase(null))
            .party(PartyEntity.builder().id(UUID.randomUUID()).build())
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getShowClaimingFor()).isFalse();
        assertThat(payload.getShowClaimingSpecificSum()).isFalse();
        assertThat(payload.getShowClaimAmount()).isFalse();
        assertThat(payload.getShowMaximumClaimValue()).isFalse();
        assertThat(payload.getShowNeedsHelpWithFees()).isFalse();
        assertThat(payload.getShowHwfRef()).isFalse();
        assertThat(payload.getShowRespondentNames()).isFalse();
        assertThat(payload.getShowCounterClaimFor()).isFalse();
        assertThat(payload.getShowCounterClaimReasons()).isFalse();
        assertThat(payload.getShowOtherOrderSection()).isFalse();
        assertThat(payload.getShowStatementOfTruthName()).isFalse();
    }

    @Test
    void showOtherOrderSectionIsTrueWhenOnlyOneOtherOrderFieldPopulated() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(minimalCase(null))
            .party(PartyEntity.builder().id(UUID.randomUUID()).build())
            .otherOrderRequestDetails("Do the repairs")
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getShowOtherOrderSection()).isTrue();
    }

    @Test
    void statementOfTruthNameIsNullWhenStatementOfTruthMissing() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(minimalCase(null))
            .party(PartyEntity.builder().id(UUID.randomUUID()).firstName("Bob").lastName("Defendant").build())
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getStatementOfTruthName()).isNull();
    }

    @Test
    void statementOfTruthNamePicksResponseForThisDefendantWhenCaseHasMultipleDefendants() {
        PartyEntity thisDefendant = PartyEntity.builder().id(UUID.randomUUID())
            .firstName("Bob").lastName("Defendant").build();
        PartyEntity otherDefendant = PartyEntity.builder().id(UUID.randomUUID())
            .firstName("Charlie").lastName("Other").build();

        PcsCaseEntity pcsCase = minimalCase(null);
        pcsCase.getDefendantResponses().add(DefendantResponseEntity.builder()
            .party(otherDefendant)
            .statementOfTruth(StatementOfTruthEntity.builder().fullName("Charlie Other").build())
            .build());
        pcsCase.getDefendantResponses().add(DefendantResponseEntity.builder()
            .party(thisDefendant)
            .statementOfTruth(StatementOfTruthEntity.builder().fullName("Robert J Defendant").build())
            .build());

        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .party(thisDefendant)
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getStatementOfTruthName()).isEqualTo("Robert J Defendant");
    }

    @Test
    void statementOfTruthNameIsNullWhenAssociatedResponseHasNoStatementOfTruth() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID())
            .firstName("Bob").lastName("Defendant").build();

        PcsCaseEntity pcsCase = minimalCase(null);
        pcsCase.getDefendantResponses().add(DefendantResponseEntity.builder()
            .party(defendant)
            .build());

        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .party(defendant)
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getStatementOfTruthName()).isNull();
    }

    @Test
    void leavesNullFieldsUnsetWhenSourceEntityHasNulls() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(minimalCase(null))
            .party(PartyEntity.builder().id(UUID.randomUUID()).build())
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-1234-5678");
        assertThat(payload.getIssueDateSealed()).isNull();
        assertThat(payload.getSubmittedOn()).isNull();
        assertThat(payload.getClaimingFor()).isNull();
        assertThat(payload.getClaimingSpecificSum()).isNull();
        assertThat(payload.getClaimAmount()).isNull();
        assertThat(payload.getMaximumClaimValue()).isNull();
        assertThat(payload.getNeedsHelpWithFees()).isNull();
        assertThat(payload.getHwfReferenceNumber()).isNull();
        assertThat(payload.getRespondentNames()).isNull();
        assertThat(payload.getCounterClaimFor()).isNull();
        assertThat(payload.getCounterClaimReasons()).isNull();
    }

    @Test
    void utcTimestampLateInDayConvertsToNextUkDateDuringBst() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .pcsCase(minimalCase(null))
            .claimIssuedDate(LocalDateTime.of(2026, Month.JUNE, 15, 23, 30))
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, Month.JUNE, 16));
    }

    @Test
    void respondentNamesReturnsNullWhenEveryPartyYieldsBlankName() {
        PartyEntity blankParty = PartyEntity.builder().id(UUID.randomUUID()).firstName(" ").lastName("").build();
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(minimalCase(null))
            .party(PartyEntity.builder().id(UUID.randomUUID()).build())
            .counterClaimParties(new ArrayList<>(List.of(
                CounterClaimPartyEntity.builder().party(blankParty).build())))
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getRespondentNames()).isNull();
    }

    @Test
    void respondentNameUsesOrgWhenPresentOtherwiseFullName() {
        PartyEntity org = PartyEntity.builder().id(UUID.randomUUID()).orgName("Acme Ltd").firstName("ignored").build();
        PartyEntity person = PartyEntity.builder().id(UUID.randomUUID()).firstName("Bob").lastName("Smith").build();
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(minimalCase(null))
            .party(PartyEntity.builder().id(UUID.randomUUID()).build())
            .counterClaimParties(new ArrayList<>(List.of(
                CounterClaimPartyEntity.builder().party(org).build(),
                CounterClaimPartyEntity.builder().party(person).build())))
            .build();

        CounterClaimFormPayload payload = builder.build(counterClaim);

        assertThat(payload.getRespondentNames()).isEqualTo("Acme Ltd\nBob Smith");
    }

    private CounterClaimEntity fullyPopulatedCounterClaim() {
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID())
            .firstName("Alice").lastName("Claimant").nameKnown(VerticalYesNo.YES).build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID())
            .firstName("Bob").lastName("Defendant").nameKnown(VerticalYesNo.YES).build();
        PartyEntity respondentOrg = PartyEntity.builder().id(UUID.randomUUID()).orgName("Acme Ltd").build();

        PcsCaseEntity pcsCase = minimalCase(claimAgainst(claimant, defendant));
        pcsCase.getDefendantResponses().add(DefendantResponseEntity.builder()
            .party(defendant)
            .statementOfTruth(StatementOfTruthEntity.builder().fullName("Robert J Defendant").build())
            .build());

        return CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .party(defendant)
            .claimIssuedDate(LocalDateTime.of(2026, Month.JUNE, 15, 10, 0))
            .claimSubmittedDate(LocalDateTime.of(2026, Month.JUNE, 14, 9, 30))
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("1200.00"))
            .estimatedMaxClaimAmount(new BigDecimal("5000.00"))
            .needHelpWithFees(VerticalYesNo.NO)
            .hwfReferenceNumber("HWF-123")
            .counterClaimFor("Unpaid deposit refund")
            .counterClaimReasons("Landlord kept deposit unlawfully")
            .otherOrderRequestDetails("Restore quiet enjoyment")
            .otherOrderRequestFacts("Landlord entered without notice")
            .counterClaimParties(new ArrayList<>(List.of(
                CounterClaimPartyEntity.builder().party(claimant).build(),
                CounterClaimPartyEntity.builder().party(respondentOrg).build())))
            .build();
    }

    private PcsCaseEntity minimalCase(ClaimEntity claim) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        pcsCase.setClaims(new ArrayList<>(claim == null ? List.of() : List.of(claim)));
        return pcsCase;
    }

    private ClaimEntity claimAgainst(PartyEntity claimant, PartyEntity defendant) {
        ClaimPartyEntity claimantParty = ClaimPartyEntity.builder()
            .party(claimant).role(PartyRole.CLAIMANT).rank(1).build();
        ClaimPartyEntity defendantParty = ClaimPartyEntity.builder()
            .party(defendant).role(PartyRole.DEFENDANT).rank(1).build();
        return ClaimEntity.builder()
            .claimParties(new ArrayList<>(List.of(claimantParty, defendantParty)))
            .build();
    }
}

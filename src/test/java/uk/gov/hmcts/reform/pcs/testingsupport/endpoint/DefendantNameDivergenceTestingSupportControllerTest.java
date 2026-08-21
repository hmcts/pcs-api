package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.PackRecipientResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PartyAttributeAssertationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantNameDivergenceTestingSupportControllerTest {

    private static final long CASE_REFERENCE = 1234123412341234L;
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final String S2S_TOKEN = "Bearer s2s-token";
    private static final String ASSERTED_NAME_JSON = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private PartyAttributeAssertationService partyAttributeAssertationService;

    @Mock
    private PackRecipientResolver packRecipientResolver;

    // The production resolver, unmodified - it is the thing under observation.
    private final RecipientAddressResolver recipientAddressResolver = new RecipientAddressResolver();

    private DefendantNameDivergenceTestingSupportController controller;

    @BeforeEach
    void setUp() {
        controller = new DefendantNameDivergenceTestingSupportController(
            pcsCaseRepository,
            recipientAddressResolver,
            partyAttributeAssertationService,
            packRecipientResolver,
            new ObjectMapper()
        );
    }

    @Test
    void returnsNotFoundForUnknownCase() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        ResponseEntity<DefendantNameDivergenceTestingSupportController.CaseNameReport> response =
            controller.getDefendantNameDivergence(S2S_TOKEN, CASE_REFERENCE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reportsDivergenceWhenTheCoversheetStillSaysPersonsUnknown() {
        // The HDPI-7686 shape: the claimant could not name the defendant, the defendant supplied a name
        // through the response journey, but the party record was never updated.
        PartyEntity defendant = defendant(VerticalYesNo.NO, null, null);
        givenCaseWithDefendant(defendant);
        givenNameAssertion();

        DefendantNameDivergenceTestingSupportController.CaseNameReport report = report();

        assertThat(report.anyDivergence()).isTrue();
        assertThat(report.defendants()).singleElement().satisfies(names -> {
            assertThat(names.coversheetName()).isEqualTo("Persons unknown");
            assertThat(names.formName()).isEqualTo("John Doe");
            assertThat(names.diverges()).isTrue();
        });
    }

    @Test
    void reportsNoDivergenceOnceTheNameIsWrittenBackToTheParty() {
        PartyEntity defendant = defendant(VerticalYesNo.YES, "John", "Doe");
        givenCaseWithDefendant(defendant);
        givenNameAssertion();

        DefendantNameDivergenceTestingSupportController.CaseNameReport report = report();

        assertThat(report.anyDivergence()).isFalse();
        assertThat(report.defendants()).singleElement().satisfies(names -> {
            assertThat(names.coversheetName()).isEqualTo("John Doe");
            assertThat(names.formName()).isEqualTo("John Doe");
            assertThat(names.diverges()).isFalse();
        });
    }

    @Test
    void ignoresPartiesThatAreNotDefendants() {
        PcsCaseEntity pcsCase = caseWith(claimParty(defendant(VerticalYesNo.YES, "Acme", "Lettings"),
            PartyRole.CLAIMANT));
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        when(packRecipientResolver.resolveDefenceRecipients(CASE_ID)).thenReturn(List.of());

        DefendantNameDivergenceTestingSupportController.CaseNameReport report = report();

        assertThat(report.defendants()).isEmpty();
        assertThat(report.anyDivergence()).isFalse();
    }

    private DefendantNameDivergenceTestingSupportController.CaseNameReport report() {
        ResponseEntity<DefendantNameDivergenceTestingSupportController.CaseNameReport> response =
            controller.getDefendantNameDivergence(S2S_TOKEN, CASE_REFERENCE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private void givenCaseWithDefendant(PartyEntity defendant) {
        PcsCaseEntity pcsCase = caseWith(claimParty(defendant, PartyRole.DEFENDANT));
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        when(packRecipientResolver.resolveDefenceRecipients(CASE_ID)).thenReturn(List.of());
    }

    private void givenNameAssertion() {
        when(partyAttributeAssertationService.getSubmittedAssertionsForParty(PARTY_ID)).thenReturn(List.of(
            PartyAttributeAssertationEntity.builder()
                .attributesName(PartyAttributeType.DEFENDANT_NAME)
                .assertedValue(ASSERTED_NAME_JSON)
                .build()
        ));
    }

    private static PartyEntity defendant(VerticalYesNo nameKnown, String firstName, String lastName) {
        return PartyEntity.builder()
            .id(PARTY_ID)
            .nameKnown(nameKnown)
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }

    private static ClaimPartyEntity claimParty(PartyEntity party, PartyRole role) {
        return ClaimPartyEntity.builder()
            .party(party)
            .role(role)
            .rank(1)
            .build();
    }

    private static PcsCaseEntity caseWith(ClaimPartyEntity claimParty) {
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(List.of(claimParty))
            .build();

        return PcsCaseEntity.builder()
            .id(CASE_ID)
            .caseReference(CASE_REFERENCE)
            .claims(List.of(claim))
            .build();
    }
}

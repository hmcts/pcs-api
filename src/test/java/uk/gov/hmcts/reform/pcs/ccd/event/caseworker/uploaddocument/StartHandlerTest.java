package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.uploaddocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentListService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType.OCCUPATION_LICENCE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType.PART_20_COUNTERCLAIM;
import static uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType.TENANCY_AGREEMENT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType.WITNESS_STATEMENT;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class StartHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaim;

    private StartHandler underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getMainClaim()).thenReturn(mainClaim);

        underTest = new StartHandler(pcsCaseService, new CaseworkerDocumentListService(partyService));
    }

    @ParameterizedTest
    @MethodSource("relatedSubmissionsFlagScenarios")
    void shouldSetRelatedSubmissionsFlag(List<GeneralApplication> genApps,
                                         List<CounterClaimEntity> counterClaimEntities,
                                         VerticalYesNo expectedRelatedSubmissionsFlag) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .genApps(wrapListItems(genApps))
            .legislativeCountry(ENGLAND)
            .build();

        when(pcsCaseEntity.getCounterClaims()).thenReturn(counterClaimEntities);

        // When
        PCSCase result = underTest.start(toEventPayload(caseData));

        // Then
        assertThat(result.getCaseworkerDocument().getShowRelatedSubmissionsList())
            .isEqualTo(expectedRelatedSubmissionsFlag);
    }

    @Test
    void shouldListRelatedSubmissionsInDescendingDateOrder() {
        // Given
        LocalDateTime baseDateTime = LocalDateTime.parse("2026-05-04T10:00:00");

        final String genApp1Id = UUID.randomUUID().toString();
        GeneralApplication genApp1 = createGenApp(baseDateTime);
        genApp1.setRank(1);

        final String genApp2Id = UUID.randomUUID().toString();
        GeneralApplication genApp2 = createGenApp(baseDateTime.plusDays(10));
        genApp2.setRank(2);

        PartyEntity claimant1Party = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity defendant4Party = PartyEntity.builder().id(UUID.randomUUID()).build();
        when(partyService.getPartyLabel(mainClaim, claimant1Party.getId())).thenReturn("Claimant 1 Label");
        when(partyService.getPartyLabel(mainClaim, defendant4Party.getId())).thenReturn("Defendant 1 Label");

        CounterClaimEntity counterClaimEntity1 = createCounterClaimEntity(baseDateTime.minusDays(5), claimant1Party);
        CounterClaimEntity counterClaimEntity2 = createCounterClaimEntity(baseDateTime.plusDays(5), defendant4Party);
        CounterClaimEntity counterClaimEntity3 = createCounterClaimEntity(null, defendant4Party);
        counterClaimEntity3.setStatus(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED);

        PCSCase caseData = PCSCase.builder()
            .genApps(List.of(
                         ListValue.<GeneralApplication>builder().id(genApp1Id).value(genApp1).build(),
                         ListValue.<GeneralApplication>builder().id(genApp2Id).value(genApp2).build()
                     )
            )
            .legislativeCountry(ENGLAND)
            .build();

        when(pcsCaseEntity.getCounterClaims())
            .thenReturn(List.of(counterClaimEntity1, counterClaimEntity2, counterClaimEntity3));

        // When
        PCSCase result = underTest.start(toEventPayload(caseData));

        // Then
        DynamicStringList relatedSubmissionList = result.getCaseworkerDocument().getRelatedSubmission();

        List<DynamicStringListElement> listItems = relatedSubmissionList.getListItems();

        assertThat(listItems).map(DynamicStringListElement::getLabel)
            .containsExactly(
                "Gen app GA2 - submitted 14 May 2026",
                "Counter claim CC1 - submitted 9 May 2026",
                "Gen app GA1 - submitted 4 May 2026",
                "Counter claim CC1 - submitted 29 April 2026",
                "Not related to an application or counterclaim"
        );

        assertThat(listItems).map(DynamicStringListElement::getCode)
            .containsExactly(
                "GEN_APP:%s".formatted(genApp2Id),
                "COUNTERCLAIM:%s".formatted(counterClaimEntity2.getId()),
                "GEN_APP:%s".formatted(genApp1Id),
                "COUNTERCLAIM:%s".formatted(counterClaimEntity1.getId()),
                "NONE"
        );

    }

    @Test
    void shouldSetDocumentTypeListsForEngland() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .genApps(List.of())
            .legislativeCountry(ENGLAND)
            .build();

        // When
        PCSCase result = underTest.start(toEventPayload(caseData));

        // Then
        CaseworkerDocument caseworkerDocument = result.getCaseworkerDocument();
        assertThat(caseworkerDocument.getRelatedSubmissionsDocumentType().getListItems())
            .map(DynamicStringListElement::getCode)
            .contains(WITNESS_STATEMENT.name(), PART_20_COUNTERCLAIM.name()) // Common
            .contains(TENANCY_AGREEMENT.name()) // England only
            .doesNotContain(OCCUPATION_LICENCE.name()); // Wales only

        assertThat(caseworkerDocument.getRelatedSubmissionsDocumentType())
            .isSameAs(caseworkerDocument.getStandaloneDocumentType());

    }

    @Test
    void shouldSetDocumentTypeListsForWales() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .genApps(List.of())
            .legislativeCountry(WALES)
            .build();

        // When
        PCSCase result = underTest.start(toEventPayload(caseData));

        // Then
        CaseworkerDocument caseworkerDocument = result.getCaseworkerDocument();
        assertThat(caseworkerDocument.getRelatedSubmissionsDocumentType().getListItems())
            .map(DynamicStringListElement::getCode)
            .contains(WITNESS_STATEMENT.name(), PART_20_COUNTERCLAIM.name()) // Common
            .contains(OCCUPATION_LICENCE.name()) // Wales only
            .doesNotContain(TENANCY_AGREEMENT.name()); // England only

        assertThat(caseworkerDocument.getRelatedSubmissionsDocumentType())
            .isSameAs(caseworkerDocument.getStandaloneDocumentType());

    }

    private static EventPayload<PCSCase, State> toEventPayload(PCSCase caseData) {
        return new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);
    }

    @Test
    void shouldSetRelatedPartyListWithClaimantsFirst() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .genApps(List.of())
            .legislativeCountry(WALES)
            .build();

        PartyEntity claimantParty1 = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity defendantParty1 = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity defendantParty2 = PartyEntity.builder().id(UUID.randomUUID()).build();

        when(mainClaim.getClaimParties()).thenReturn(List.of(
            ClaimPartyEntity.builder()
                .role(PartyRole.DEFENDANT)
                .party(defendantParty1)
                .build(),
            ClaimPartyEntity.builder()
                .role(PartyRole.CLAIMANT)
                .party(claimantParty1)
                .build(),
            ClaimPartyEntity.builder()
                .role(PartyRole.DEFENDANT)
                .party(defendantParty2)
                .build()
        ));

        when(partyService.getPartyName(defendantParty1)).thenReturn("defendant 1 name");
        when(partyService.getPartyName(claimantParty1)).thenReturn("claimant 1 name");
        when(partyService.getPartyName(defendantParty2)).thenReturn("defendant 2 name");

        when(partyService.getPartyLabel(mainClaim, defendantParty1.getId())).thenReturn("defendant 1 label");
        when(partyService.getPartyLabel(mainClaim, claimantParty1.getId())).thenReturn("claimant 1 label");
        when(partyService.getPartyLabel(mainClaim, defendantParty2.getId())).thenReturn("defendant 2 label");

        // When
        PCSCase result = underTest.start(toEventPayload(caseData));

        // Then
        CaseworkerDocument caseworkerDocument = result.getCaseworkerDocument();

        assertThat(caseworkerDocument.getRelatedParty().getListItems())
            .map(DynamicListElement::getCode)
            .containsExactly(
                claimantParty1.getId(),
                defendantParty1.getId(),
                defendantParty2.getId()
            );

        assertThat(caseworkerDocument.getRelatedParty().getListItems())
            .map(DynamicListElement::getLabel)
            .containsExactly(
                "claimant 1 name - claimant 1 label",
                "defendant 1 name - defendant 1 label",
                "defendant 2 name - defendant 2 label"
            );
    }

    private static Stream<Arguments> relatedSubmissionsFlagScenarios() {
        GeneralApplication genApp = createGenApp(LocalDateTime.now());
        CounterClaimEntity counterClaimEntity = createCounterClaimEntity(LocalDateTime.now());

        return Stream.of(
            argumentSet("No genapps or counterclaims", List.of(), List.of(), VerticalYesNo.NO),
            argumentSet("Only genapps", List.of(genApp), List.of(), VerticalYesNo.YES),
            argumentSet("Only counterclaims", List.of(), List.of(counterClaimEntity), VerticalYesNo.YES),
            argumentSet("Genapps and counterclaims", List.of(genApp), List.of(counterClaimEntity), VerticalYesNo.YES)
        );
    }

    private static GeneralApplication createGenApp(LocalDateTime submittedOn) {
        return GeneralApplication.builder()
            .submittedOn(submittedOn)
            .build();
    }

    private static CounterClaimEntity createCounterClaimEntity(LocalDateTime submittedDate) {
        return createCounterClaimEntity(submittedDate, PartyEntity.builder().build());
    }

    private static CounterClaimEntity createCounterClaimEntity(LocalDateTime submittedDate, PartyEntity partyEntity) {
        return CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .claimSubmittedDate(submittedDate)
            .party(partyEntity)
            .build();
    }


}

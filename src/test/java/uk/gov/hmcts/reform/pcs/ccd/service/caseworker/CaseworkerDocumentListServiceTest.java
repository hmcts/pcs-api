package uk.gov.hmcts.reform.pcs.ccd.service.caseworker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;

class CaseworkerDocumentListServiceTest {

    private static final UUID CLAIMANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEFENDANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String GEN_APP_ID = "33333333-3333-3333-3333-333333333333";

    private FeatureToggleService featureToggleService;
    private CaseworkerDocumentListService underTest;

    @BeforeEach
    void setUp() {
        featureToggleService = mock(FeatureToggleService.class);
        underTest = new CaseworkerDocumentListService(
            new PartyService(null, null, null, null, null),
            featureToggleService);
    }

    @Test
    void shouldRetainSelectedRelatedSubmissionWhenListIsRebuilt() {
        DynamicStringList existingList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(GEN_APP_ID_PREFIX + ":" + GEN_APP_ID)
                .build())
            .build();

        DynamicStringList result = underTest.buildRelatedSubmissionsList(
            PcsCaseEntity.builder().build(),
            List.of(ListValue.<GeneralApplication>builder()
                .id(GEN_APP_ID)
                .value(GeneralApplication.builder()
                    .rank(1)
                    .state(GenAppState.GEN_APP_ISSUED)
                    .submittedOn(LocalDateTime.of(2026, 7, 1, 9, 30))
                    .build())
                .build()),
            List.of(),
            existingList
        );

        assertThat(result.getValue().getCode()).isEqualTo(GEN_APP_ID_PREFIX + ":" + GEN_APP_ID);
        assertThat(result.getValue().getLabel()).isEqualTo("General Application GA1 - submitted 1 July 2026");
    }

    @Test
    void shouldRetainSelectedRelatedPartyWhenListIsRebuilt() {
        PcsCaseEntity pcsCaseEntity = caseWithParties();
        DynamicList existingList = DynamicList.builder()
            .value(DynamicListElement.builder()
                .code(DEFENDANT_ID)
                .build())
            .build();

        DynamicList result = underTest.buildRelatedPartyList(pcsCaseEntity, existingList);

        assertThat(result.getValue().getCode()).isEqualTo(DEFENDANT_ID);
        assertThat(result.getValue().getLabel()).isEqualTo("Daniel Defendant - Defendant 1");
    }

    @Test
    void shouldExcludeRemovedPartiesFromRelatedPartyList() {
        PcsCaseEntity pcsCaseEntity = caseWithParties();
        PartyEntity removedDefendant = PartyEntity.builder()
            .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
            .firstName("Rory")
            .lastName("Removed")
            .removed(true)
            .build();
        pcsCaseEntity.getMainClaim().addParty(removedDefendant, PartyRole.DEFENDANT);

        DynamicList result = underTest.buildRelatedPartyList(pcsCaseEntity);

        assertThat(result.getListItems())
            .extracting(DynamicListElement::getCode)
            .containsExactly(CLAIMANT_ID, DEFENDANT_ID);
    }

    @Test
    void shouldRetainSelectedDocumentTypeWhenListIsRebuilt() {
        DynamicStringList existingList = DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(CaseworkerDocumentType.WITNESS_STATEMENT.name())
                .build())
            .build();

        DynamicStringList result = underTest.buildDocumentTypeList(LegislativeCountry.ENGLAND, existingList);

        assertThat(result.getValue().getCode()).isEqualTo(CaseworkerDocumentType.WITNESS_STATEMENT.name());
        assertThat(result.getValue().getLabel()).isEqualTo(CaseworkerDocumentType.WITNESS_STATEMENT.getLabel());
    }

    @Test
    void shouldUseDefendantNumberWhenRelease14IsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .party(PartyEntity.builder().id(DEFENDANT_ID).build())
            .rank(3)
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .claimSubmittedDate(LocalDateTime.of(2026, 5, 12, 10, 0))
            .build();

        DynamicStringList result = underTest.buildRelatedSubmissionsList(
            caseWithParties(), List.of(), List.of(counterClaim));

        assertThat(result.getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .contains("Counter claim CC1 - submitted 12 May 2026");
    }

    @Test
    void shouldUseStoredRankWhenRelease14IsOn() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);
        CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .id(UUID.randomUUID())
            .party(PartyEntity.builder().id(DEFENDANT_ID).build())
            .rank(3)
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .claimSubmittedDate(LocalDateTime.of(2026, 5, 12, 10, 0))
            .build();

        DynamicStringList result = underTest.buildRelatedSubmissionsList(
            caseWithParties(), List.of(), List.of(counterClaim));

        assertThat(result.getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .contains("Counter claim CC3 - submitted 12 May 2026");
    }

    private static PcsCaseEntity caseWithParties() {
        PartyEntity claimant = PartyEntity.builder()
            .id(CLAIMANT_ID)
            .firstName("Clara")
            .lastName("Claimant")
            .build();
        PartyEntity defendant = PartyEntity.builder()
            .id(DEFENDANT_ID)
            .firstName("Daniel")
            .lastName("Defendant")
            .build();
        ClaimEntity claim = ClaimEntity.builder().build();
        claim.addParty(claimant, PartyRole.CLAIMANT);
        claim.addParty(defendant, PartyRole.DEFENDANT);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        pcsCaseEntity.addClaim(claim);
        return pcsCaseEntity;
    }
}

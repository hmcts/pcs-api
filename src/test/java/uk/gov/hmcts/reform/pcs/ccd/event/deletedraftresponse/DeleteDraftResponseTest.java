package uk.gov.hmcts.reform.pcs.ccd.event.deletedraftresponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;
import uk.gov.hmcts.reform.pcs.exception.MultiplePartiesException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_4;

@ExtendWith(MockitoExtension.class)
class DeleteDraftResponseTest extends BaseEventTest {

    private static final String ORGANISATION_ID = "organisation-1";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private DefendantPartyExtractor defendantPartyExtractor;
    @Mock
    private DraftCaseDataService draftCaseDataService;

    private DeleteDraftResponse underTest;

    @BeforeEach
    void setUp() {
        underTest = new DeleteDraftResponse(
            pcsCaseService, organisationService, defendantPartyExtractor, draftCaseDataService);
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredForCaseIssuedState() {
        assertConfiguredForStates(State.CASE_ISSUED);
    }

    @Test
    void shouldBeConfiguredAsShowForDraftResponseAndFeatureFlag() {
        assertConfiguredShowConditions(ShowConditions.and(
            "hasDraftResponse=\"Yes\"",
            ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_4)));
    }

    @Test
    void shouldContainCorrectGrants() {
        assertGrants(UserRole.DEFENDANT_SOLICITOR, Permission.CRUD);
        assertGrants(UserRole.GA_DEFENDANT_SOLICITOR, Permission.CRUD);
    }

    @Test
    void shouldMakeDeleteDraftResponseMandatory() {
        assertThat(getEventFieldIds()).contains("deleteDraftResponse");
    }

    @Test
    void shouldDeleteDraftForRepresentedDefendantWhenAnswerIsYes() {
        // Given
        PartyEntity representedDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        stubRepresentedDefendants(List.of(representedDefendant));

        PCSCase caseData = PCSCase.builder()
            .deleteDraftResponse(YesOrNo.YES)
            .build();

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        verify(draftCaseDataService).deleteUnsubmittedCaseData(
            TEST_CASE_REFERENCE, respondPossessionClaim, representedDefendant.getId(), ORGANISATION_ID);

        assertThat(submitResponse.getState()).isNull();
        assertThat(submitResponse.getConfirmationBody())
            .contains("Your draft response has been deleted")
            .contains("Go back to the case list");
    }

    @Test
    void shouldThrowWhenNoRepresentedDefendantFound() {
        // Given
        stubRepresentedDefendants(List.of());

        PCSCase caseData = PCSCase.builder()
            .deleteDraftResponse(YesOrNo.YES)
            .build();

        // When / Then
        assertThatThrownBy(() -> callSubmitHandler(caseData))
            .isInstanceOf(PartyNotFoundException.class)
            .hasMessage("No represented party found");

        verify(draftCaseDataService, never())
            .deleteUnsubmittedCaseData(anyLong(), any(), any(), any());
    }

    @Test
    void shouldThrowWhenMoreThanOneRepresentedDefendantFound() {
        // Given
        stubRepresentedDefendants(List.of(
            PartyEntity.builder().id(UUID.randomUUID()).build(),
            PartyEntity.builder().id(UUID.randomUUID()).build()));

        PCSCase caseData = PCSCase.builder()
            .deleteDraftResponse(YesOrNo.YES)
            .build();

        // When / Then
        assertThatThrownBy(() -> callSubmitHandler(caseData))
            .isInstanceOf(MultiplePartiesException.class)
            .hasMessage("Deleting a draft response for multiple parties is not supported");

        verify(draftCaseDataService, never())
            .deleteUnsubmittedCaseData(anyLong(), any(), any(), any());
    }

    @Test
    void shouldNotDeleteDraftWhenAnswerIsNo() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .deleteDraftResponse(YesOrNo.NO)
            .build();

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        assertThat(submitResponse).isEqualTo(SubmitResponse.defaultResponse());
        verifyNoInteractions(pcsCaseService, defendantPartyExtractor);
        verify(draftCaseDataService, never())
            .deleteUnsubmittedCaseData(anyLong(), any(), any(), any());
    }

    private void stubRepresentedDefendants(List<PartyEntity> representedDefendants) {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORGANISATION_ID);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(defendantPartyExtractor.extractDefendantsRepresentedBy(pcsCaseEntity, ORGANISATION_ID))
            .thenReturn(representedDefendants);
    }

}

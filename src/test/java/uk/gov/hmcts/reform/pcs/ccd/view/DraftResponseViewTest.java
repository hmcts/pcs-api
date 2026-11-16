package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class DraftResponseViewTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String ORGANISATION_ID = "organisation-1";

    @Mock
    private DefendantPartyExtractor defendantPartyExtractor;
    @Mock
    private DraftCaseDataService draftCaseDataService;

    private DraftResponseView underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftResponseView(defendantPartyExtractor, draftCaseDataService);
    }

    @Test
    void shouldSetHasDraftResponseWhenRepresentedDefendantHasDraft() {
        // Given
        PartyEntity representedDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PCSCase pcsCase = PCSCase.builder().build();

        when(defendantPartyExtractor.extractDefendantsRepresentedBy(pcsCaseEntity, ORGANISATION_ID))
            .thenReturn(List.of(representedDefendant));
        when(draftCaseDataService.hasMeaningfulRespondDraft(
            CASE_REFERENCE, respondPossessionClaim, representedDefendant.getId(), ORGANISATION_ID))
            .thenReturn(true);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity, State.CASE_ISSUED, ORGANISATION_ID);

        // Then
        assertThat(pcsCase.getHasDraftResponse()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldNotSetHasDraftResponseWhenRepresentedDefendantHasNoDraft() {
        // Given
        PartyEntity representedDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PCSCase pcsCase = PCSCase.builder().build();

        when(defendantPartyExtractor.extractDefendantsRepresentedBy(pcsCaseEntity, ORGANISATION_ID))
            .thenReturn(List.of(representedDefendant));
        when(draftCaseDataService.hasMeaningfulRespondDraft(
            CASE_REFERENCE, respondPossessionClaim, representedDefendant.getId(), ORGANISATION_ID))
            .thenReturn(false);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity, State.CASE_ISSUED, ORGANISATION_ID);

        // Then
        assertThat(pcsCase.getHasDraftResponse()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldNotSetHasDraftResponseWhenNoRepresentedDefendants() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PCSCase pcsCase = PCSCase.builder().build();

        when(defendantPartyExtractor.extractDefendantsRepresentedBy(pcsCaseEntity, ORGANISATION_ID))
            .thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity, State.CASE_ISSUED, ORGANISATION_ID);

        // Then
        assertThat(pcsCase.getHasDraftResponse()).isEqualTo(YesOrNo.NO);
        verifyNoInteractions(draftCaseDataService);
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = "CASE_ISSUED", mode = EnumSource.Mode.EXCLUDE)
    void shouldNotSetHasDraftResponseWhenCaseIsNotIssued(State state) {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity, state, ORGANISATION_ID);

        // Then
        assertThat(pcsCase.getHasDraftResponse()).isEqualTo(YesOrNo.NO);
        verifyNoInteractions(defendantPartyExtractor, draftCaseDataService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldNotSetHasDraftResponseWhenOrganisationIdIsBlank(String organisationId) {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity, State.CASE_ISSUED, organisationId);

        // Then
        assertThat(pcsCase.getHasDraftResponse()).isEqualTo(YesOrNo.NO);
        verifyNoInteractions(defendantPartyExtractor, draftCaseDataService);
    }

}

package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthTest extends BasePageTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new StatementOfTruth(pcsCaseService, partyService));
    }

    @Test
    void shouldSetClaimantPartyIdAsApplicantId() {
        // Given
        UUID claimantPartyId = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PartyEntity claimantParty = mock(PartyEntity.class);
        when(partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity)).thenReturn(claimantParty);
        when(claimantParty.getId()).thenReturn(claimantPartyId);

        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder().build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getXuiGenAppRequest().getApplicantPartyId())
            .isEqualTo(claimantPartyId.toString());
    }
}

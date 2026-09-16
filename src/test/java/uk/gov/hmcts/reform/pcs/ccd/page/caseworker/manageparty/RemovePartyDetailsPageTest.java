package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.PARTY_CANNOT_BE_REMOVED_ERROR;

class RemovePartyDetailsPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RemovePartyDetailsPage());
    }

    @Test
    void shouldRejectNoSelection() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .removePartyDetails(RemovePartyDetails.builder()
                .removeSelectedParty(YesOrNo.NO)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo(PARTY_CANNOT_BE_REMOVED_ERROR);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldAcceptYesSelection() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .removePartyDetails(RemovePartyDetails.builder()
                .removeSelectedParty(YesOrNo.YES)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
    }
}

package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SelectPartyTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new SelectParty());
    }

    @Test
    void shouldSetCurrentRepresentedPartyIdAndName() {
        // Given
        UUID selectedRepresentedPartyUuid = UUID.randomUUID();
        String selectedRepresentedPartyName = "Some name";

        DynamicList representedParties = DynamicList.builder()
            .value(DynamicListElement.builder()
                       .code(selectedRepresentedPartyUuid)
                       .label(selectedRepresentedPartyName)
                       .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .representedPartyNames(representedParties)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getCurrentRepresentedPartyId()).isEqualTo(selectedRepresentedPartyUuid.toString());
        assertThat(updatedCaseData.getCurrentRepresentedPartyName()).isEqualTo(selectedRepresentedPartyName);
    }

}

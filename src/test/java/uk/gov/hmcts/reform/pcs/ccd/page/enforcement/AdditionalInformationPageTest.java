package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.AdditionalInformation.ADDITIONAL_INFORMATION_DETAILS_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

class AdditionalInformationPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextValidationService textValidationService = new TextValidationService();
        setPageUnderTest(new AdditionalInformationPage(textValidationService));
    }

    @Test
    void shouldHandleNoSelection() {
        // Given
        AdditionalInformation additionalInformation = AdditionalInformation.builder()
            .additionalInformationSelect(VerticalYesNo.NO)
            .build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .additionalInformation(additionalInformation)
            .build();
        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getEnforcementOrder().getAdditionalInformation().getAdditionalInformationDetails())
            .isNull();
    }

    @Test
    void shouldHandleYesSelection() {
        // Given
        String additionalInformationDetails = "Additional information details";
        AdditionalInformation additionalInformation = AdditionalInformation.builder()
            .additionalInformationSelect(VerticalYesNo.YES)
            .additionalInformationDetails(additionalInformationDetails)
            .build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .additionalInformation(additionalInformation)
            .build();
        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getEnforcementOrder().getAdditionalInformation().getAdditionalInformationDetails())
            .isEqualTo(additionalInformationDetails);
    }

    @Test
    void shouldHandleYesSelection_ToManyCharacters() {
        // Given
        String additionalInformationDetails = "a".repeat(RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT + 1);
        AdditionalInformation additionalInformation = AdditionalInformation.builder()
            .additionalInformationSelect(VerticalYesNo.YES)
            .additionalInformationDetails(additionalInformationDetails)
            .build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .additionalInformation(additionalInformation)
            .build();
        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors().getFirst()).contains(ADDITIONAL_INFORMATION_DETAILS_LABEL);
    }

}

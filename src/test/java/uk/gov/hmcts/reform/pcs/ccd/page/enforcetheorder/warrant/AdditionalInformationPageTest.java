package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation.ADDITIONAL_INFORMATION_DETAILS_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

class AdditionalInformationPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new AdditionalInformationPage(textAreaValidationService));
    }

    @Test
    void shouldHandleNoSelection() {
        // Given
        AdditionalInformation additionalInformation = AdditionalInformation.builder()
            .additionalInformationSelect(VerticalYesNo.NO)
            .build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(WarrantDetails.builder()
                .additionalInformation(additionalInformation)
                .build())
            .build();
        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getEnforcementOrder().getWarrantDetails()
                .getAdditionalInformation().getAdditionalInformationDetails()).isNull();
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
            .warrantDetails(WarrantDetails.builder()
                .additionalInformation(additionalInformation)
                .build())
            .build();
        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getEnforcementOrder().getWarrantDetails()
                .getAdditionalInformation().getAdditionalInformationDetails()).isEqualTo(additionalInformationDetails);
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
            .warrantDetails(WarrantDetails.builder()
                .additionalInformation(additionalInformation)
                .build())
            .build();
        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).contains(ADDITIONAL_INFORMATION_DETAILS_LABEL);
    }

}

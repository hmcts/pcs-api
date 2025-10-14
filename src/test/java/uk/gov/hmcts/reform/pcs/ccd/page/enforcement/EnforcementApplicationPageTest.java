package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;

@ExtendWith(MockitoExtension.class)
class EnforcementApplicationPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EnforcementApplicationPage());
    }

    @Test
    void shouldMaintainCaseDataOnMidEvent() {
        // Given
        String firstName = "Test";
        String lastName = "Testing";
        AddressUK address = AddressUK.builder().addressLine1("123 Test Street").addressLine2("Test Town").build();
        PCSCase caseData = PCSCase.builder()
            .defendant1(DefendantDetails.builder().firstName(firstName).lastName(lastName).build())
            .propertyAddress(address)
            .hasOtherAdditionalGrounds(YES)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getDefendant1().getFirstName()).isEqualTo(firstName);
        assertThat(response.getData().getDefendant1().getLastName()).isEqualTo(lastName);
        assertThat(response.getData().getPropertyAddress()).isEqualTo(address);
    }

}


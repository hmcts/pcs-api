package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactPreferencesTest extends BasePageTest {

    @Mock
    private AddressValidator addressValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ContactPreferences(addressValidator));
    }

    @Test
    void shouldReturnValidationErrorsWhenAddressInvalid() {
        // Given
        AddressUK contactAddress = mock(AddressUK.class);
        PCSCase caseData = PCSCase.builder()
            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
            .overriddenClaimantContactAddress(contactAddress)
            .build();

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(addressValidator.validateAddressFields(contactAddress)).thenReturn(expectedValidationErrors);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
    }
}

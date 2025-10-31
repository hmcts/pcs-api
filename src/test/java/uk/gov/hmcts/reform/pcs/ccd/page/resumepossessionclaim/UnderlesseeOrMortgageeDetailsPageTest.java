package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.UnderlesseeMortgageeValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class UnderlesseeOrMortgageeDetailsPageTest extends BasePageTest {

    @Mock
    private UnderlesseeMortgageeValidator underlesseeMortgageeValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new UnderlesseeOrMortgageeDetailsPage(underlesseeMortgageeValidator));
    }

    @Test
    void shouldValidateSingleUnderlesseeOrMortgagee() {

        // Given
        UnderlesseeMortgageeDetails underlesseeOrMortgagee = mock(UnderlesseeMortgageeDetails.class);

        PCSCase caseData = PCSCase.builder()
            .underlesseeOrMortgagee1(underlesseeOrMortgagee)
            .addAdditionalUnderlesseeOrMortgagee(VerticalYesNo.NO)
            .build();

        List<String> expectedValidationErrors = List.of("some error 1", "some error 2");
        when(underlesseeMortgageeValidator.validateUnderlesseeOrMortgagee1(underlesseeOrMortgagee, false))
            .thenReturn(expectedValidationErrors);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
        verify(underlesseeMortgageeValidator, never()).validateAdditionalUnderlesseeOrMortgagee(anyList());
    }

    @Test
    void shouldValidateMultipleUnderlesseeOrMortgagee() {

        // Given
        UnderlesseeMortgageeDetails underlesseeOrMortgagee = mock(UnderlesseeMortgageeDetails.class);
        UnderlesseeMortgageeDetails additionalUnderlessee = mock(UnderlesseeMortgageeDetails.class);
        UnderlesseeMortgageeDetails additionalMortgagee = mock(UnderlesseeMortgageeDetails.class);

        List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeMortgageeList = wrapListItems(List.of(
            additionalUnderlessee,
            additionalMortgagee
        ));

        PCSCase caseData = PCSCase.builder()
            .underlesseeOrMortgagee1(underlesseeOrMortgagee)
            .addAdditionalUnderlesseeOrMortgagee(VerticalYesNo.YES)
            .additionalUnderlesseeOrMortgagee(additionalUnderlesseeMortgageeList)
            .build();

        String errorMessage1 = "some error 1";
        String errorMessage2 = "some error 2";
        String errorMessage3 = "some error 3";

        when(underlesseeMortgageeValidator.validateUnderlesseeOrMortgagee1(underlesseeOrMortgagee, true))
            .thenReturn(List.of(errorMessage1));

        when(underlesseeMortgageeValidator.validateAdditionalUnderlesseeOrMortgagee(additionalUnderlesseeMortgageeList))
            .thenReturn(List.of(errorMessage2,errorMessage3));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(errorMessage1, errorMessage2, errorMessage3);

    }

}

package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addcasereviewdate.AddCaseReviewDateConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReviewDateService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddCaseReviewDateTest extends BaseEventTest {

    @Mock
    private AddCaseReviewDateConfigurer addCaseReviewDateConfigurer;
    @Mock
    private CaseReviewDateService caseReviewDateService;
    @Mock
    private AddressFormatter addressFormatter;

    @InjectMocks
    private AddCaseReviewDate addCaseReviewDate;

    @BeforeEach
    void setUp() {
        setEventUnderTest(addCaseReviewDate);
    }

    @Test
    void shouldConfigurePages() {
        // Given
        AddressUK address = AddressUK.builder().build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(address)
            .build();

        when(addressFormatter.formatShortAddress(address, AddressFormatter.COMMA_DELIMITER))
            .thenReturn("address");

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(addCaseReviewDateConfigurer).configurePages(any(PageBuilder.class));
    }


    @Test
    void shouldCallCaseReviewDateServiceOnSubmit() {
        // Given
        AddressUK address = AddressUK.builder().build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(address)
            .caseNameHmctsInternal("Claimant v Defendant")
            .build();

        when(addressFormatter.formatShortAddress(address, AddressFormatter.COMMA_DELIMITER))
            .thenReturn("address");

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        // Then
        verify(caseReviewDateService).addCaseReviewDate(TEST_CASE_REFERENCE, pcsCase);
        assertThat(submitResponse.getConfirmationBody()).isEqualTo(
            """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Review dates added</span><br>
            <span class="govuk-panel__body">Case number #1234</span><br>
            <span class="govuk-panel__body">address</span><br>
            <span class="govuk-panel__body">Claimant v Defendant</span><br>
            </div>

            <h3>What happens next</h3>

            Work allocation tasks will be created for court staff to complete on the review dates.
            """
        );
    }
}

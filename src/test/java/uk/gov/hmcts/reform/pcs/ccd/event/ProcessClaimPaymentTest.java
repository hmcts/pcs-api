package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ProcessClaimPaymentTest extends BaseEventTest {

    @Mock
    private PostCodeCourtService postCodeCourtService;
    @Mock
    private PcsCaseService pcsCaseService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new ProcessClaimPayment(postCodeCourtService, pcsCaseService));
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        PCSCase caseData = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);

        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(propertyAddress.getPostCode()).thenReturn("M37 5SF");

        callSubmitHandler(caseData);

        verify(pcsCaseService).patchCase(TEST_CASE_REFERENCE, caseData);
    }

}

package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private FeeApplier feeApplier;
    @Mock
    private EnterPropertyAddress enterPropertyAddress;
    @Mock
    private CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    @Mock
    private PropertyNotEligible propertyNotEligible;

    @BeforeEach
    void setUp() {
        CreatePossessionClaim underTest = new CreatePossessionClaim(
            pcsCaseService,
            feeApplier,
            enterPropertyAddress,
            crossBorderPostcodeSelection,
            propertyNotEligible
        );
        setEventUnderTest(underTest);
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        PCSCase caseData = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        LegislativeCountry legislativeCountry = mock(LegislativeCountry.class);

        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(caseData.getLegislativeCountry()).thenReturn(legislativeCountry);

        callSubmitHandler(caseData);

        verify(pcsCaseService).createCase(TEST_CASE_REFERENCE, propertyAddress, legislativeCountry);
    }

    @ParameterizedTest
    @ValueSource(strings = {"£0", "£123.45", "£123"})
    void shouldHandleWithVariousFormattedValues(String expectedFormattedFee) {
        PCSCase caseData = PCSCase.builder().build();

        doAnswer(invocation -> {
            PCSCase pcs = invocation.getArgument(0);
            BiConsumer<PCSCase, String> setter = invocation.getArgument(2);
            setter.accept(pcs, expectedFormattedFee);
            return null;
        }).when(feeApplier).applyFeeAmount(
            eq(caseData),
            eq(FeeTypes.CASE_ISSUE_FEE),
            any()
        );

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo(expectedFormattedFee);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(FeeTypes.CASE_ISSUE_FEE), any());

    }


    @Test
    void shouldSetDefaultFeeWhenFeeServiceThrowsRuntimeException() {
        PCSCase caseData = PCSCase.builder().build();
        String expectedFeesMessage = FeeApplier.UNABLE_TO_RETRIEVE;

        doAnswer(invocation -> {
            PCSCase pcs = invocation.getArgument(0);
            BiConsumer<PCSCase, String> setter = invocation.getArgument(2);
            try {
                throw new RuntimeException("Fee not found");
            } catch (RuntimeException e) {
                setter.accept(pcs, expectedFeesMessage);
            }
            return null;
        }).when(feeApplier).applyFeeAmount(
            eq(caseData),
            any(FeeTypes.class),
            any());

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo(expectedFeesMessage);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(FeeTypes.CASE_ISSUE_FEE), any());
    }

}

package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class UploadRequiredDocumentsWalesTest extends BasePageTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateMultipleTextAreas(any(), any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());

        setPageUnderTest(new UploadRequiredDocumentsWales(textAreaValidationService));
    }

    @Test
    void shouldValidateUploadRequiredDocumentsWalesInputs() {
        // Given
        String reason1 = "reasons1";
        String reason2 = "reasons2";
        String reason3 = "reasons3";
        PCSCase caseData = PCSCase.builder()
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .noEnergyPerformanceCertificateReason(reason1)
                    .noGasSafetyReportReason(reason2)
                    .noElectricalInstallationConditionReportReason(reason3)
                    .build()
            )
            .build();

        // when
        callMidEventHandler(caseData);

        verify(textAreaValidationService).validateMultipleTextAreas(
            argThat(f -> f.fieldValue.equals(reason1)
                && f.fieldLabel.equals("Why can you not upload a copy of the energy performance certificate?")
                && f.maxCharacters == 500),

            argThat(f -> f.fieldValue.equals(reason2)
                && f.fieldLabel.equals("Why can you not upload a copy of the gas safety report?")
                && f.maxCharacters == 500),

            argThat(f -> f.fieldValue.equals(reason3)
                && f.fieldLabel.equals(
                    "Why can you not upload a copy of the current Electrical Installation Condition Report (EICR)"
                )
                && f.maxCharacters == 500)
        );

    }
}

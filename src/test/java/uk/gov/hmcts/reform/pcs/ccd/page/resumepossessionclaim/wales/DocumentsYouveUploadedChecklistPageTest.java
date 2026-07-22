package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.UploadedDocumentChecklistType;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentsYouveUploadedChecklistPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new DocumentsYouveUploadedChecklistPage());
    }

    @Test
    void shouldRejectEmptySelection() {
        PCSCase caseData = PCSCase.builder()
            .documentsYouveUploaded(Set.of())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isEqualTo("Please select at least one option");
    }

    @Test
    void shouldRejectNullSelection() {
        PCSCase caseData = PCSCase.builder().build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isEqualTo("Please select at least one option");
    }

    @Test
    void shouldAcceptMultiSelectSelection() {
        PCSCase caseData = PCSCase.builder()
            .documentsYouveUploaded(Set.of(
                UploadedDocumentChecklistType.ENERGY_PERFORMANCE_CERTIFICATE,
                UploadedDocumentChecklistType.CURRENT_GAS_SAFETY_REPORT
            ))
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isNullOrEmpty();
        assertThat(response.getData().getDocumentsYouveUploaded()).containsExactlyInAnyOrder(
            UploadedDocumentChecklistType.ENERGY_PERFORMANCE_CERTIFICATE,
            UploadedDocumentChecklistType.CURRENT_GAS_SAFETY_REPORT
        );
    }
}

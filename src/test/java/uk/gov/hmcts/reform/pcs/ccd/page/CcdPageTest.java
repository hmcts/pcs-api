package uk.gov.hmcts.reform.pcs.ccd.page;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.UploadAdditionalDocumentsPage;

import static org.assertj.core.api.Assertions.assertThat;

class CcdPageTest {

    @Test
    void shouldReturnClassNameWithoutPageSuffixAndLeadingLowerCaseCharIfFound() {
        // Given
        Class<? extends CcdPage> clazz = UploadAdditionalDocumentsPage.class;

        // When
        String pageId = CcdPage.derivePageKey(clazz);

        // Then
        assertThat(pageId).isEqualTo("uploadAdditionalDocuments");
    }
}
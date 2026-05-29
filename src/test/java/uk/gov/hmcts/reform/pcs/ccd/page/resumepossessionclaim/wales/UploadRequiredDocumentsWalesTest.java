package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

public class UploadRequiredDocumentsWalesTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new UploadRequiredDocumentsWales());
    }

    @Test
    void shouldConfigureUploadRequiredDocumentsWalesPage() {
        assertThat(event.getFields().getPagesToMidEvent()).doesNotContainKey("uploadRequiredDocumentsWales");
    }

    @Test
    void shouldUseCcdMaxValidationForReasonTextAreas() throws NoSuchFieldException {
        assertThat(ccdAnnotation("noEpcReason").max()).isEqualTo(500);
        assertThat(ccdAnnotation("noGasReportReason").max()).isEqualTo(500);
        assertThat(ccdAnnotation("noEicrReason").max()).isEqualTo(500);
    }

    private static CCD ccdAnnotation(String fieldName) throws NoSuchFieldException {
        Field field = WalesDocuments.class.getDeclaredField(fieldName);
        return field.getAnnotation(CCD.class);
    }
}

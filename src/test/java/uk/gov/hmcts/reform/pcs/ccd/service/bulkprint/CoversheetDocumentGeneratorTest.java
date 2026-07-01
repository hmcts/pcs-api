package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoversheetDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private CoversheetDocumentGenerator underTest;

    @Test
    void rendersCoversheetTemplateAsPdf() {
        CoversheetPayload payload = CoversheetPayload.builder().recipientName("Jane Doe").build();
        when(docAssemblyService.generateDocument(
            payload, "CV-PCS-LET-ENG-Coversheet.docx", OutputType.PDF, "Coversheet"))
            .thenReturn("http://dm-store/documents/cover");

        String url = underTest.generate(payload);

        assertThat(url).isEqualTo("http://dm-store/documents/cover");
    }
}

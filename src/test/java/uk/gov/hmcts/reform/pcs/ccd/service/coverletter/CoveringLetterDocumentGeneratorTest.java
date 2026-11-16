package uk.gov.hmcts.reform.pcs.ccd.service.coverletter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.coverletter.CoveringLetterPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoveringLetterDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private CoveringLetterDocumentGenerator underTest;

    @Test
    void shouldRenderCoveringLetterTemplateAsPdf() {
        CoveringLetterPayload payload = CoveringLetterPayload.builder().recipientName("Jane Doe").build();
        when(docAssemblyService.generateDocument(
            payload, "CV-PCS-LET-ENG-Covering-Letter.docx", OutputType.PDF, "Covering Letter"))
            .thenReturn("http://dm-store/documents/letter");

        assertThat(underTest.generate(payload)).isEqualTo("http://dm-store/documents/letter");
    }
}

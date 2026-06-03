package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimPackDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private ClaimPackDocumentGenerator generator;

    @Test
    void delegatesToDocAssemblyServiceWithFixedTemplateAndFilename() {
        ClaimPackFormPayload payload = ClaimPackFormPayload.builder().build();
        when(docAssemblyService.generateDocument(
            eq(payload),
            eq(ClaimPackDocumentGenerator.TEMPLATE_ID),
            eq(OutputType.PDF),
            eq(ClaimPackDocumentGenerator.OUTPUT_FILENAME)
        )).thenReturn("https://dm-store/abc");

        String url = generator.generate(payload);

        assertThat(url).isEqualTo("https://dm-store/abc");
        verify(docAssemblyService).generateDocument(
            payload, ClaimPackDocumentGenerator.TEMPLATE_ID, OutputType.PDF,
            ClaimPackDocumentGenerator.OUTPUT_FILENAME
        );
    }

    @Test
    void templateIdMatchesRdoDocmosisNamingConvention() {
        // Sanity-check the template name format: CV-PCS-CLM-ENG-Claim-Pack.docx
        // (jurisdiction CV, service PCS, doc-type CLM, language ENG — per
        // docs/bulk-print/docmosis-template-management-gui.pdf)
        assertThat(ClaimPackDocumentGenerator.TEMPLATE_ID)
            .matches("^CV-PCS-CLM-(ENG|WEL)-.+\\.docx$");
    }
}

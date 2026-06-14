package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefenceFormDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private DefenceFormDocumentGenerator generator;

    @Test
    void delegatesToDocAssemblyWithPerDefendantFilename() {
        DefenceFormPayload payload = DefenceFormPayload.builder().build();
        when(docAssemblyService.generateDocument(
            eq(payload),
            eq(DefenceFormDocumentGenerator.TEMPLATE_ID),
            eq(OutputType.PDF),
            eq("Defence - Defendant 2")
        )).thenReturn("https://dm-store/abc");

        String url = generator.generate(payload, 2);

        assertThat(url).isEqualTo("https://dm-store/abc");
        verify(docAssemblyService).generateDocument(
            payload, DefenceFormDocumentGenerator.TEMPLATE_ID, OutputType.PDF, "Defence - Defendant 2");
    }

    @Test
    void templateIdMatchesRdoDocmosisNamingConvention() {
        assertThat(DefenceFormDocumentGenerator.TEMPLATE_ID)
            .matches("^CV-PCS-DEF-(ENG|WEL)-.+\\.docx$");
    }
}

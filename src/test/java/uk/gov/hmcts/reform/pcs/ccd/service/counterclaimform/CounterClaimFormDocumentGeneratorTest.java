package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimFormDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private CounterClaimFormDocumentGenerator generator;

    @Test
    void delegatesToDocAssemblyWithPerDefendantFilename() {
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder().build();
        when(docAssemblyService.generateDocument(eq(payload),
            eq(CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID),
            eq(OutputType.PDF),
            eq("Counterclaim - Defendant 2")
        )).thenReturn("https://dm-store/abc");

        String url = generator.generate(payload, 2);

        assertThat(url).isEqualTo("https://dm-store/abc");
        verify(docAssemblyService).generateDocument(
            payload, CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID, OutputType.PDF, "Counterclaim - Defendant 2");
    }

    @Test
    void usesLegalRepTemplateWhenPayloadFlagged() {
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder()
            .completedByLegalRepresentative(true).build();
        when(docAssemblyService.generateDocument(eq(payload),
            eq(CounterClaimFormDocumentGenerator.LR_TEMPLATE_ID),
            eq(OutputType.PDF),
            eq("Counterclaim - Defendant 1")
        )).thenReturn("https://dm-store/lr");

        String url = generator.generate(payload, 1);

        assertThat(url).isEqualTo("https://dm-store/lr");
        verify(docAssemblyService).generateDocument(
            payload, CounterClaimFormDocumentGenerator.LR_TEMPLATE_ID, OutputType.PDF, "Counterclaim - Defendant 1");
    }

    @Test
    void templateIdsMatchRdoDocmosisNamingConvention() {
        assertThat(CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID)
            .matches("^CV-PCS-CLM-(ENG|WEL)-.+\\.docx$");
        assertThat(CounterClaimFormDocumentGenerator.LR_TEMPLATE_ID)
            .matches("^CV-PCS-CLM-(ENG|WEL)-.+-LR\\.docx$");
    }
}

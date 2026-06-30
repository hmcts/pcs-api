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
        when(docAssemblyService.generateDocument((payload),
            eq(CounterClaimFormDocumentGenerator.TEMPLATE_ID),
            eq(OutputType.PDF),
            eq("Counterclaim - Defendant 2")
        )).thenReturn("https://dm-store/abc");

        String url = generator.generate(payload, 2);

        assertThat(url).isEqualTo("https://dm-store/abc");
        verify(docAssemblyService).generateDocument(
            payload, CounterClaimFormDocumentGenerator.TEMPLATE_ID, OutputType.PDF, "Counterclaim - Defendant 2");
    }

    @Test
    void templateIdMatchesRdoDocmosisNamingConvention() {
        assertThat(CounterClaimFormDocumentGenerator.TEMPLATE_ID)
            .matches("^CV-PCS-CLM-(ENG|WEL)-.+\\.docx$");
    }
}

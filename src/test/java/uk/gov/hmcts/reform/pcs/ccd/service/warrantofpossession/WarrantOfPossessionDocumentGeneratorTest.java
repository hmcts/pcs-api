package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantOfPossessionDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private WarrantOfPossessionDocumentGenerator underTest;

    @Test
    void shouldGenerateTheWarrantAsAPdfNamedWithItsRank() {
        WarrantOfPossessionPayload payload = WarrantOfPossessionPayload.builder().build();
        when(docAssemblyService.generateDocument(payload, WarrantOfPossessionDocumentGenerator.TEMPLATE_ID,
            OutputType.PDF, "Warrant of possession - application 2")).thenReturn("document url");

        assertThat(underTest.generate(payload, 2)).isEqualTo("document url");
    }
}

package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritOfPossessionDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private WritOfPossessionDocumentGenerator underTest;

    @Test
    void shouldGenerateTheWritAsAPdfNamedWithItsRank() {
        WritOfPossessionPayload payload = WritOfPossessionPayload.builder().build();
        when(docAssemblyService.generateDocument(payload, WritOfPossessionDocumentGenerator.TEMPLATE_ID,
            OutputType.PDF, "Writ of possession - application 2")).thenReturn("document url");

        assertThat(underTest.generate(payload, 2)).isEqualTo("document url");
    }
}

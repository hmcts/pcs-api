package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.hearingnotice.HearingNoticePayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingNoticeDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private HearingNoticeDocumentGenerator underTest;

    @Test
    void shouldGenerateTheNoticeAsAPdfWithTheGivenFilename() {
        HearingNoticePayload payload = HearingNoticePayload.builder().build();
        when(docAssemblyService.generateDocument(payload, HearingNoticeDocumentGenerator.TEMPLATE_ID,
            OutputType.PDF, "Notice of hearing 1 - Defendant 1")).thenReturn("document url");

        assertThat(underTest.generate(payload, "Notice of hearing 1 - Defendant 1")).isEqualTo("document url");
    }
}

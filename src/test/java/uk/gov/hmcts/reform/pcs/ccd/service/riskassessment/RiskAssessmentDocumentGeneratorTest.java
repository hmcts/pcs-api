package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @InjectMocks
    private RiskAssessmentDocumentGenerator underTest;

    @Test
    void shouldGenerateTheRiskAssessmentAsAPdfWithTheGivenFilename() {
        RiskAssessmentPayload payload = RiskAssessmentPayload.builder().build();
        when(docAssemblyService.generateDocument(payload, RiskAssessmentDocumentGenerator.TEMPLATE_ID,
            OutputType.PDF, "Risk assessment")).thenReturn("document url");

        assertThat(underTest.generate(payload, "Risk assessment")).isEqualTo("document url");
    }
}

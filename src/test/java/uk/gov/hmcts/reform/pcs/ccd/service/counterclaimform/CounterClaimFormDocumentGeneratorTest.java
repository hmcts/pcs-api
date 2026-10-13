package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimFormDocumentGeneratorTest {

    @Mock
    private DocAssemblyService docAssemblyService;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private CounterClaimFormDocumentGenerator generator;

    @BeforeEach
    void enableRelease14() {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);
    }

    @Test
    void delegatesToDocAssemblyWithPerDefendantFilename() {
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder().build();
        when(docAssemblyService.generateDocument(payload,
            CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID,
            OutputType.PDF,
            "Counterclaim CC1 - Defendant 2"
        )).thenReturn("https://dm-store/abc");

        String url = generator.generate(payload, 1, 2);

        assertThat(url).isEqualTo("https://dm-store/abc");
        verify(docAssemblyService).generateDocument(
            payload, CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID, OutputType.PDF,
            "Counterclaim CC1 - Defendant 2");
    }

    @Test
    void usesDefendantNumberOnlyWhenRelease14IsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder().build();
        when(docAssemblyService.generateDocument(payload,
            CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID,
            OutputType.PDF,
            "Counterclaim - Defendant 2"
        )).thenReturn("https://dm-store/abc");

        String url = generator.generate(payload, 3, 2);

        assertThat(url).isEqualTo("https://dm-store/abc");
        verify(docAssemblyService).generateDocument(
            payload, CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID, OutputType.PDF,
            "Counterclaim - Defendant 2");
    }

    @Test
    void usesLegalRepTemplateWhenPayloadFlagged() {
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder()
            .completedByLegalRepresentative(true).build();
        when(docAssemblyService.generateDocument(payload,
            CounterClaimFormDocumentGenerator.LR_TEMPLATE_ID,
            OutputType.PDF,
            "Counterclaim CC1 - Defendant 1"
        )).thenReturn("https://dm-store/lr");

        String url = generator.generate(payload, 1, 1);

        assertThat(url).isEqualTo("https://dm-store/lr");
        verify(docAssemblyService).generateDocument(
            payload, CounterClaimFormDocumentGenerator.LR_TEMPLATE_ID, OutputType.PDF,
            "Counterclaim CC1 - Defendant 1");
    }

    @Test
    void expectedCounterClaimFormFilenameMatchesGeneratedFilename() {
        assertThat(CounterClaimFormDocumentGenerator.expectedCounterClaimFormFilename(2))
            .isEqualTo("Counterclaim - Defendant 2");
    }

    @Test
    void templateIdsMatchRdoDocmosisNamingConvention() {
        assertThat(CounterClaimFormDocumentGenerator.LIP_TEMPLATE_ID)
            .matches("^CV-PCS-CLM-(ENG|WEL)-.+\\.docx$");
        assertThat(CounterClaimFormDocumentGenerator.LR_TEMPLATE_ID)
            .matches("^CV-PCS-CLM-(ENG|WEL)-.+-LR\\.docx$");
    }
}

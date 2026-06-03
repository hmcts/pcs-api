package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimPackServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimPackPayloadBuilder payloadBuilder;
    @Mock
    private ClaimPackDocumentGenerator documentGenerator;

    @InjectMocks
    private ClaimPackService claimPackService;

    @Test
    void loadsCaseBuildsPayloadGeneratesPdfAndReturnsUrl() {
        PcsCaseEntity loaded = new PcsCaseEntity();
        ClaimPackFormPayload payload = ClaimPackFormPayload.builder().build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(payloadBuilder.build(loaded)).thenReturn(payload);
        when(documentGenerator.generate(payload)).thenReturn("https://dm-store/xyz");

        String url = claimPackService.generateAndRender(CASE_REFERENCE);

        assertThat(url).isEqualTo("https://dm-store/xyz");

        // Verify the three steps happen in order: load → build → generate.
        InOrder order = inOrder(pcsCaseService, payloadBuilder, documentGenerator);
        order.verify(pcsCaseService).loadCase(CASE_REFERENCE);
        order.verify(payloadBuilder).build(loaded);
        order.verify(documentGenerator).generate(payload);
        order.verifyNoMoreInteractions();
    }
}

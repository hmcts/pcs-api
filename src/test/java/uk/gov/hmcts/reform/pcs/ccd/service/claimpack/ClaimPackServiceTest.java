package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimPackServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimPackPayloadBuilder payloadBuilder;
    @Mock
    private ClaimPackDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;

    @InjectMocks
    private ClaimPackService claimPackService;

    @Test
    void rendersStoresAndAttachesClaimPackToClaim() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        ClaimPackFormPayload payload = ClaimPackFormPayload.builder().build();
        DocumentEntity document = DocumentEntity.builder().build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimPackDocument()).thenReturn(null);
        when(payloadBuilder.build(loaded)).thenReturn(payload);
        when(documentGenerator.generate(payload)).thenReturn(DM_STORE_URL);
        when(documentImportService.addDocumentToCase(
            CASE_REFERENCE, DM_STORE_URL, CaseFileCategory.STATEMENTS_OF_CASE)).thenReturn(document);

        claimPackService.generateAndAttach(CASE_REFERENCE);

        // load, build, generate, store, then attach, in order.
        InOrder order = inOrder(pcsCaseService, payloadBuilder, documentGenerator, documentImportService, claim);
        order.verify(pcsCaseService).loadCase(CASE_REFERENCE);
        order.verify(payloadBuilder).build(loaded);
        order.verify(documentGenerator).generate(payload);
        order.verify(documentImportService)
            .addDocumentToCase(CASE_REFERENCE, DM_STORE_URL, CaseFileCategory.STATEMENTS_OF_CASE);
        order.verify(claim).setClaimPackDocument(document);
    }

    @Test
    void skipsRegenerationWhenClaimPackAlreadyAttached() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimPackDocument()).thenReturn(DocumentEntity.builder().build());

        claimPackService.generateAndAttach(CASE_REFERENCE);

        // Already attached: no render, no store, no second attach.
        verifyNoInteractions(payloadBuilder, documentGenerator, documentImportService);
        verify(claim, never()).setClaimPackDocument(org.mockito.ArgumentMatchers.any());
    }
}

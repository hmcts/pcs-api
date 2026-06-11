package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimFormServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimFormPayloadBuilder payloadBuilder;
    @Mock
    private ClaimFormDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private ClaimActivityLogService claimActivityLogService;

    @InjectMocks
    private ClaimFormService claimFormService;

    @Test
    void rendersStoresAndAttachesClaimFormToClaim() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        ClaimFormPayload payload = ClaimFormPayload.builder().build();
        DocumentEntity document = DocumentEntity.builder().build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimFormDocument()).thenReturn(null);
        when(payloadBuilder.build(loaded)).thenReturn(payload);
        when(documentGenerator.generate(payload)).thenReturn(DM_STORE_URL);
        when(documentImportService.addDocumentToCase(
            CASE_REFERENCE, DM_STORE_URL, CaseFileCategory.STATEMENTS_OF_CASE)).thenReturn(document);

        claimFormService.generateAndAttach(CASE_REFERENCE);

        // load, build, generate, store, then attach, in order.
        InOrder order = inOrder(pcsCaseService, payloadBuilder, documentGenerator, documentImportService, claim);
        order.verify(pcsCaseService).loadCase(CASE_REFERENCE);
        order.verify(payloadBuilder).build(loaded);
        order.verify(documentGenerator).generate(payload);
        order.verify(documentImportService)
            .addDocumentToCase(CASE_REFERENCE, DM_STORE_URL, CaseFileCategory.STATEMENTS_OF_CASE);
        order.verify(claim).setClaimFormDocument(document);

        // AC01: document typed as a Claim, and a success activity-log entry written in this transaction.
        assertThat(document.getType()).isEqualTo(DocumentType.CLAIM);
        verify(claimActivityLogService).logGenerationSuccess(CASE_REFERENCE);
    }

    @Test
    void skipsRegenerationWhenClaimFormAlreadyAttached() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimFormDocument()).thenReturn(DocumentEntity.builder().build());

        claimFormService.generateAndAttach(CASE_REFERENCE);

        // Already attached: no render, no store, no second attach, no activity-log entry.
        verifyNoInteractions(payloadBuilder, documentGenerator, documentImportService, claimActivityLogService);
        verify(claim, never()).setClaimFormDocument(org.mockito.ArgumentMatchers.any());
    }
}

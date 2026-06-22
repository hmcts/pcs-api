package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimFormPersistenceServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimFormPayloadBuilder payloadBuilder;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private ClaimActivityLogService claimActivityLogService;

    @InjectMocks
    private ClaimFormPersistenceService underTest;

    @Test
    void buildsPayloadWhenNotAttached() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        ClaimFormPayload payload = ClaimFormPayload.builder().build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimFormDocument()).thenReturn(null);
        when(payloadBuilder.build(loaded)).thenReturn(payload);

        assertThat(underTest.buildPayloadIfNotAttached(CASE_REFERENCE)).contains(payload);
    }

    @Test
    void returnsEmptyAndDoesNotBuildWhenAlreadyAttached() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimFormDocument()).thenReturn(DocumentEntity.builder().build());

        assertThat(underTest.buildPayloadIfNotAttached(CASE_REFERENCE)).isEmpty();
        verifyNoInteractions(payloadBuilder);
    }

    @Test
    void attachStoresTypesLinksDocumentAndLogsSuccess() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        DocumentEntity document = DocumentEntity.builder().build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimFormDocument()).thenReturn(null);
        when(documentImportService.addDocumentToCase(
            CASE_REFERENCE, DM_STORE_URL, CaseFileCategory.STATEMENTS_OF_CASE)).thenReturn(document);

        underTest.attach(CASE_REFERENCE, DM_STORE_URL);

        assertThat(document.getType()).isEqualTo(DocumentType.CLAIM);
        verify(claim).setClaimFormDocument(document);
        verify(claimActivityLogService).logGenerationSuccess(CASE_REFERENCE);
    }

    @Test
    void attachSkipsWhenAlreadyAttached() {
        ClaimEntity claim = mock(ClaimEntity.class);
        PcsCaseEntity loaded = mock(PcsCaseEntity.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(loaded);
        when(loaded.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimFormDocument()).thenReturn(DocumentEntity.builder().build());

        underTest.attach(CASE_REFERENCE, DM_STORE_URL);

        verify(claim, never()).setClaimFormDocument(any());
        verifyNoInteractions(documentImportService, claimActivityLogService);
    }
}

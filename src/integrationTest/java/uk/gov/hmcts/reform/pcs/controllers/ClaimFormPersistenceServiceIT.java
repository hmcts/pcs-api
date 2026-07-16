package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormPersistenceService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the scheduler's discovery invariant: the claim form document and its DOCUMENTS_CREATED activity-log
 * row are written in one transaction, so a committed document can never exist without its discovery row (and
 * the sweep can never miss a case that has documents).
 */
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Claim form generation - document and DOCUMENTS_CREATED are written atomically")
class ClaimFormPersistenceServiceIT extends AbstractPostgresContainerIT {

    private static final String DM_STORE_URL = "http://dm-store/documents/11111111-1111-1111-1111-111111111111";

    @Autowired
    private ClaimFormPersistenceService claimFormPersistenceService;
    @Autowired
    private CaseCreationHelper caseCreationHelper;
    @Autowired
    private ClaimRepository claimRepository;

    @MockitoBean
    private DocumentImportService documentImportService;
    @MockitoBean
    private ClaimActivityLogService claimActivityLogService;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private CaseAssignmentApi caseAssignmentApi;

    @Test
    @DisplayName("rolls back the attached document when the DOCUMENTS_CREATED write fails")
    void documentRolledBackWhenActivityLogWriteFails() {
        long caseReference = 1781000000000011L;
        final PcsCaseEntity caseEntity = caseCreationHelper.createTestCaseWithParty(
            caseReference, UUID.randomUUID(), PartyRole.CLAIMANT);
        when(documentImportService.addDocumentToCase(eq(caseReference), anyString(), any()))
            .thenReturn(DocumentEntity.builder().documentId(UUID.randomUUID()).build());
        doThrow(new RuntimeException("activity log write failed"))
            .when(claimActivityLogService).logGenerationSuccess(caseReference);

        assertThrows(RuntimeException.class, () -> claimFormPersistenceService.attach(caseReference, DM_STORE_URL));

        UUID claimId = caseEntity.getClaims().getFirst().getId();
        assertThat(claimRepository.findById(claimId).orElseThrow().getClaimFormDocument())
            .as("the document write must roll back with the failed activity-log write")
            .isNull();
    }

    @Test
    @DisplayName("attaches the document and records generation success in the same unit")
    void attachesDocumentAndRecordsGenerationSuccess() {
        long caseReference = 1781000000000012L;
        final PcsCaseEntity caseEntity = caseCreationHelper.createTestCaseWithParty(
            caseReference, UUID.randomUUID(), PartyRole.CLAIMANT);
        when(documentImportService.addDocumentToCase(eq(caseReference), anyString(), any()))
            .thenReturn(DocumentEntity.builder().documentId(UUID.randomUUID()).build());

        claimFormPersistenceService.attach(caseReference, DM_STORE_URL);

        UUID claimId = caseEntity.getClaims().getFirst().getId();
        assertThat(claimRepository.findById(claimId).orElseThrow().getClaimFormDocument()).isNotNull();
        verify(claimActivityLogService).logGenerationSuccess(caseReference);
    }
}

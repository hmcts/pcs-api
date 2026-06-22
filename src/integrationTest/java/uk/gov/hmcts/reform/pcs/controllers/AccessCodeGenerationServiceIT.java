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
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeGenerationService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.PinPackDocumentGenerator;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Access code generation - transaction isolation")
class AccessCodeGenerationServiceIT extends AbstractPostgresContainerIT {

    @Autowired
    private AccessCodeGenerationService accessCodeGenerationService;
    @Autowired
    private CaseCreationHelper caseCreationHelper;
    @Autowired
    private PartyAccessCodeRepository partyAccessCodeRepository;

    @MockitoBean
    private PinPackDocumentGenerator pinPackDocumentGenerator;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private CaseAssignmentApi caseAssignmentApi;

    @Test
    @DisplayName("one defendant's failure does not roll back another's committed access code")
    void perDefendantTransactionIsolation() {
        PcsCaseEntity caseEntity = caseCreationHelper.createTestCaseWithMultipleDefendants(
            1781000000000001L, UUID.randomUUID(), UUID.randomUUID());
        List<PartyEntity> defendants = caseCreationHelper.getDefendants(caseEntity);
        UUID failingDefendantId = defendants.get(0).getId();
        UUID succeedingDefendantId = defendants.get(1).getId();

        when(pinPackDocumentGenerator.generatePinPack(any(), any(),
            argThat(party -> party != null && failingDefendantId.equals(party.getId())), anyString()))
            .thenThrow(new RuntimeException("docmosis down"));
        when(pinPackDocumentGenerator.generatePinPack(any(), any(),
            argThat(party -> party != null && succeedingDefendantId.equals(party.getId())), anyString()))
            .thenReturn("http://dm-store/documents/ok");

        assertThrows(IllegalStateException.class,
            () -> accessCodeGenerationService.createAccessCodesForParties("1781000000000001", true));

        List<UUID> partyIdsWithCode = partyAccessCodeRepository.findAllByPcsCase_Id(caseEntity.getId()).stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .toList();
        assertThat(partyIdsWithCode).containsExactly(succeedingDefendantId);
    }

    @Test
    @DisplayName("re-run skips defendants that already have a code")
    void retryIsIdempotent() {
        PcsCaseEntity caseEntity = caseCreationHelper.createTestCaseWithMultipleDefendants(
            1781000000000002L, UUID.randomUUID(), UUID.randomUUID());
        List<PartyEntity> defendants = caseCreationHelper.getDefendants(caseEntity);
        UUID firstDefendantId = defendants.get(0).getId();
        UUID secondDefendantId = defendants.get(1).getId();

        when(pinPackDocumentGenerator.generatePinPack(any(), any(), any(), anyString()))
            .thenReturn("http://dm-store/documents/ok");

        accessCodeGenerationService.createAccessCodesForParties("1781000000000002", true);
        accessCodeGenerationService.createAccessCodesForParties("1781000000000002", true);

        List<UUID> partyIdsWithCode = partyAccessCodeRepository.findAllByPcsCase_Id(caseEntity.getId()).stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .toList();
        assertThat(partyIdsWithCode).containsExactlyInAnyOrder(firstDefendantId, secondDefendantId);
    }
}

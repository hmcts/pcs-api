package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeFormDocumentGenerator;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage.ENGLISH;
import static uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage.WELSH;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Access code generation - transaction isolation")
class AccessCodeGenerationServiceIT extends AbstractPostgresContainerIT {

    private static final String ENGLISH_URL = "http://dm-store/documents/22222222-2222-2222-2222-222222222222";
    private static final String WELSH_URL = "http://dm-store/documents/33333333-3333-3333-3333-333333333333";

    @Autowired
    private AccessCodeGenerationService accessCodeGenerationService;
    @Autowired
    private CaseCreationHelper caseCreationHelper;
    @Autowired
    private PartyAccessCodeRepository partyAccessCodeRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AccessCodeFormDocumentGenerator accessCodeFormDocumentGenerator;
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
            1781000000000001L, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        List<PartyEntity> defendants = caseCreationHelper.getDefendants(caseEntity);
        UUID failingDefendantId = defendants.get(0).getId();
        UUID succeedingDefendantId = defendants.get(1).getId();

        when(accessCodeFormDocumentGenerator.generate(any(), any(),
            argThat(party -> party != null && failingDefendantId.equals(party.getId())), anyString(), any()))
            .thenThrow(new RuntimeException("docmosis down"));
        when(accessCodeFormDocumentGenerator.generate(any(), any(),
            argThat(party -> party != null && succeedingDefendantId.equals(party.getId())), anyString(), any()))
            .thenReturn("http://dm-store/documents/11111111-1111-1111-1111-111111111111");

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
            1781000000000002L, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        List<PartyEntity> defendants = caseCreationHelper.getDefendants(caseEntity);
        final UUID firstDefendantId = defendants.get(0).getId();
        final UUID secondDefendantId = defendants.get(1).getId();

        when(accessCodeFormDocumentGenerator.generate(any(), any(), any(), anyString(), any()))
            .thenReturn("http://dm-store/documents/11111111-1111-1111-1111-111111111111");

        accessCodeGenerationService.createAccessCodesForParties("1781000000000002", true);
        accessCodeGenerationService.createAccessCodesForParties("1781000000000002", true);

        List<UUID> partyIdsWithCode = partyAccessCodeRepository.findAllByPcsCase_Id(caseEntity.getId()).stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .toList();
        assertThat(partyIdsWithCode).containsExactlyInAnyOrder(firstDefendantId, secondDefendantId);
    }

    @Test
    @DisplayName("a failed Welsh render rolls back both letters and the code; the retry creates both")
    void walesWelshRenderFailureRollsBackBothLettersThenRetryCreatesBoth() {
        PcsCaseEntity caseEntity = caseCreationHelper.createTestCaseWithMultipleDefendants(
            1781000000000021L, UUID.randomUUID().toString(), UUID.randomUUID().toString(), LegislativeCountry.WALES);
        List<PartyEntity> defendants = caseCreationHelper.getDefendants(caseEntity);
        final UUID d1 = defendants.get(0).getId();
        final UUID d2 = defendants.get(1).getId();

        doReturn(ENGLISH_URL).when(accessCodeFormDocumentGenerator)
            .generate(any(), any(), any(), anyString(), eq(ENGLISH));
        doThrow(new DocAssemblyException("Document generation failed")).doReturn(WELSH_URL)
            .when(accessCodeFormDocumentGenerator)
            .generate(any(), any(), argThat(party -> party != null && d1.equals(party.getId())), anyString(),
                      eq(WELSH));
        doReturn(WELSH_URL).when(accessCodeFormDocumentGenerator)
            .generate(any(), any(), argThat(party -> party != null && d2.equals(party.getId())), anyString(),
                      eq(WELSH));

        assertThrows(IllegalStateException.class,
            () -> accessCodeGenerationService.createAccessCodesForParties("1781000000000021", true));

        assertThat(partyIdsWithCode(caseEntity)).containsExactly(d2);
        assertThat(letterFileNames(d1)).isEmpty();
        assertThat(letterFileNames(d2)).containsExactlyInAnyOrder(null, "Defendant Access Code Welsh " + d2);
        assertThat(failureDetails(d1)).singleElement().satisfies(details -> assertThat(details)
            .contains("Welsh letter: DocAssemblyException")
            .contains("RENDER_FAILED"));

        accessCodeGenerationService.createAccessCodesForParties("1781000000000021", true);

        assertThat(partyIdsWithCode(caseEntity)).containsExactlyInAnyOrder(d1, d2);
        assertThat(letterFileNames(d1)).containsExactlyInAnyOrder(null, "Defendant Access Code Welsh " + d1);
        assertThat(letterFileNames(d2)).hasSize(2);
    }

    private List<UUID> partyIdsWithCode(PcsCaseEntity caseEntity) {
        return partyAccessCodeRepository.findAllByPcsCase_Id(caseEntity.getId()).stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .toList();
    }

    private List<String> letterFileNames(UUID partyId) {
        return jdbcTemplate.queryForList(
            "SELECT file_name FROM document WHERE party_id = ? AND type = 'DEFENDANT_ACCESS_CODE'",
            String.class, partyId);
    }

    private List<String> failureDetails(UUID partyId) {
        return jdbcTemplate.queryForList(
            "SELECT details::text FROM claim_activity_log WHERE party_id = ? AND activity_type = 'DOCUMENTS_CREATED'"
                + " AND status = 'FAILURE'",
            String.class, partyId);
    }
}

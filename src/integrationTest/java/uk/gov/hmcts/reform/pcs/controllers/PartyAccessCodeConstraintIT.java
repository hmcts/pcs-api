package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("party_access_code - one access code per party per case")
class PartyAccessCodeConstraintIT extends AbstractPostgresContainerIT {

    @Autowired
    private CaseCreationHelper caseCreationHelper;
    @Autowired
    private PartyAccessCodeRepository partyAccessCodeRepository;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private CaseAssignmentApi caseAssignmentApi;

    @Test
    @DisplayName("rejects a second code for the same defendant, even with a different code value")
    void rejectsDuplicateCodeForSameParty() {
        PcsCaseEntity caseEntity = caseCreationHelper.createTestCaseWithMultipleDefendants(
            1781000000000003L, UUID.randomUUID(), UUID.randomUUID());
        PartyEntity defendant = caseCreationHelper.getDefendants(caseEntity).get(0);

        partyAccessCodeRepository.saveAndFlush(accessCode(caseEntity, defendant, "CODE_A"));

        assertThatThrownBy(() ->
            partyAccessCodeRepository.saveAndFlush(accessCode(caseEntity, defendant, "CODE_B")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("allows one access code for each defendant on the case")
    void allowsOneCodePerDefendant() {
        PcsCaseEntity caseEntity = caseCreationHelper.createTestCaseWithMultipleDefendants(
            1781000000000004L, UUID.randomUUID(), UUID.randomUUID());
        List<PartyEntity> defendants = caseCreationHelper.getDefendants(caseEntity);

        partyAccessCodeRepository.saveAndFlush(accessCode(caseEntity, defendants.get(0), "CODE_1"));
        partyAccessCodeRepository.saveAndFlush(accessCode(caseEntity, defendants.get(1), "CODE_2"));

        assertThat(partyAccessCodeRepository.findAllByPcsCase_Id(caseEntity.getId())).hasSize(2);
    }

    private PartyAccessCodeEntity accessCode(PcsCaseEntity pcsCase, PartyEntity defendant, String code) {
        return PartyAccessCodeEntity.builder()
            .partyId(defendant.getId())
            .pcsCase(pcsCase)
            .code(code)
            .role(PartyRole.DEFENDANT)
            .build();
    }
}

package uk.gov.hmcts.reform.pcs.noc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NocServiceTest {

    private static final long CASE_ID = 1712345678901234L;
    private static final String AUTHORISATION = "Bearer user-token";
    private static final String USER_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private IdamService idamService;

    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;

    @Mock
    private LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    private NocService nocService;

    @BeforeEach
    void setUp() {
        nocService = new NocService(
            pcsCaseService,
            idamService,
            caseRoleAssignmentService,
            legalRepresentativePartyLinkService
        );
    }

    @Test
    void shouldReturnDefendantNameQuestions() {
        when(pcsCaseService.loadCase(CASE_ID)).thenReturn(caseWithDefendants(defendant("John", "Smith")));

        NocQuestionsResponse response = nocService.getQuestions(CASE_ID);

        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().getFirst().questionId()).isEqualTo(NocService.FIRST_NAME_QUESTION_ID);
        assertThat(response.questions().getLast().questionId()).isEqualTo(NocService.LAST_NAME_QUESTION_ID);
    }

    @Test
    void shouldVerifyMatchingAnswers() {
        when(pcsCaseService.loadCase(CASE_ID)).thenReturn(caseWithDefendants(defendant("John", "Smith")));

        assertThat(nocService.verifyAnswers(answers("john", "smith"))).isTrue();
    }

    @Test
    void shouldRejectNoMatchingDefendant() {
        when(pcsCaseService.loadCase(CASE_ID)).thenReturn(caseWithDefendants(defendant("John", "Smith")));

        assertThatThrownBy(() -> nocService.verifyAnswers(answers("Jane", "Smith")))
            .isInstanceOf(NoticeOfChangeAnswersException.class)
            .hasMessage(NocService.ANSWERS_NOT_MATCHED_ANY_LITIGANT);
    }

    @Test
    void shouldRejectAmbiguousDefendantMatch() {
        when(pcsCaseService.loadCase(CASE_ID)).thenReturn(caseWithDefendants(
            defendant("John", "Smith"),
            defendant("John", "Smith")
        ));

        assertThatThrownBy(() -> nocService.verifyAnswers(answers("John", "Smith")))
            .isInstanceOf(NoticeOfChangeAnswersException.class)
            .hasMessage(NocService.ANSWERS_NOT_IDENTIFY_LITIGANT);
    }

    @Test
    void shouldSubmitApprovedNoCAndLinkDefendantSolicitor() {
        PartyEntity defendant = defendant("John", "Smith");
        when(pcsCaseService.loadCase(CASE_ID)).thenReturn(caseWithDefendants(defendant));
        UserInfo userInfo = new UserInfo(null, USER_ID, "John", "Solicitor", "solicitor@test.com", List.of());
        when(idamService.validateAuthToken(AUTHORISATION)).thenReturn(new User(AUTHORISATION, userInfo));

        NocSubmissionResponse response = nocService.submit(AUTHORISATION, answers("John", "Smith"));

        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
        verify(caseRoleAssignmentService).assignRasRole(CASE_ID, USER_ID, UserRole.DEFENDANT_SOLICITOR);
        verify(legalRepresentativePartyLinkService).linkLegalRepresentativeToParty(
            CASE_ID,
            defendant.getId().toString(),
            userInfo
        );
    }

    private NocAnswersRequest answers(String firstName, String lastName) {
        return new NocAnswersRequest(CASE_ID, List.of(
            new NocAnswer(NocService.FIRST_NAME_QUESTION_ID, firstName),
            new NocAnswer(NocService.LAST_NAME_QUESTION_ID, lastName)
        ));
    }

    private PcsCaseEntity caseWithDefendants(PartyEntity... parties) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_ID).build();
        ClaimEntity claim = ClaimEntity.builder().build();
        pcsCase.addClaim(claim);

        for (PartyEntity party : parties) {
            claim.addParty(party, PartyRole.DEFENDANT);
        }

        return pcsCase;
    }

    private PartyEntity defendant(String firstName, String lastName) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }
}

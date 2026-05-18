package uk.gov.hmcts.reform.pcs.noc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NocService {

    public static final String FIRST_NAME_QUESTION_ID = "pcs-defendant-first-name";
    public static final String LAST_NAME_QUESTION_ID = "pcs-defendant-last-name";
    public static final String ANSWERS_EMPTY = "Challenge question answers can not be empty";
    public static final String ANSWERS_MISMATCH_QUESTIONS = "The number of provided answers must match the number of questions";
    public static final String ANSWERS_NOT_IDENTIFY_LITIGANT = "The answers did not uniquely identify a litigant";
    public static final String ANSWERS_NOT_MATCHED_ANY_LITIGANT = "The answers did not match those for any litigant";

    private static final String CASE_TYPE_ID = "PCS";

    private final PcsCaseService pcsCaseService;
    private final IdamService idamService;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    public NocQuestionsResponse getQuestions(long caseId) {
        pcsCaseService.loadCase(caseId);
        return new NocQuestionsResponse(List.of(
            textQuestion("1", "What is the defendant's first name?", FIRST_NAME_QUESTION_ID),
            textQuestion("2", "What is the defendant's last name?", LAST_NAME_QUESTION_ID)
        ));
    }

    public boolean verifyAnswers(NocAnswersRequest request) {
        resolveDefendantParty(request);
        return true;
    }

    @Transactional
    public NocSubmissionResponse submit(String authorisation, NocAnswersRequest request) {
        PartyEntity defendant = resolveDefendantParty(request);
        User user = idamService.validateAuthToken(authorisation);
        UserInfo userDetails = user.getUserDetails();

        caseRoleAssignmentService.assignRasRole(request.caseId(), userDetails.getUid(), UserRole.DEFENDANT_SOLICITOR);
        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            request.caseId(),
            defendant.getId().toString(),
            userDetails
        );

        return new NocSubmissionResponse("APPROVED");
    }

    private PartyEntity resolveDefendantParty(NocAnswersRequest request) {
        validateRequest(request);
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(request.caseId());
        Map<String, String> answersById = Map.of(
            FIRST_NAME_QUESTION_ID, getAnswer(request, FIRST_NAME_QUESTION_ID),
            LAST_NAME_QUESTION_ID, getAnswer(request, LAST_NAME_QUESTION_ID)
        );

        List<PartyEntity> matches = pcsCase.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .filter(Objects::nonNull)
            .filter(party -> namesMatch(party, answersById))
            .toList();

        if (matches.isEmpty()) {
            throw new NoticeOfChangeAnswersException(ANSWERS_NOT_MATCHED_ANY_LITIGANT);
        }

        if (matches.size() > 1) {
            throw new NoticeOfChangeAnswersException(ANSWERS_NOT_IDENTIFY_LITIGANT);
        }

        return matches.getFirst();
    }

    private void validateRequest(NocAnswersRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new NoticeOfChangeAnswersException(ANSWERS_EMPTY);
        }

        if (request.answers().size() != 2
            || request.answers().stream().noneMatch(answer -> FIRST_NAME_QUESTION_ID.equals(answer.questionId()))
            || request.answers().stream().noneMatch(answer -> LAST_NAME_QUESTION_ID.equals(answer.questionId()))) {
            throw new NoticeOfChangeAnswersException(ANSWERS_MISMATCH_QUESTIONS);
        }
    }

    private String getAnswer(NocAnswersRequest request, String questionId) {
        return request.answers().stream()
            .filter(answer -> questionId.equals(answer.questionId()))
            .findFirst()
            .map(NocAnswer::value)
            .orElse("");
    }

    private boolean namesMatch(PartyEntity party, Map<String, String> answersById) {
        return normalise(party.getFirstName()).equals(normalise(answersById.get(FIRST_NAME_QUESTION_ID)))
            && normalise(party.getLastName()).equals(normalise(answersById.get(LAST_NAME_QUESTION_ID)));
    }

    private String normalise(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private NocQuestion textQuestion(String order, String text, String questionId) {
        return new NocQuestion(
            CASE_TYPE_ID,
            order,
            text,
            new NocQuestion.AnswerFieldType("Text", "Text", null, null, null, List.of(), List.of(), null),
            "1",
            "NoC",
            null,
            questionId
        );
    }
}

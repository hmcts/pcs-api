package uk.gov.hmcts.reform.pcs.noc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswer;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswersResponse;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswersRequest;
import uk.gov.hmcts.ccd.sdk.api.noc.NocQuestionBuilder;
import uk.gov.hmcts.ccd.sdk.api.noc.NocQuestionsResponse;
import uk.gov.hmcts.ccd.sdk.api.noc.NocSubmissionResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NocService implements CCDConfig<PCSCase, State, UserRole> {

    public static final String FIRST_NAME_QUESTION_ID = "pcs-defendant-first-name";
    public static final String LAST_NAME_QUESTION_ID = "pcs-defendant-last-name";

    private final PcsCaseService pcsCaseService;
    private final IdamService idamService;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.noc()
            .questions(this::getQuestions)
            .verifyAnswers(this::verifyAnswers)
            .submit(this::submit);
    }

    public NocQuestionsResponse getQuestions(long caseId, NocQuestionBuilder questions) {
        return questions.response(
            questions.text("What is the defendant's first name?", "NoC", FIRST_NAME_QUESTION_ID),
            questions.text("What is the defendant's last name?", "NoC", LAST_NAME_QUESTION_ID)
        );
    }

    public NocAnswersResponse verifyAnswers(NocAnswersRequest request) {
        return resolveDefendantParty(request).toVerificationResponse();
    }

    @Transactional
    public NocSubmissionResponse submit(String authorisation, NocAnswersRequest request) {
        DefendantResolution resolution = resolveDefendantParty(request);
        if (!resolution.isResolved()) {
            return NocSubmissionResponse.invalid(resolution.error().code(), resolution.error().message());
        }

        PartyEntity defendant = resolution.defendant();
        User user = idamService.validateAuthToken(authorisation);
        UserInfo userDetails = user.getUserDetails();

        caseRoleAssignmentService.assignRasRole(
            request.caseId(),
            userDetails.getUid(),
            UserRole.DEFENDANT_SOLICITOR
        );
        Optional<String> previousLegalRepresentativeId = legalRepresentativePartyLinkService
            .linkLegalRepresentativeToParty(
                request.caseId(),
                defendant.getId().toString(),
                userDetails
            ).map(Object::toString);
        previousLegalRepresentativeId
            .filter(previousUserId -> !previousUserId.equals(userDetails.getUid()))
            .ifPresent(previousUserId -> caseRoleAssignmentService.revokeRasRole(
                request.caseId(),
                previousUserId,
                UserRole.DEFENDANT_SOLICITOR
            ));

        return NocSubmissionResponse.approved();
    }

    private DefendantResolution resolveDefendantParty(NocAnswersRequest request) {
        Optional<NocAnswersResponse> validationError = validateRequest(request);
        if (validationError.isPresent()) {
            return DefendantResolution.invalid(validationError.get());
        }

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
            return DefendantResolution.invalid(NocAnswersResponse.answersNotMatchedAnyLitigant());
        }

        if (matches.size() > 1) {
            return DefendantResolution.invalid(NocAnswersResponse.answersNotIdentifyLitigant());
        }

        return DefendantResolution.resolved(matches.getFirst());
    }

    private Optional<NocAnswersResponse> validateRequest(NocAnswersRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            return Optional.of(NocAnswersResponse.answersEmpty());
        }

        if (request.answers().size() != 2) {
            return Optional.of(NocAnswersResponse.answersMismatchQuestions(
                2,
                request.answers().size()
            ));
        }

        if (request.answers().stream().noneMatch(answer -> FIRST_NAME_QUESTION_ID.equals(answer.questionId()))) {
            return Optional.of(NocAnswersResponse.noAnswerProvidedForQuestion(FIRST_NAME_QUESTION_ID));
        }

        if (request.answers().stream().noneMatch(answer -> LAST_NAME_QUESTION_ID.equals(answer.questionId()))) {
            return Optional.of(NocAnswersResponse.noAnswerProvidedForQuestion(LAST_NAME_QUESTION_ID));
        }

        return Optional.empty();
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

    private record DefendantResolution(PartyEntity defendant, NocAnswersResponse error) {

        static DefendantResolution resolved(PartyEntity defendant) {
            return new DefendantResolution(defendant, null);
        }

        static DefendantResolution invalid(NocAnswersResponse error) {
            return new DefendantResolution(null, error);
        }

        boolean isResolved() {
            return defendant != null;
        }

        NocAnswersResponse toVerificationResponse() {
            return isResolved() ? NocAnswersResponse.verified() : error;
        }
    }
}

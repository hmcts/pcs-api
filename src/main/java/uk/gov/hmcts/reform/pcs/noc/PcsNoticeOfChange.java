package uk.gov.hmcts.reform.pcs.noc;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswer;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswersRequest;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswersResponse;
import uk.gov.hmcts.ccd.sdk.api.noc.NocOrganisation;
import uk.gov.hmcts.ccd.sdk.api.noc.NocSubmissionResponse;
import uk.gov.hmcts.ccd.sdk.api.noc.NocSubmitContext;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.task.NocAccessChangeTaskComponent;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.ccd.sdk.api.noc.NocError.ANSWERS_NOT_IDENTIFY_LITIGANT;
import static uk.gov.hmcts.ccd.sdk.api.noc.NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT;
import static uk.gov.hmcts.ccd.sdk.api.noc.NocError.REQUESTING_ORG_ALREADY_REPRESENTS_PARTY;

@Component
@RequiredArgsConstructor
public class PcsNoticeOfChange implements CCDConfig<PCSCase, State, UserRole> {

    private static final String FIRST_NAME_QUESTION_ID = "pcs-defendant-first-name";
    private static final String LAST_NAME_QUESTION_ID = "pcs-defendant-last-name";
    private static final String CHALLENGE_ID = "NoCChallenge";

    private static final int EXPECTED_ANSWER_COUNT = 2;
    private static final UserRole CASE_ROLE = UserRole.DEFENDANT_SOLICITOR;

    private static final String NO_DEFENDANTS_FOUND_MESSAGE = "We cannot find a defendant matching this name."
        + " Enter their name exactly as it appears on any documents received from the court";

    private static final String DUPLICATE_DEFENDANT_NAME_MESSAGE = "A notice of change cannot be completed for this "
        + "defendant as there is more than one defendant with the same name on this case."
        + " Contact the issuing court for help.";

    private static final String ORG_ALREADY_REPRESENTS_PARTY_MESSAGE = "Your organisation already has access"
        + " to this case."
        + "You or a colleague are already representing this client on this case."
        + " Contact the issuing court for help.";

    private static final String FEATURE_FLAG_DISABLED_CODE = "feature-disabled";

    private static final String FEATURE_FLAG_DISABLED_MESSAGE = "The Notice of change feature is "
        + "currently disabled";


    private final PcsCaseRepository pcsCaseRepository;
    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final SchedulerClient schedulerClient;
    private final FeatureToggleService featureToggleService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {

        var noticeOfChange = builder.noticeOfChange()
            .validate(this::validate)
            .submit(this::submit);

        var challenge = noticeOfChange.challenge(CHALLENGE_ID);
        challenge
            .question(FIRST_NAME_QUESTION_ID, "Enter client first name")
            .answer(CASE_ROLE)
            .complex(PCSCase::getDefendant1)
            .field(DefendantDetails::getFirstName)
            .done()
            .question(LAST_NAME_QUESTION_ID, "Enter client last name")
            .answer(CASE_ROLE)
            .complex(PCSCase::getDefendant1)
            .field(DefendantDetails::getLastName);
    }

    public NocAnswersResponse validate(NocSubmitContext context, NocAnswersRequest request) {
        if (isFeatureDisabled()) {
            return NocAnswersResponse.invalid(FEATURE_FLAG_DISABLED_CODE, FEATURE_FLAG_DISABLED_MESSAGE);
        }

        Optional<NocAnswersResponse> validationError = validateRequest(request);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        PcsCaseEntity pcsCase = loadCase(request.caseId());
        List<PartyEntity> matches = matchingDefendants(pcsCase, request);
        validationError = validateMatches(matches);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        PartyEntity matchedParty = matches.getFirst();
        OrganisationDetailsResponse organisation = organisationDetailsService.getOrganisationDetails(context.userId());

        if (legalRepresentativeRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
            organisation.getOrganisationIdentifier(),
            matchedParty.getId()
        )) {
            return NocAnswersResponse.invalid(REQUESTING_ORG_ALREADY_REPRESENTS_PARTY.code(),
                                                          ORG_ALREADY_REPRESENTS_PARTY_MESSAGE);
        }

        return NocAnswersResponse.verified(new NocOrganisation(
            organisation.getOrganisationIdentifier(),
            organisation.getName()
        ));
    }

    public NocSubmissionResponse submit(NocSubmitContext context, NocAnswersRequest request) {
        PcsCaseEntity pcsCase = loadCase(request.caseId());
        PartyEntity matchedParty = matchingDefendants(pcsCase, request).getFirst();
        UUID currentUserId = currentUserId(context);
        OrganisationDetailsResponse organisationDetails = organisationDetailsService.getOrganisationDetails(
            currentUserId.toString());

        NocAccessChangePlan accessChangePlan = planAccessChanges(
            pcsCase,
            matchedParty,
            context.userId(),
            organisationDetails
        );

        scheduleAccessChanges(accessChangePlan);

        return NocSubmissionResponse.approved(CASE_ROLE.getRole());
    }

    private NocAccessChangePlan planAccessChanges(
        PcsCaseEntity pcsCase,
        PartyEntity matchedParty,
        String currentUserIdString,
        OrganisationDetailsResponse organisationDetailsResponse
    ) {
        List<NocAccessChangeTaskData> changes = new ArrayList<>();
        changes.add(accessChange(pcsCase.getCaseReference(), currentUserIdString, organisationDetailsResponse,
                                 matchedParty.getId()));
        return new NocAccessChangePlan(changes);
    }

    private NocAccessChangeTaskData accessChange(
        long caseReference,
        String userId,
        OrganisationDetailsResponse organisationDetailsResponse,
        UUID partyId
    ) {
        return NocAccessChangeTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .organisationDetailsResponse(organisationDetailsResponse)
            .userId(userId)
            .partyId(partyId.toString())
            .build();
    }

    private void scheduleAccessChanges(NocAccessChangePlan accessChangePlan) {
        accessChangePlan.changes().forEach(change -> schedulerClient.scheduleIfNotExists(
            NocAccessChangeTaskComponent.NOC_ACCESS_CHANGE_TASK_DESCRIPTOR
                .instance(taskId(change))
                .data(change)
                .scheduledTo(Instant.now())
        ));
    }

    private String taskId(NocAccessChangeTaskData change) {
        return "noc-%s-%s".formatted(
            change.getCaseReference(),
            change.getUserId()
        );
    }

    private Optional<NocAnswersResponse> validateRequest(NocAnswersRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            return Optional.of(NocAnswersResponse.answersEmpty());
        }

        if (request.answers().size() != EXPECTED_ANSWER_COUNT) {
            return Optional.of(NocAnswersResponse.answersMismatchQuestions(
                EXPECTED_ANSWER_COUNT,
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

    private PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    private List<PartyEntity> matchingDefendants(PcsCaseEntity pcsCase, NocAnswersRequest request) {
        Map<String, String> answersById = Map.of(
            FIRST_NAME_QUESTION_ID, getAnswer(request, FIRST_NAME_QUESTION_ID),
            LAST_NAME_QUESTION_ID, getAnswer(request, LAST_NAME_QUESTION_ID)
        );

        return pcsCase.getParties().stream()
            .filter(Objects::nonNull)
            .filter(this::isDefendant)
            .filter(party -> namesMatch(party, answersById))
            .toList();
    }

    private Optional<NocAnswersResponse> validateMatches(List<PartyEntity> matches) {
        if (matches.isEmpty()) {
            return Optional.of(NocAnswersResponse.invalid(ANSWERS_NOT_MATCHED_ANY_LITIGANT.code(),
                                                          NO_DEFENDANTS_FOUND_MESSAGE
            ));
        }

        if (matches.size() > 1) {
            return Optional.of(NocAnswersResponse.invalid(ANSWERS_NOT_IDENTIFY_LITIGANT.code(),
                                                          DUPLICATE_DEFENDANT_NAME_MESSAGE
            ));
        }

        return Optional.empty();
    }

    private boolean isDefendant(PartyEntity party) {
        return party.getClaimParties().stream()
            .anyMatch(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT);
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
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private UUID currentUserId(NocSubmitContext context) {
        return UUID.fromString(context.userId());
    }

    private record NocAccessChangePlan(List<NocAccessChangeTaskData> changes) {
    }

    private boolean isFeatureDisabled() {
        return !this.featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)
            || !this.featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR);

    }
}

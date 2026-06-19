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
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeAction;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.task.NocAccessChangeTaskComponent;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativePartyLinkService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PcsNoticeOfChange implements CCDConfig<PCSCase, State, UserRole> {

    static final String FIRST_NAME_QUESTION_ID = "pcs-defendant-first-name";
    static final String LAST_NAME_QUESTION_ID = "pcs-defendant-last-name";
    static final String CHALLENGE_ID = "NoC";

    private static final int EXPECTED_ANSWER_COUNT = 2;
    private static final UserRole CASE_ROLE = UserRole.DEFENDANT;

    private final PcsCaseRepository pcsCaseRepository;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeRepository;
    private final LegalRepresentativePartyLinkService legalRepresentativePartyLinkService;
    private final OrganisationDetailsService organisationDetailsService;
    private final SchedulerClient schedulerClient;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> builder) {
        var noticeOfChange = builder.noticeOfChange()
            .validate(this::validate)
            .submit(this::submit);

        var challenge = noticeOfChange.challenge(CHALLENGE_ID);
        challenge
            .question(FIRST_NAME_QUESTION_ID, "What is the defendant's first name?")
            .answer(CASE_ROLE)
            .complex(PCSCase::getDefendant1)
            .field(DefendantDetails::getFirstName)
            .done()
            .question(LAST_NAME_QUESTION_ID, "What is the defendant's last name?")
            .answer(CASE_ROLE)
            .complex(PCSCase::getDefendant1)
            .field(DefendantDetails::getLastName)
            .done();
    }

    public NocAnswersResponse validate(NocSubmitContext context, NocAnswersRequest request) {
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
        UUID currentUserId = currentUserId(context);
        if (legalRepresentativeRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
            currentUserId.toString(),
            matchedParty.getId()
        )) {
            return NocAnswersResponse.requestingOrgAlreadyRepresentsParty();
        }

        OrganisationDetailsResponse organisation = organisationDetailsService.getOrganisationDetails(context.userId());
        return NocAnswersResponse.verified(new NocOrganisation(
            organisation.getOrganisationIdentifier(),
            organisation.getName()
        ));
    }

    public NocSubmissionResponse submit(NocSubmitContext context, NocAnswersRequest request) {
        NocAnswersResponse validationResponse = validate(context, request);
        if (!validationResponse.isValid()) {
            return NocSubmissionResponse.invalid(validationResponse.code(), validationResponse.message());
        }

        PcsCaseEntity pcsCase = loadCase(request.caseId());
        PartyEntity matchedParty = matchingDefendants(pcsCase, request).getFirst();
        UUID currentUserId = currentUserId(context);
        Optional<LegalRepresentativeOrganisationEntity> incumbent = legalRepresentativeRepository
            .findByOrganisationIdAndCaseReference(currentUserId.toString(), pcsCase.getCaseReference());
        NocAccessChangePlan accessChangePlan = planAccessChanges(
            pcsCase,
            matchedParty,
            incumbent,
            currentUserId,
            context.userId()
        );

        OrganisationDetailsResponse organisationDetails = organisationDetailsService.getOrganisationDetails(
            currentUserId.toString());

        legalRepresentativePartyLinkService.linkLegalRepresentativeToParty(
            pcsCase.getCaseReference(),
            matchedParty.getId().toString(),
            currentUserId,
            organisationDetails
        );

        scheduleAccessChanges(accessChangePlan);

        return NocSubmissionResponse.approved(CASE_ROLE.getRole());
    }

    private NocAccessChangePlan planAccessChanges(
        PcsCaseEntity pcsCase,
        PartyEntity matchedParty,
        Optional<LegalRepresentativeOrganisationEntity> incumbent,
        UUID currentUserId,
        String currentUserIdString
    ) {
        List<NocAccessChangeTaskData> changes = new ArrayList<>();
        if (!legalRepresentativeRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
            currentUserId.toString(),
            matchedParty.getId()
        )) {
            changes.add(accessChange(pcsCase.getCaseReference(), currentUserIdString, NocAccessChangeAction.GRANT));
        }

//        incumbent
//            .filter(representation -> shouldRevokeIncumbent(representation, pcsCase, matchedParty, currentUserId))
//            .map(LegalRepresentativeEntity::getIdamId)
//            .map(UUID::toString)
//            .map(userId -> accessChange(pcsCase.getCaseReference(), userId, NocAccessChangeAction.REVOKE))
//            .ifPresent(changes::add);

        return new NocAccessChangePlan(changes);
    }

    private boolean shouldRevokeIncumbent(
        LegalRepresentativeOrganisationEntity incumbent,
        PcsCaseEntity pcsCase,
        PartyEntity matchedParty,
        UUID currentUserId
    ) {

        return false;
//        UUID previousUserId = incumbent.getIdamId();
//        return previousUserId != null
//            && !previousUserId.equals(currentUserId)
//            && !legalRepresentativeRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
//                incumbent.getId(),
//                pcsCase.getCaseReference(),
//                matchedParty.getId()
//            );
    }

    private NocAccessChangeTaskData accessChange(
        long caseReference,
        String userId,
        NocAccessChangeAction action
    ) {
        return NocAccessChangeTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .userId(userId)
            .action(action)
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
        return "noc-%s-%s-%s".formatted(
            change.getAction().name().toLowerCase(),
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
            return Optional.of(NocAnswersResponse.answersNotMatchedAnyLitigant());
        }

        if (matches.size() > 1) {
            return Optional.of(NocAnswersResponse.answersNotIdentifyLitigant());
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
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private UUID currentUserId(NocSubmitContext context) {
        return UUID.fromString(context.userId());
    }

    private UserInfo userInfo(NocSubmitContext context) {
        return new UserInfo(
            context.email(),
            context.userId(),
            context.name(),
            context.givenName(),
            context.familyName(),
            context.roles()
        );
    }

    private record NocAccessChangePlan(List<NocAccessChangeTaskData> changes) {
    }
}

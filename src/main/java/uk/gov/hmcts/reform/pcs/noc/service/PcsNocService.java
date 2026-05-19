package uk.gov.hmcts.reform.pcs.noc.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobType;
import uk.gov.hmcts.reform.pcs.noc.entity.PartyRepresentationEntity;
import uk.gov.hmcts.reform.pcs.noc.entity.PartyRepresentationSource;
import uk.gov.hmcts.reform.pcs.noc.entity.PartyRepresentationStatus;
import uk.gov.hmcts.reform.pcs.noc.exception.NocException;
import uk.gov.hmcts.reform.pcs.noc.model.NocAnswer;
import uk.gov.hmcts.reform.pcs.noc.model.NocAnswersRequest;
import uk.gov.hmcts.reform.pcs.noc.model.NocApprovalStatus;
import uk.gov.hmcts.reform.pcs.noc.model.NocFieldType;
import uk.gov.hmcts.reform.pcs.noc.model.NocQuestion;
import uk.gov.hmcts.reform.pcs.noc.model.NocQuestionsResponse;
import uk.gov.hmcts.reform.pcs.noc.repository.PartyRepresentationRepository;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationUsersResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

@Service
public class PcsNocService {

    static final String DEFENDANT_FIRST_NAME_QUESTION_ID = "pcs-defendant-first-name";
    static final String DEFENDANT_LAST_NAME_QUESTION_ID = "pcs-defendant-last-name";
    static final String DEFENDANT_SOLICITOR_ROLE = "[DEFENDANTSOLICITOR]";
    static final String CASE_TYPE_ID = "PCS";

    private final PcsCaseRepository pcsCaseRepository;
    private final PartyRepresentationRepository partyRepresentationRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final IdamService idamService;
    private final NocSideEffectService sideEffectService;

    public PcsNocService(
        PcsCaseRepository pcsCaseRepository,
        PartyRepresentationRepository partyRepresentationRepository,
        OrganisationDetailsService organisationDetailsService,
        IdamService idamService,
        NocSideEffectService sideEffectService
    ) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepresentationRepository = partyRepresentationRepository;
        this.organisationDetailsService = organisationDetailsService;
        this.idamService = idamService;
        this.sideEffectService = sideEffectService;
    }

    @Transactional(readOnly = true)
    public NocQuestionsResponse questions(String caseId) {
        loadCase(caseId);

        NocFieldType textField = new NocFieldType("Text", "Text", null, null, null, List.of(), List.of(), null);

        return new NocQuestionsResponse(List.of(
            question(1, "What is the defendant's first name?", DEFENDANT_FIRST_NAME_QUESTION_ID, textField),
            question(2, "What is the defendant's last name?", DEFENDANT_LAST_NAME_QUESTION_ID, textField)
        ));
    }

    @Transactional(readOnly = true)
    public NocApprovalStatus verify(NocAnswersRequest request, String authorisation) {
        verifyInternal(request, authorisation, false);

        return approved("NoC answers verified");
    }

    @Transactional
    public NocApprovalStatus submit(NocAnswersRequest request, String authorisation) {
        VerificationResult result = verifyInternal(request, authorisation, true);

        if (result.incomingOrganisationAlreadyRepresentsParty()) {
            return approved("NoC already applied");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<PartyRepresentationEntity> activeRepresentation = activeRepresentation(result);
        Optional<String> incumbentOrgId = activeRepresentation.map(PartyRepresentationEntity::getOrganisationId);

        activeRepresentation.ifPresent(representation -> {
            representation.setStatus(PartyRepresentationStatus.ENDED);
            representation.setEndedAt(now);
        });

        partyRepresentationRepository.save(PartyRepresentationEntity.builder()
            .caseReference(result.pcsCase().getCaseReference())
            .partyId(result.matchedParty().getId())
            .partyRole(PartyRole.DEFENDANT)
            .organisationId(result.incomingOrganisation().organisationId())
            .organisationName(result.incomingOrganisation().organisationName())
            .caseRole(DEFENDANT_SOLICITOR_ROLE)
            .status(PartyRepresentationStatus.ACTIVE)
            .source(PartyRepresentationSource.NOC)
            .startedAt(now)
            .build());

        enqueueSideEffects(result, incumbentOrgId.orElse(null));

        return approved("NoC approved");
    }

    private void enqueueSideEffects(VerificationResult result, String incumbentOrgId) {
        List<OrganisationUsersResponse.ProfessionalUser> incomingUsers =
            usersForOrganisation(result.incomingOrganisation().organisationId());

        incomingUsers.forEach(user -> enqueueGrant(result, user));

        if (incumbentOrgId == null || incumbentOrgId.isBlank()) {
            enqueueAudit(result, "NoC added defendant representation with no incumbent organisation");
            return;
        }

        boolean incumbentStillRepresentsAnotherParty = partyRepresentationRepository
            .findByCaseReferenceAndOrganisationIdAndCaseRoleAndStatus(
                result.pcsCase().getCaseReference(),
                incumbentOrgId,
                DEFENDANT_SOLICITOR_ROLE,
                PartyRepresentationStatus.ACTIVE
            )
            .stream()
            .anyMatch(representation -> !result.matchedParty().getId().equals(representation.getPartyId()));

        List<OrganisationUsersResponse.ProfessionalUser> incumbentUsers = usersForOrganisation(incumbentOrgId);

        if (incumbentStillRepresentsAnotherParty) {
            incumbentUsers.forEach(user -> enqueueRetainedNotification(result, incumbentOrgId, user));
            enqueueAudit(result, "NoC changed representation; incumbent organisation retained case access");
        } else {
            incumbentUsers.forEach(user -> {
                enqueueRevoke(result, incumbentOrgId, user);
                enqueueRemovedNotification(result, incumbentOrgId, user);
            });
            enqueueAudit(result, "NoC changed representation; incumbent organisation access queued for removal");
        }
    }

    private VerificationResult verifyInternal(
        NocAnswersRequest request,
        String authorisation,
        boolean allowAlreadyRepresentingMatchedParty
    ) {
        validateAnswers(request);

        PcsCaseEntity pcsCase = loadCase(request.caseId());
        PartyEntity matchedParty = matchDefendant(pcsCase, request);
        Organisation incomingOrganisation = incomingOrganisation(authorisation);
        boolean alreadyRepresentsMatchedParty = partyRepresentationRepository
            .existsByCaseReferenceAndPartyIdAndOrganisationIdAndStatus(
                pcsCase.getCaseReference(),
                matchedParty.getId(),
                incomingOrganisation.organisationId(),
                PartyRepresentationStatus.ACTIVE
            );

        if (alreadyRepresentsMatchedParty && !allowAlreadyRepresentingMatchedParty) {
            throw new NocException(
                "requesting-org-already-represents-party",
                "The requesting organisation already represents the matched party."
            );
        }

        return new VerificationResult(
            pcsCase,
            matchedParty,
            incomingOrganisation,
            alreadyRepresentsMatchedParty
        );
    }

    private void validateAnswers(NocAnswersRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new NocException("answers-empty", "No answers were provided.");
        }
        if (request.answers().size() != 2) {
            throw new NocException(
                "answers-mismatch-questions",
                "The number of provided answers does not match the number of questions."
            );
        }
    }

    private NocQuestion question(int order, String questionText, String questionId, NocFieldType fieldType) {
        return new NocQuestion(
            CASE_TYPE_ID,
            order,
            questionText,
            fieldType,
            "1",
            "NoCChallenge",
            null,
            questionId
        );
    }

    private PcsCaseEntity loadCase(String caseId) {
        long caseReference = parseCaseReference(caseId);
        return pcsCaseRepository.findWithPartiesAndClaimPartiesByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    private long parseCaseReference(String caseId) {
        try {
            return Long.parseLong(caseId);
        } catch (NumberFormatException ex) {
            throw new NocException("case-id-invalid", "Case ID must be a 16 digit number.");
        }
    }

    private PartyEntity matchDefendant(PcsCaseEntity pcsCase, NocAnswersRequest request) {
        String firstName = answerValue(request, DEFENDANT_FIRST_NAME_QUESTION_ID);
        String lastName = answerValue(request, DEFENDANT_LAST_NAME_QUESTION_ID);

        List<PartyEntity> matches = defendants(pcsCase).stream()
            .filter(party -> normalise(firstName).equals(normalise(party.getForename())))
            .filter(party -> normalise(lastName).equals(normalise(party.getSurname())))
            .toList();

        if (matches.isEmpty()) {
            throw new NocException("answers-not-matched-any-litigant", "The answers did not match any litigant.");
        }
        if (matches.size() > 1) {
            throw new NocException(
                "answers-not-identify-litigant",
                "The answers did not identify a single represented party."
            );
        }
        return matches.getFirst();
    }

    private List<PartyEntity> defendants(PcsCaseEntity pcsCase) {
        return pcsCase.getParties().stream()
            .filter(party -> party.getClaimParties().stream()
                .filter(Objects::nonNull)
                .anyMatch(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT))
            .toList();
    }

    private String answerValue(NocAnswersRequest request, String questionId) {
        return request.answers().stream()
            .filter(answer -> questionId.equals(answer.questionId()))
            .map(NocAnswer::value)
            .map(value -> Optional.ofNullable(value).orElse(""))
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElseThrow(() -> new NocException(
                "no-answer-provided-for-question",
                "No answer has been provided for question ID '" + questionId + "'"
            ));
    }

    private Organisation incomingOrganisation(String authorisation) {
        UserInfo userInfo = idamService.validateAuthToken(authorisation).getUserDetails();
        OrganisationDetailsResponse organisationDetails = organisationDetailsService
            .getOrganisationDetails(userInfo.getUid());

        if (organisationDetails == null || organisationDetails.getOrganisationIdentifier() == null) {
            throw new NocException("requesting-user-not-in-organisation", "The requesting user is not in an org.");
        }

        return new Organisation(
            organisationDetails.getOrganisationIdentifier(),
            organisationDetails.getName()
        );
    }

    private Optional<PartyRepresentationEntity> activeRepresentation(VerificationResult result) {
        return partyRepresentationRepository.findByCaseReferenceAndPartyIdAndStatus(
            result.pcsCase().getCaseReference(),
            result.matchedParty().getId(),
            PartyRepresentationStatus.ACTIVE
        );
    }

    private List<OrganisationUsersResponse.ProfessionalUser> usersForOrganisation(String organisationId) {
        if (organisationId == null || organisationId.isBlank()) {
            return List.of();
        }
        OrganisationUsersResponse response = organisationDetailsService.getOrganisationUsers(organisationId);
        return Optional.ofNullable(response.getUsers()).orElse(Collections.emptyList());
    }

    private void enqueueGrant(VerificationResult result, OrganisationUsersResponse.ProfessionalUser user) {
        enqueueUserJob(
            result,
            NocSideEffectJobType.GRANT_CASE_ROLE,
            result.incomingOrganisation().organisationId(),
            user,
            "grant"
        );
    }

    private void enqueueRevoke(
        VerificationResult result,
        String incumbentOrgId,
        OrganisationUsersResponse.ProfessionalUser user
    ) {
        enqueueUserJob(result, NocSideEffectJobType.REVOKE_CASE_ROLE, incumbentOrgId, user, "revoke");
    }

    private void enqueueRemovedNotification(
        VerificationResult result,
        String incumbentOrgId,
        OrganisationUsersResponse.ProfessionalUser user
    ) {
        enqueueUserJob(
            result,
            NocSideEffectJobType.NOTIFY_CASE_ACCESS_REMOVED,
            incumbentOrgId,
            user,
            "notify-removed"
        );
    }

    private void enqueueRetainedNotification(
        VerificationResult result,
        String incumbentOrgId,
        OrganisationUsersResponse.ProfessionalUser user
    ) {
        enqueueUserJob(
            result,
            NocSideEffectJobType.NOTIFY_PARTY_REPRESENTATION_RETAINED,
            incumbentOrgId,
            user,
            "notify-retained"
        );
    }

    private void enqueueUserJob(
        VerificationResult result,
        NocSideEffectJobType type,
        String organisationId,
        OrganisationUsersResponse.ProfessionalUser user,
        String action
    ) {
        sideEffectService.enqueue(sideEffectService.job(
            result.pcsCase().getCaseReference(),
            result.matchedParty().getId(),
            type,
            user.getUserIdentifier(),
            organisationId,
            DEFENDANT_SOLICITOR_ROLE,
            user.getEmail(),
            null,
            idempotencyKey(result, action, organisationId, user.getUserIdentifier())
        ));
    }

    private void enqueueAudit(VerificationResult result, String detail) {
        sideEffectService.enqueue(sideEffectService.job(
            result.pcsCase().getCaseReference(),
            result.matchedParty().getId(),
            NocSideEffectJobType.AUDIT,
            null,
            result.incomingOrganisation().organisationId(),
            DEFENDANT_SOLICITOR_ROLE,
            null,
            detail,
            idempotencyKey(result, "audit", result.incomingOrganisation().organisationId(), "case")
        ));
    }

    private String idempotencyKey(VerificationResult result, String action, String organisationId, String userId) {
        return "noc:%s:%s:%s:%s:%s".formatted(
            result.pcsCase().getCaseReference(),
            result.matchedParty().getId(),
            action,
            organisationId,
            userId
        );
    }

    private NocApprovalStatus approved(String message) {
        return new NocApprovalStatus("noc-approved", message, DEFENDANT_SOLICITOR_ROLE, "APPROVED");
    }

    private String normalise(String value) {
        return Optional.ofNullable(value).orElse("").trim().toLowerCase(Locale.UK);
    }

    private record VerificationResult(
        PcsCaseEntity pcsCase,
        PartyEntity matchedParty,
        Organisation incomingOrganisation,
        boolean incomingOrganisationAlreadyRepresentsParty
    ) {
    }

    private record Organisation(
        String organisationId,
        String organisationName
    ) {
    }
}

package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkSubmitError;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimExtrasService;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimStagingSerializer;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.RoleAssignmentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.task.CaseRoleAssignmentTaskComponent;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.BulkPbaDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.service.ClaimIssueFeeScheduler;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createBulkPossessionClaim;

/**
 * Creates a case from a claim the bulk claim API has already validated and staged. Only the PCS system user can
 * run it; the staging row, not the event payload, carries the claim.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateBulkPossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    static final String SUBMISSION_ID_PATH = "bulkClaimSubmissionId";

    private final BulkClaimSubmissionRepository submissionRepository;
    private final BulkClaimStagingSerializer stagingSerializer;
    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final BulkClaimExtrasService extrasService;
    private final ClaimIssueFeeScheduler claimIssueFeeScheduler;
    private final SchedulerClient schedulerClient;
    private final SecurityContextService securityContextService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createBulkPossessionClaim.name(), this::submit)
                .initialState(State.PENDING_CASE_ISSUED)
                .name("Create bulk possession claim")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRU, UserRole.SYSTEM_USER)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);

        new PageBuilder(eventBuilder)
            .page("createBulkPossessionClaim")
            .optional(PCSCase::getBulkClaimSubmissionId, ShowConditions.NEVER_SHOW, true);
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        String submissionId = eventPayload.caseData().getBulkClaimSubmissionId();

        try {
            return createCase(caseReference, submissionId);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException
                 | DataIntegrityViolationException e) {
            log.error("Bulk claim submission {} could not be created as case {}", submissionId, caseReference, e);
            return reject(BulkFailReason.SAVE_FAILED, SUBMISSION_ID_PATH, "The claim could not be saved");
        }
    }

    private SubmitResponse<State> createCase(long caseReference, String submissionId) {
        UUID id = parseId(submissionId);
        if (id == null) {
            return reject(BulkFailReason.SAVE_FAILED, SUBMISSION_ID_PATH,
                          "A valid submission id is required");
        }

        BulkClaimSubmissionEntity submission = submissionRepository.findByIdForUpdate(id).orElse(null);
        if (submission == null) {
            return reject(BulkFailReason.SAVE_FAILED, SUBMISSION_ID_PATH, "Unknown submission");
        }
        if (submission.getStatus() != BulkClaimSubmissionStatus.PENDING || submission.getCaseReference() != null) {
            return reject(BulkFailReason.SAVE_FAILED, SUBMISSION_ID_PATH,
                          "The submission has already been processed");
        }

        PCSCase stagedCase = stagingSerializer.readCase(submission.getStagedCaseData());
        OrganisationDetailsResponse organisation = stagingSerializer.readOrganisation(
            submission.getOrganisationSnapshot());

        final PcsCaseEntity pcsCase = pcsCaseService.createCase(
            caseReference, stagedCase.getPropertyAddress(), stagedCase.getLegislativeCountry(), organisation);
        pcsCaseService.createMainClaimOnCase(caseReference, stagedCase, organisation);
        extrasService.apply(caseReference, stagingSerializer.readExtras(submission.getStagedExtras()));

        claimIssueFeeScheduler.scheduleCaseIssueFeePayment(
            caseReference,
            partyService.getPrimaryClaimantPartyEntity(pcsCaseService.loadCase(caseReference)),
            new BulkPbaDetails(submission.getPbaAccountNumber(), submission.getPaymentCustomerReference(),
                               organisation.getName(), submission.getId()));
        scheduleCreatorRevoke(caseReference);

        submission.setStatus(BulkClaimSubmissionStatus.CASE_CREATED);
        submission.setStatusCode(BulkFailReason.CLAIM_SUCCESS.getStatusCode());
        submission.setFailReasonCode(null);
        submission.setFailReason(null);
        submission.setValidationErrors(null);
        submission.setCaseReference(caseReference);
        submission.setPcsCase(pcsCase);
        submissionRepository.save(submission);

        return SubmitResponse.<State>builder().state(State.PENDING_CASE_ISSUED).build();
    }

    private SubmitResponse<State> reject(BulkFailReason reason, String path, String message) {
        String error = new BulkSubmitError(reason.getFailReasonCode(), path, message).format();
        return SubmitResponse.<State>builder().errors(List.of(error)).build();
    }

    private void scheduleCreatorRevoke(long caseReference) {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        RoleAssignmentTaskData taskData = RoleAssignmentTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .userId(userId)
            .build();

        schedulerClient.scheduleIfNotExists(
            CaseRoleAssignmentTaskComponent.ROLE_ASSIGNMENT_TASK_DESCRIPTOR
                .instance(UUID.randomUUID().toString())
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }

    private String currentUserId() {
        try {
            return securityContextService.getCurrentUserDetails().getUid();
        } catch (SecurityContextException e) {
            log.warn("No current user to revoke the creator role from; skipping");
            return null;
        }
    }

    private static UUID parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

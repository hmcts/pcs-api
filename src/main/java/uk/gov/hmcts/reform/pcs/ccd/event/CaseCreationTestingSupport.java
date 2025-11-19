package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PostcodeNotAssignedToCourt;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.TestingSupportService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationNameService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_FURTHER_CLAIM_DETAILS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR;

@Component
@Slf4j
@AllArgsConstructor
public class CaseCreationTestingSupport implements CCDConfig<PCSCase, State, UserRole> {

    private final SecurityContextService securityContextService;
    private final OrganisationNameService organisationNameService;
    private final SchedulerClient schedulerClient;
    private final PcsCaseService pcsCaseService;
    private final TestingSupportService testingSupportService;
    private final DraftCaseDataService draftCaseDataService;
    private final PartyService partyService;
    private final ClaimService claimService;

    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    private final PropertyNotEligible propertyNotEligible;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestCase.name(), this::submit, this::start)
                .initialState(AWAITING_FURTHER_CLAIM_DETAILS)
                .showSummary()
                .name("DA & QA - Test Case Creation")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(new StartTheService())
            .add(enterPropertyAddress)
            .add(crossBorderPostcodeSelection)
            .add(propertyNotEligible)
            .add(new PostcodeNotAssignedToCourt());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setFeeAmount("123.45");
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {

        PCSCase pcsCase = eventPayload.caseData();
        testingSupportService.generateTestPCSCase(pcsCase);
        long caseReference = eventPayload.caseReference();

        String userEmail = securityContextService.getCurrentUserDetails().getSub();
        String organisationName = organisationNameService.getOrganisationNameForCurrentUser();
        if (organisationName != null) {
            pcsCase.setOrganisationName(organisationName);
        } else {
            pcsCase.setOrganisationName(userEmail);
        }
        pcsCase.setClaimantContactEmail(userEmail);
        pcsCaseService.createCase(caseReference, pcsCase.getPropertyAddress(), pcsCase.getLegislativeCountry());
        draftCaseDataService.patchUnsubmittedCaseData(caseReference, pcsCase);

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        pcsCaseService.mergeCaseData(pcsCaseEntity, pcsCase);

        PartyEntity claimantPartyEntity = createClaimantPartyEntity(pcsCase);
        pcsCaseEntity.addParty(claimantPartyEntity);

        ClaimEntity claimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);
        pcsCaseEntity.addClaim(claimEntity);

        pcsCaseService.save(pcsCaseEntity);

        draftCaseDataService.deleteUnsubmittedCaseData(caseReference);

        scheduleCaseIssuedFeeTask(caseReference, pcsCase.getOrganisationName());

        return SubmitResponse.defaultResponse();
    }

    private PartyEntity createClaimantPartyEntity(PCSCase pcsCase) {
        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        UUID userID = UUID.fromString(userDetails.getUid());

        String claimantName = isNotBlank(pcsCase.getOverriddenClaimantName())
            ? pcsCase.getOverriddenClaimantName() : pcsCase.getClaimantName();

        AddressUK contactAddress = pcsCase.getOverriddenClaimantContactAddress() != null
            ? pcsCase.getOverriddenClaimantContactAddress() : pcsCase.getPropertyAddress();

        String contactEmail = isNotBlank(pcsCase.getOverriddenClaimantContactEmail())
            ? pcsCase.getOverriddenClaimantContactEmail() : pcsCase.getClaimantContactEmail();

        return partyService.createPartyEntity(
            userID,
            claimantName,
            null,
            contactEmail,
            contactAddress,
            pcsCase.getClaimantContactPhoneNumber()
        );
    }

    private static final String CASE_ISSUED_FEE_TYPE = "caseIssueFee";

    private void scheduleCaseIssuedFeeTask(long caseReference, String responsibleParty) {
        String taskId = UUID.randomUUID().toString();

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeType(CASE_ISSUED_FEE_TYPE)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(String.valueOf(caseReference))
            .responsibleParty(responsibleParty)
            .build();

        schedulerClient.scheduleIfNotExists(
            FEE_CASE_ISSUED_TASK_DESCRIPTOR
                .instance(taskId)
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }

}

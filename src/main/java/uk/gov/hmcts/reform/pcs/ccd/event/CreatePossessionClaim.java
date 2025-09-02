package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.SelectClaimType;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.SelectLegislativeCountry;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.GroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PreActionProtocol;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.payment.fee.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.fee.service.FeeService;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.model.ServiceRequestResponse;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.service.ServiceRequestService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;


@Slf4j
@Component
@AllArgsConstructor
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;
    private final FeeService feeService;
    private final ServiceRequestService serviceRequestService;
    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    private final PropertyNotEligible propertyNotEligible;


    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createPossessionClaim.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new StartTheService())
            .add(enterPropertyAddress)
            .add(crossBorderPostcodeSelection)
            .add(propertyNotEligible)
            .add(new SelectLegislativeCountry())
            .add(new SelectClaimantType())
            .add(new ClaimantTypeNotEligibleEngland())
            .add(new ClaimantTypeNotEligibleWales())
            .add(new SelectClaimType())
            .add(new ClaimTypeNotEligibleEngland())
            .add(new ClaimTypeNotEligibleWales())
            .add(new ClaimantInformation())
            .add(new ContactPreferences())
            .add(new GroundsForPossession())
            .add(new PreActionProtocol())
            .add(new MediationAndSettlement())
            .add(new CheckingNotice())
            .add(new NoticeDetails())
            .add(new RentDetails());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        String userDetails = securityContextService.getCurrentUserDetails().getSub();
        caseData.setClaimantName(userDetails);
        caseData.setClaimantContactEmail(userDetails);

        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);
        pcsCase.setClaimantContactAddress(pcsCase.getPropertyAddress());

        UUID userID = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        String claimantName = isNotBlank(pcsCase.getOverriddenClaimantName())
            ? pcsCase.getOverriddenClaimantName() : pcsCase.getClaimantName();

        AddressUK contactAddress = pcsCase.getOverriddenClaimantContactAddress() != null
            ? pcsCase.getOverriddenClaimantContactAddress() : pcsCase.getClaimantContactAddress();

        String contactEmail = isNotBlank(pcsCase.getOverriddenClaimantContactEmail())
            ? pcsCase.getOverriddenClaimantContactEmail() : pcsCase.getClaimantContactEmail();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.createCase(caseReference, pcsCase);
        PartyEntity party = partyService.createAndLinkParty(
            pcsCaseEntity,
            userID,
            claimantName,
            null,
            contactEmail,
            contactAddress,
            pcsCase.getClaimantContactPhoneNumber(),
            true);

        ClaimEntity claimEntity = claimService.createAndLinkClaim(
            pcsCaseEntity,
            party,
            "Main Claim",
            PartyRole.CLAIMANT);

        claimService.saveClaim(claimEntity);

        try {
            log.info("Starting fee lookup and service request creation for case: {}", caseReference);

            // Step 1: Get the fee
            Fee fee = feeService.getFeeWithoutHearing();
            log.info("Fee retrieved successfully - code: {}, amount: {}", fee.getCode(), fee.getCalculatedAmount());

            // Step 2: Create service request with the fee
            ServiceRequestResponse serviceRequestResponse = serviceRequestService.createServiceRequest(
                String.valueOf(caseReference), // caseReference
                String.valueOf(caseReference), // ccdCaseNumber (using same as case reference)
                fee
            );

            log.info("Service request created successfully: {}", serviceRequestResponse.getServiceRequestReference());

        } catch (Exception e) {
            log.error("Failed to create fee lookup or service request for case {}: {}", caseReference,
                        e.getMessage(), e);
            log.warn("Case {} was created successfully but payment setup failed", caseReference);
        }
    }

}

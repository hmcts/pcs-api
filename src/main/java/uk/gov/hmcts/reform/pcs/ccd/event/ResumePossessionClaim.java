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
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.DailyRentAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.GroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.NoRentArrearsGroundsForPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.NoRentArrearsGroundsForPossessionReason;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PreActionProtocol;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.RentArrears;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.SelectClaimType;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.GroundForPossessionRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.GroundForPossessionAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimGroundService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_FURTHER_CLAIM_DETAILS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;


@Slf4j
@Component
@AllArgsConstructor
public class ResumePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;
    private final ClaimGroundService claimGroundService;
    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final ResumeClaim resumeClaim;
    private final UnsubmittedCaseDataService unsubmittedCaseDataService;
    private final NoticeDetails noticeDetails;

    private final TenancyLicenceDetails tenancyLicenceDetails;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(resumePossessionClaim.name(), this::submit, this::start)
                .forStateTransition(AWAITING_FURTHER_CLAIM_DETAILS, AWAITING_SUBMISSION_TO_HMCTS)
                .name("Make a claim")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .showSummary();

        savingPageBuilderFactory.create(eventBuilder)
            .add(resumeClaim)
            .add(new SelectClaimantType())
            .add(new ClaimantTypeNotEligibleEngland())
            .add(new ClaimantTypeNotEligibleWales())
            .add(new SelectClaimType())
            .add(new ClaimTypeNotEligibleEngland())
            .add(new ClaimTypeNotEligibleWales())
            .add(new ClaimantInformation())
            .add(new ContactPreferences())
            .add(new DefendantsDetails())
            .add(tenancyLicenceDetails)
            .add(new GroundsForPossession())
            .add(new GroundForPossessionRentArrears())
            .add(new GroundForPossessionAdditionalGrounds())
            .add(new NoRentArrearsGroundsForPossessionOptions())
            .add(new NoRentArrearsGroundsForPossessionReason())
            .add(new PreActionProtocol())
            .add(new MediationAndSettlement())
            .add(new CheckingNotice())
            .add(noticeDetails)
            .add(new RentDetails())
            .add(new DailyRentAmount())
            .add(new RentArrears());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        String userDetails = securityContextService.getCurrentUserDetails().getSub();
        caseData.setClaimantName(userDetails);
        caseData.setClaimantContactEmail(userDetails);

        AddressUK propertyAddress = caseData.getPropertyAddress();
        if (propertyAddress == null) {
            throw new IllegalStateException("Cannot resume claim without property address already set");
        }

        LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();
        if (legislativeCountry == null) {
            throw new IllegalStateException("Cannot resume claim without legislative country already set");
        }

        List<DynamicStringListElement> listItems = Arrays.stream(ClaimantType.values())
            .filter(value -> value.isApplicableFor(legislativeCountry))
            .map(value -> DynamicStringListElement.builder().code(value.name()).label(value.getLabel())
                .build())
            .toList();

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .listItems(listItems)
            .build();
        caseData.setClaimantType(claimantTypeList);

        String formattedAddress = String.format(
            "%s<br>%s<br>%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );
        caseData.setFormattedClaimantContactAddress(formattedAddress);

        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);

        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        UUID userID = UUID.fromString(userDetails.getUid());

        String claimantName = isNotBlank(pcsCase.getOverriddenClaimantName())
            ? pcsCase.getOverriddenClaimantName() : pcsCase.getClaimantName();

        AddressUK contactAddress = pcsCase.getOverriddenClaimantContactAddress() != null
            ? pcsCase.getOverriddenClaimantContactAddress() : pcsCase.getPropertyAddress();

        String contactEmail = isNotBlank(pcsCase.getOverriddenClaimantContactEmail())
            ? pcsCase.getOverriddenClaimantContactEmail() : pcsCase.getClaimantContactEmail();

        List<ListValue<DefendantDetails>> defendantsList = new ArrayList<>();
        if (pcsCase.getDefendant1() != null) {
            if (VerticalYesNo.YES == pcsCase.getDefendant1().getAddressSameAsPossession()) {
                pcsCase.getDefendant1().setCorrespondenceAddress(pcsCase.getPropertyAddress());
            }
            defendantsList.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant1()));
            pcsCaseService.clearHiddenDefendantDetailsFields(defendantsList);
            pcsCase.setDefendants(defendantsList);
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.patchCase(caseReference, pcsCase);
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

        List<ClaimGroundEntity> claimGroundEntities =
            claimGroundService.getGroundsWithReason(pcsCase);

        claimEntity.addClaimGroundEntities(claimGroundEntities);

        claimService.saveClaim(claimEntity);

        unsubmittedCaseDataService.deleteUnsubmittedCaseData(caseReference);
    }
}

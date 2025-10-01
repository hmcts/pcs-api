package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.PCSCaseEntity;
import uk.gov.hmcts.reform.pcs.entity.Party;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.service.CcdMoneyFieldFormatter;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class CreateClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PartyService partyService;
    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimRepository claimRepository;
    private final PartyRepository partyRepository;
    private final CcdMoneyFieldFormatter ccdMoneyFieldFormatter;

    public CreateClaim(PartyService partyService,
                       PcsCaseRepository pcsCaseRepository,
                       ClaimRepository claimRepository,
                       PartyRepository partyRepository,
                       CcdMoneyFieldFormatter ccdMoneyFieldFormatter) {

        this.partyService = partyService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.claimRepository = claimRepository;
        this.partyRepository = partyRepository;
        this.ccdMoneyFieldFormatter = ccdMoneyFieldFormatter;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.createClaim.name(), this::submit)
            .forAllStates()
            .name("Create claim")
            .showSummary()
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
            .fields()
            .page("summary")
            .mandatory(PCSCase::getClaimToAdd)
            .page("claimants", this::claimantsSelected)
            .mandatory(PCSCase::getClaimantsToAdd)
            .page("defendants", this::defendantsSelected)
            .mandatory(PCSCase::getDefendantsToAdd)
            .page("interested-parties")
            .showCondition("ipEmpty=\"No\"")
            .optional(PCSCase::getInterestedPartiesToAdd)
            .readonly(PCSCase::getIpEmpty, NEVER_SHOW)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> start(CaseDetails<PCSCase, State> caseDetails) {
        PCSCase pcsCase = caseDetails.getData();

        List<DynamicListElement> claimantsOptionsList = partyService.getAllPartiesAsOptionsList(caseDetails.getId());

        DynamicMultiSelectList dynamicList = DynamicMultiSelectList.builder()
            .listItems(claimantsOptionsList)
            .build();
        pcsCase.setClaimantsToAdd(dynamicList);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> claimantsSelected(CaseDetails<PCSCase, State> details,
                                                                           CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase pcsCase = details.getData();

        DynamicMultiSelectList claimantsMultiSelectList = pcsCase.getClaimantsToAdd();
        List<DynamicListElement> selectedClaimants = claimantsMultiSelectList.getValue();
        List<DynamicListElement> defendantOptionsList = new ArrayList<>(claimantsMultiSelectList.getListItems());

        defendantOptionsList.removeAll(selectedClaimants);

        DynamicMultiSelectList defendantsMultiSelectList = DynamicMultiSelectList.builder()
            .listItems(defendantOptionsList)
            .build();

        pcsCase.setDefendantsToAdd(defendantsMultiSelectList);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> defendantsSelected(CaseDetails<PCSCase, State> details,
                                                                            CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase pcsCase = details.getData();

        DynamicMultiSelectList defendantsMultiSelectList = pcsCase.getDefendantsToAdd();
        List<DynamicListElement> selectedDefendants = defendantsMultiSelectList.getValue();
        List<DynamicListElement> interestedPartiesOptionsList
            = new ArrayList<>(defendantsMultiSelectList.getListItems());

        interestedPartiesOptionsList.removeAll(selectedDefendants);

        DynamicMultiSelectList interestedPartiesMultiSelectList = DynamicMultiSelectList.builder()
            .listItems(interestedPartiesOptionsList)
            .build();

        pcsCase.setInterestedPartiesToAdd(interestedPartiesMultiSelectList);
        pcsCase.setIpEmpty(YesOrNo.from(interestedPartiesOptionsList.isEmpty()));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        Claim claimToAdd = pcsCase.getClaimToAdd();

        uk.gov.hmcts.reform.pcs.entity.Claim claimEntity = uk.gov.hmcts.reform.pcs.entity.Claim.builder()
            .summary(claimToAdd.getSummary())
            .amount(ccdMoneyFieldFormatter.parsePenceString(claimToAdd.getAmountInPence()))
            .build();

        List<UUID> claimantUuids = getSelectedItemCodes(pcsCase.getClaimantsToAdd());
        List<UUID> defendantUuids = getSelectedItemCodes(pcsCase.getDefendantsToAdd());
        List<UUID> interestedPartyUuids = getSelectedItemCodes(pcsCase.getInterestedPartiesToAdd());

        // TODO: Optimise this DB update
        claimantUuids.forEach(
            claimantUuid -> {
                Party partyReference = partyRepository.getReferenceById(claimantUuid);
                claimEntity.addParty(partyReference, PartyRole.CLAIMANT);
            }
        );

        defendantUuids.forEach(
            defendantUuid -> {
                Party partyReference = partyRepository.getReferenceById(defendantUuid);
                claimEntity.addParty(partyReference, PartyRole.DEFENDANT);
            }
        );

        interestedPartyUuids.forEach(
            claimantUuid -> {
                Party interestedPartyUuid = partyRepository.getReferenceById(claimantUuid);
                claimEntity.addParty(interestedPartyUuid, PartyRole.INTERESTED_PARTY);
            }
        );

        long caseReference = eventPayload.caseReference();
        PCSCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException("Case not found for " + caseReference));

        pcsCaseEntity.addClaim(claimEntity);
        claimRepository.save(claimEntity);
    }

    @NonNull
    private static List<UUID> getSelectedItemCodes(DynamicMultiSelectList multiSelectList) {
        if (multiSelectList != null) {
            return multiSelectList.getValue().stream()
                .map(DynamicListElement::getCode)
                .toList();
        } else {
            return List.of();
        }
    }

}

package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.service.ClaimService;
import uk.gov.hmcts.reform.pcs.service.PartyService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.MultiSelectListUtils.getSelectedCodes;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
public class AddClaimantToClaim implements CCDConfig<PcsCase, State, UserRole> {

    private final PartyService partyService;
    private final ClaimService claimService;
    private final ClaimRepository claimRepository;

    public AddClaimantToClaim(PartyService partyService,
                              ClaimService claimService,
                              ClaimRepository claimRepository) {

        this.partyService = partyService;
        this.claimService = claimService;
        this.claimRepository = claimRepository;
    }

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EventId.addClaimantToClaim.name(), this::submit, this::start)
            .forAllStates()
            .name("Add claimant")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .showCondition(NEVER_SHOW)
            .fields()
            .page("claimants")
            .mandatory(PcsCase::getClaimantsToAdd)
            .label("no-claimants-label", "### There are no unassigned parties for this claim", "partyListEmpty=\"Yes\"")
            .readonly(PcsCase::getCurrentClaimId, NEVER_SHOW)
            .readonly(PcsCase::getPartyListEmpty, NEVER_SHOW)
            .done();
    }

    private PcsCase start(EventPayload<PcsCase, State> payload) {
        PcsCase pcsCase = payload.caseData();

        String claimId = payload.urlParams().getFirst("claimId");

        uk.gov.hmcts.reform.pcs.entity.Claim claimEntity = claimRepository.findById(UUID.fromString(claimId))
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found for ID " + claimId));

        Set<UUID> existingClaimPartyIds = claimEntity.getClaimParties().stream()
            .map(claimParty -> claimParty.getParty().getId())
            .collect(Collectors.toSet());

        List<DynamicListElement> claimantsOptionsList = partyService
            .getAllPartiesAsOptionsList(payload.caseReference())
            .stream()
            .filter(listElement -> !existingClaimPartyIds.contains(listElement.getCode()))
            .toList();

        DynamicMultiSelectList dynamicList = DynamicMultiSelectList.builder()
            .listItems(claimantsOptionsList)
            .build();

        pcsCase.setCurrentClaimId(claimId);
        pcsCase.setClaimantsToAdd(dynamicList);
        pcsCase.setPartyListEmpty(YesOrNo.from(claimantsOptionsList.isEmpty()));

        return pcsCase;
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();

        UUID currentClaimId = UUID.fromString(pcsCase.getCurrentClaimId());
        List<UUID> partiesIdsToAdd = getSelectedCodes(pcsCase.getClaimantsToAdd());

        claimService.linkClaimants(currentClaimId, partiesIdsToAdd);
    }

}

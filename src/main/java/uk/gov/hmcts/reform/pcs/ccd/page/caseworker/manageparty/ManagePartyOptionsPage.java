package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.LAST_PARTY_ERROR;

@Component
@RequiredArgsConstructor
public class ManagePartyOptionsPage implements CcdPageConfiguration {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String MANAGE_PARTY_OPTIONS_FIELD = "addParty_ManagePartyOptions";
    private static final String REMOVE_PARTY_CONDITION =
        ShowConditions.fieldEquals(MANAGE_PARTY_OPTIONS_FIELD, ManagePartyOptions.REMOVE_PARTY);
    private static final String DATE_OF_BIRTH_UNKNOWN = "(Date of birth unknown)";
    private static final String ADDRESS_UNKNOWN = "(Address unknown)";
    private static final String CAN_SELECT_PARTY_CONDITION =
        ShowConditions.and(REMOVE_PARTY_CONDITION, "removeParty_CanSelectParty=\"Yes\"");
    private static final String CANNOT_SELECT_PARTY_CONDITION =
        ShowConditions.and(REMOVE_PARTY_CONDITION, "removeParty_CanSelectParty=\"No\"");

    private final PartyService partyService;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final PcsCaseService pcsCaseService;
    private final RemovePartyService removePartyService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("managePartyOptions", this::midEvent)
            .pageLabel("Update, add or remove")
            .label("managePartyOptions-separator", "---")
            .complex(PCSCase::getAddPartyDetails)
                .mandatory(AddPartyDetails::getManagePartyOptions)
                .mandatory(
                    AddPartyDetails::getAddPartyType,
                    ShowConditions.fieldEquals(MANAGE_PARTY_OPTIONS_FIELD, ManagePartyOptions.ADD_PARTY))
            .done()
            .complex(PCSCase::getRemovePartyDetails)
                .readonly(RemovePartyDetails::getCanSelectParty, ShowConditions.NEVER_SHOW, true)
                .mandatoryWithoutDefaultValue(
                    RemovePartyDetails::getPartyToRemove,
                    CAN_SELECT_PARTY_CONDITION,
                    "Which party are you removing?")
                .readonly(RemovePartyDetails::getLastPartyMessage, CANNOT_SELECT_PARTY_CONDITION, true)
                .readonlyNoSummary(RemovePartyDetails::getUnremovablePartyList, CAN_SELECT_PARTY_CONDITION)
            .done()
            .complex(PCSCase::getUpdatePartyDetails)
                .mandatory(
                    UpdatePartyDetails::getPartyToUpdate,
                    ShowConditions.fieldEquals(MANAGE_PARTY_OPTIONS_FIELD, ManagePartyOptions.UPDATE))
                .readonly(UpdatePartyDetails::getPreviouslySelectedPartyId, ShowConditions.NEVER_SHOW, true)
                .readonly(UpdatePartyDetails::getPartyType, ShowConditions.NEVER_SHOW, true)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (caseData.getAddPartyDetails().getManagePartyOptions() == ManagePartyOptions.UPDATE) {
            prepopulateForUpdate(caseData.getUpdatePartyDetails(), details.getId());
        } else if (caseData.getAddPartyDetails().getManagePartyOptions() == ManagePartyOptions.REMOVE_PARTY) {
            Optional<String> validationError = prepopulateForRemove(caseData.getRemovePartyDetails(), details.getId());
            if (validationError.isPresent()) {
                return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .data(caseData)
                    .errorMessageOverride(validationError.get())
                    .build();
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).build();
    }

    private void prepopulateForUpdate(UpdatePartyDetails updatePartyDetails, long caseReference) {
        UUID partyId = updatePartyDetails.getPartyToUpdate().getValueCode();
        PartyEntity partyEntity = partyService.getPartyEntityById(partyId, caseReference);

        // Same party re-selected, preserve the in-progress edits.
        if (partyId.toString().equals(updatePartyDetails.getPreviouslySelectedPartyId())) {
            return;
        }

        PartyRole role = partyService.getPartyRole(partyEntity);

        PartyType partyType = switch (role) {
            case CLAIMANT -> PartyType.CLAIMANT;
            case DEFENDANT -> PartyType.DEFENDANT;
            default -> null;
        };
        updatePartyDetails.setPartyType(partyType);
        updatePartyDetails.setAddress(clearPreviousAddress(partyEntity.getAddress()));
        updatePartyDetails.setEmail(blankIfNull(partyEntity.getEmailAddress()));
        updatePartyDetails.setPhoneNumber(blankIfNull(partyEntity.getPhoneNumber()));
        updatePartyDetails.setDateOfBirth(Optional.ofNullable(partyEntity.getDateOfBirth()));
        updatePartyDetails.setPreviouslySelectedPartyId(partyId.toString());
    }

    /**
     * Clears a stale address left over from the previously selected party's prepopulation by
     * setting the fields to empty strings if the current party's address isn't known.
     */
    private AddressUK clearPreviousAddress(AddressEntity addressEntity) {
        AddressUK address = addressEntity != null
            ? addressMapper.toAddressUK(addressEntity)
            : AddressUK.builder().build();

        return AddressUK.builder()
            .addressLine1(blankIfNull(address.getAddressLine1()))
            .addressLine2(blankIfNull(address.getAddressLine2()))
            .addressLine3(blankIfNull(address.getAddressLine3()))
            .postTown(blankIfNull(address.getPostTown()))
            .county(blankIfNull(address.getCounty()))
            .country(blankIfNull(address.getCountry()))
            .postCode(blankIfNull(address.getPostCode()))
            .build();
    }

    private String blankIfNull(String value) {
        return value != null ? value : "";
    }

    private Optional<String> prepopulateForRemove(RemovePartyDetails removePartyDetails, long caseReference) {
        if (removePartyDetails == null
            || removePartyDetails.getPartyToRemove() == null
            || removePartyDetails.getPartyToRemove().getValueCode() == null) {
            return Optional.of(hasAnyRemovableParty(caseReference)
                ? "Which party are you removing? is required"
                : LAST_PARTY_ERROR);
        }

        UUID partyId = removePartyDetails.getPartyToRemove().getValueCode();
        PartyEntity partyEntity = partyService.getPartyEntityById(partyId, caseReference);

        try {
            removePartyService.validateCanRemove(partyEntity, caseReference);
        } catch (IllegalStateException exception) {
            return Optional.of(exception.getMessage());
        }

        PartyRole role = partyService.getPartyRole(partyEntity);
        ClaimEntity mainClaim = partyEntity.getPcsCase().getClaims().getFirst();

        removePartyDetails.setSelectedPartyLabel("%s - %s".formatted(
            partyService.getPartyName(partyEntity),
            partyService.getPartyLabel(mainClaim, partyId)
        ));
        removePartyDetails.setPartyType(role == PartyRole.CLAIMANT ? PartyType.CLAIMANT : PartyType.DEFENDANT);
        removePartyDetails.setDateOfBirth(partyEntity.getDateOfBirth() != null
            ? partyEntity.getDateOfBirth().format(DATE_FORMATTER)
            : DATE_OF_BIRTH_UNKNOWN);
        removePartyDetails.setAddress(formatAddress(partyEntity.getAddress()));
        removePartyDetails.setRemoveSelectedParty(null);

        return Optional.empty();
    }

    private boolean hasAnyRemovableParty(long caseReference) {
        ClaimEntity mainClaim = pcsCaseService.loadCase(caseReference).getClaims().getFirst();
        return mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT
                || claimParty.getRole() == PartyRole.DEFENDANT)
            .filter(claimParty -> partyService.isActive(claimParty.getParty()))
            .anyMatch(claimParty -> removePartyService.canSelectForRemoval(claimParty, mainClaim));
    }

    private String formatAddress(AddressEntity addressEntity) {
        if (addressEntity == null) {
            return ADDRESS_UNKNOWN;
        }

        String address = addressFormatter.formatFullAddress(addressMapper.toAddressUK(addressEntity), "\n");
        return isNotBlank(address) ? address : ADDRESS_UNKNOWN;
    }
}

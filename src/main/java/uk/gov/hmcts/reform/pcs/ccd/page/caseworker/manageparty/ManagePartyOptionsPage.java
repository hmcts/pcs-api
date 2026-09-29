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
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ManagePartyOptionsPage implements CcdPageConfiguration {

    private final PartyService partyService;
    private final AddressMapper addressMapper;

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
                    ShowConditions.fieldEquals("addParty_ManagePartyOptions", ManagePartyOptions.ADD_PARTY))
            .done()
            .complex(PCSCase::getUpdatePartyDetails)
                .mandatory(
                    UpdatePartyDetails::getPartyToUpdate,
                    ShowConditions.fieldEquals("addParty_ManagePartyOptions", ManagePartyOptions.UPDATE))
                .readonly(UpdatePartyDetails::getPreviouslySelectedPartyId, ShowConditions.NEVER_SHOW, true)
                .readonly(UpdatePartyDetails::getPartyType, ShowConditions.NEVER_SHOW, true)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (caseData.getAddPartyDetails().getManagePartyOptions() == ManagePartyOptions.UPDATE) {
            prepopulateForUpdate(caseData.getUpdatePartyDetails(), details.getId());
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
}

package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@AllArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final ModelMapper modelMapper;

    // TODO: Test and maybe refactor (HDPI-3220)
    public void createAllParties(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity) {
        PartyEntity claimant = createClaimant(pcsCase);
        pcsCaseEntity.addParty(claimant);
        claimEntity.addParty(claimant, PartyRole.CLAIMANT);

        List<PartyEntity> defendants = createDefendants(pcsCase);
        defendants.forEach(
            defendant -> {
                pcsCaseEntity.addParty(defendant);
                claimEntity.addParty(defendant, PartyRole.DEFENDANT);
            }
        );

        List<PartyEntity> underlesseeOrMortgagees = createUnderlesseeOrMortgagees(pcsCase);
        underlesseeOrMortgagees.forEach(
            underlesseeOrMortgagee -> {
                pcsCaseEntity.addParty(underlesseeOrMortgagee);
                claimEntity.addParty(underlesseeOrMortgagee, PartyRole.UNDERLESSEE_OR_MORTGAGEE);
            }
        );
    }

    private PartyEntity createClaimant(PCSCase pcsCase) {
        PartyEntity claimantParty = new PartyEntity();

        ClaimantInformation claimantInformation = pcsCase.getClaimantInformation();
        VerticalYesNo claimantNameCorrect = claimantInformation.getIsClaimantNameCorrect();
        if (claimantNameCorrect == VerticalYesNo.YES) {
            claimantParty.setNameOverridden(YesOrNo.NO);
            claimantParty.setOrgName(claimantInformation.getClaimantName());
        } else {
            claimantParty.setNameOverridden(YesOrNo.YES);
            claimantParty.setOrgName(claimantInformation.getOverriddenClaimantName());
        }

        ClaimantContactPreferences contactPreferencesDetails = pcsCase.getContactPreferencesDetails();
        AddressUK contactAddress = contactPreferencesDetails.getOverriddenClaimantContactAddress() != null
            ? contactPreferencesDetails.getOverriddenClaimantContactAddress() : pcsCase.getPropertyAddress();

        claimantParty.setAddress(mapAddress(contactAddress));

        String contactEmail = isNotBlank(contactPreferencesDetails.getOverriddenClaimantContactEmail())
            ? contactPreferencesDetails.getOverriddenClaimantContactEmail()
            : contactPreferencesDetails.getClaimantContactEmail();

        claimantParty.setEmailAddress(contactEmail);

        VerticalYesNo phoneNumberProvided = contactPreferencesDetails.getClaimantProvidePhoneNumber();

        claimantParty.setPhoneNumberProvided(phoneNumberProvided);
        if (phoneNumberProvided == VerticalYesNo.YES) {
            claimantParty.setPhoneNumber(contactPreferencesDetails.getClaimantContactPhoneNumber());
        }

        partyRepository.save(claimantParty);

        return claimantParty;
    }

    private List<PartyEntity> createDefendants(PCSCase pcsCase) {
        Objects.requireNonNull(pcsCase.getDefendant1(), "Defendant 1 must be provided");
        createDefendant(pcsCase.getDefendant1());

        List<PartyEntity> allDefendants = new ArrayList<>();
        allDefendants.add(createDefendant(pcsCase.getDefendant1()));

        if (pcsCase.getAddAnotherDefendant() == VerticalYesNo.YES) {
            pcsCase.getAdditionalDefendants().stream()
                .map(ListValue::getValue)
                .map(this::createDefendant)
                .forEach(allDefendants::add);
        }

        return allDefendants;
    }

    private PartyEntity createDefendant(DefendantDetails defendantDetails) {
        PartyEntity defendantEntity = new PartyEntity();

        VerticalYesNo nameKnown = defendantDetails.getNameKnown();
        defendantEntity.setNameKnown(nameKnown);
        if (nameKnown == VerticalYesNo.YES) {
            defendantEntity.setFirstName(defendantDetails.getFirstName());
            defendantEntity.setLastName(defendantDetails.getLastName());
        }

        boolean addressKnown = defendantDetails.getAddressKnown().toBoolean();
        defendantEntity.setAddressKnown(nameKnown);
        if (addressKnown) {
            VerticalYesNo addressSameAsPossession = defendantDetails.getAddressSameAsPossession();
            defendantEntity.setAddressSameAsProperty(addressSameAsPossession);
            if (addressSameAsPossession == VerticalYesNo.NO) {
                defendantEntity.setAddress(mapAddress(defendantDetails.getCorrespondenceAddress()));
            }
        }

        return defendantEntity;
    }

    private List<PartyEntity> createUnderlesseeOrMortgagees(PCSCase pcsCase) {
        if (pcsCase.getHasUnderlesseeOrMortgagee() == VerticalYesNo.NO) {
            return List.of();
        }

        List<PartyEntity> allUnderlesseeOrMortgagees = new ArrayList<>();
        allUnderlesseeOrMortgagees.add(createUnderlesseeOrMortgagee(pcsCase.getUnderlesseeOrMortgagee1()));

        Objects.requireNonNull(pcsCase.getUnderlesseeOrMortgagee1(), "Underlessee or mortgagee 1 must be provided");
        createUnderlesseeOrMortgagee(pcsCase.getUnderlesseeOrMortgagee1());

        if (pcsCase.getAddAdditionalUnderlesseeOrMortgagee() == VerticalYesNo.YES) {
            pcsCase.getAdditionalUnderlesseeOrMortgagee().stream()
                .map(ListValue::getValue)
                .map(this::createUnderlesseeOrMortgagee)
                .forEach(allUnderlesseeOrMortgagees::add);
        }

        return allUnderlesseeOrMortgagees;
    }

    private PartyEntity createUnderlesseeOrMortgagee(UnderlesseeMortgageeDetails underlesseeMortgageeDetails) {

        PartyEntity underlesseeMortgageeEntity = new PartyEntity();

        VerticalYesNo nameKnown = underlesseeMortgageeDetails.getNameKnown();
        underlesseeMortgageeEntity.setNameKnown(nameKnown);
        if (nameKnown == VerticalYesNo.YES) {
            underlesseeMortgageeEntity.setOrgName(underlesseeMortgageeDetails.getName());
        }

        VerticalYesNo addressKnown = underlesseeMortgageeDetails.getAddressKnown();
        underlesseeMortgageeEntity.setAddressKnown(nameKnown);
        if (addressKnown == VerticalYesNo.YES) {
            underlesseeMortgageeEntity
                .setAddress(mapAddress(underlesseeMortgageeDetails.getAddress()));
        }

        return underlesseeMortgageeEntity;
    }

    private AddressEntity mapAddress(AddressUK address) {
        return address != null
            ? modelMapper.map(address, AddressEntity.class) : null;
    }

}

package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@AllArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final AddressMapper addressMapper;

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

        ClaimantInformation claimantInformation = pcsCase.getClaimantInformation();
        Objects.requireNonNull(claimantInformation, "Claimant must be provided");

        PartyEntity claimantParty = new PartyEntity();

        setClaimantOrgName(claimantInformation, claimantParty);

        ClaimantContactPreferences claimantContactPreferences = pcsCase.getClaimantContactPreferences();
        AddressUK contactAddress = resolveContactAddress(claimantContactPreferences);

        claimantParty.setAddress(mapAddress(contactAddress));

        String contactEmail = isNotBlank(claimantContactPreferences.getOverriddenClaimantContactEmail())
            ? claimantContactPreferences.getOverriddenClaimantContactEmail()
            : claimantContactPreferences.getClaimantContactEmail();

        claimantParty.setEmailAddress(contactEmail);

        VerticalYesNo phoneNumberProvided = claimantContactPreferences.getClaimantProvidePhoneNumber();

        claimantParty.setPhoneNumberProvided(phoneNumberProvided);
        if (phoneNumberProvided == VerticalYesNo.YES) {
            claimantParty.setPhoneNumber(claimantContactPreferences.getClaimantContactPhoneNumber());
        }

        partyRepository.save(claimantParty);

        return claimantParty;
    }

    private static void setClaimantOrgName(ClaimantInformation claimantInformation, PartyEntity claimantParty) {
        if (claimantInformation.getOrgNameFound() == YesOrNo.NO) {
            claimantParty.setNameOverridden(YesOrNo.YES);
            claimantParty.setOrgName(claimantInformation.getFallbackClaimantName());
        } else if (claimantInformation.getIsClaimantNameCorrect() == VerticalYesNo.NO) {
            claimantParty.setNameOverridden(YesOrNo.YES);
            claimantParty.setOrgName(claimantInformation.getOverriddenClaimantName());
        } else {
            claimantParty.setNameOverridden(YesOrNo.NO);
            claimantParty.setOrgName(claimantInformation.getClaimantName());
        }
    }

    private List<PartyEntity> createDefendants(PCSCase pcsCase) {
        Objects.requireNonNull(pcsCase.getDefendant1(), "Defendant 1 must be provided");

        List<PartyEntity> allDefendants = new ArrayList<>();
        allDefendants.add(createDefendant(pcsCase.getDefendant1()));

        if (pcsCase.getAddAnotherDefendant() == VerticalYesNo.YES) {
            pcsCase.getAdditionalDefendants().stream()
                .map(ListValue::getValue)
                .map(this::createDefendant)
                .forEach(allDefendants::add);
        }

        partyRepository.saveAll(allDefendants);

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

        VerticalYesNo addressKnown = defendantDetails.getAddressKnown();
        defendantEntity.setAddressKnown(addressKnown);
        if (addressKnown == VerticalYesNo.YES) {
            VerticalYesNo addressSameAsPossession = defendantDetails.getAddressSameAsPossession();
            defendantEntity.setAddressSameAsProperty(addressSameAsPossession);
            if (addressSameAsPossession == VerticalYesNo.NO) {
                defendantEntity.setAddress(mapAddress(defendantDetails.getCorrespondenceAddress()));
            }
        }

        return defendantEntity;
    }

    private List<PartyEntity> createUnderlesseeOrMortgagees(PCSCase pcsCase) {
        if (pcsCase.getHasUnderlesseeOrMortgagee() != VerticalYesNo.YES) {
            return List.of();
        }

        Objects.requireNonNull(pcsCase.getUnderlesseeOrMortgagee1(), "Underlessee or mortgagee 1 must be provided");

        List<PartyEntity> allUnderlesseeOrMortgagees = new ArrayList<>();
        allUnderlesseeOrMortgagees.add(createUnderlesseeOrMortgagee(pcsCase.getUnderlesseeOrMortgagee1()));

        if (pcsCase.getAddAdditionalUnderlesseeOrMortgagee() == VerticalYesNo.YES) {
            pcsCase.getAdditionalUnderlesseeOrMortgagee().stream()
                .map(ListValue::getValue)
                .map(this::createUnderlesseeOrMortgagee)
                .forEach(allUnderlesseeOrMortgagees::add);
        }

        partyRepository.saveAll(allUnderlesseeOrMortgagees);

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
        underlesseeMortgageeEntity.setAddressKnown(addressKnown);
        if (addressKnown == VerticalYesNo.YES) {
            underlesseeMortgageeEntity
                .setAddress(mapAddress(underlesseeMortgageeDetails.getAddress()));
        }

        return underlesseeMortgageeEntity;
    }

    private AddressEntity mapAddress(AddressUK address) {
        return address != null
            ? addressMapper.toAddressEntityAndNormalise(address) : null;
    }

    private AddressUK resolveContactAddress(ClaimantContactPreferences contactPreferences) {
        if (contactPreferences.getOverriddenClaimantContactAddress() != null) {
            return contactPreferences.getOverriddenClaimantContactAddress();
        }
        return contactPreferences.getOrganisationAddress();
    }

}

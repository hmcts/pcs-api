package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;

@Service
@Slf4j
@AllArgsConstructor
public class LegalRepresentativePageService {

    private final ClaimPartyContactDetailsRepository
        claimPartyContactDetailsRepository;
    private final AddressMapper addressMapper;
    private final SecurityContextService securityContextService;
    private final AddressFormatter addressFormatter;

    @Transactional
    public void save(long caseReference, LegalRepresentativeDetails legalRepresentativeDetails) {
        Optional<ClaimPartyContactDetailsEntity> contactDetails =
            claimPartyContactDetailsRepository.findByOrganisationIdAndCaseReference(
                legalRepresentativeDetails.getOrganisationId(),
            caseReference
        );

        ClaimPartyContactDetailsEntity organisationContactDetails = contactDetails
                .orElseThrow(() -> new IllegalStateException("Cannot find "
                                                                 + "LegalRepresentativeOrganisationContactDetails"));

        if (legalRepresentativeDetails.getDifferentPostalAddress() != null
            && legalRepresentativeDetails.getDifferentPostalAddress().equals(VerticalYesNo.YES)) {

            organisationContactDetails
                .setAddress(mapAddressUkToAddressEntity(legalRepresentativeDetails.getUpdatedCorrespondenceAddress()));
        }

        if (legalRepresentativeDetails.getProvideContactPhoneNumber() != null
            && legalRepresentativeDetails.getProvideContactPhoneNumber().equals(VerticalYesNo.YES)) {
            organisationContactDetails.setPhoneNumber(legalRepresentativeDetails
                                                                             .getContactPhoneNumber());
        }

        if (legalRepresentativeDetails.getReference() != null && !legalRepresentativeDetails.getReference().isEmpty()) {
            organisationContactDetails
                .setContactReference(legalRepresentativeDetails.getReference());
        }

        updateEmail(legalRepresentativeDetails.getUseEmailAddress(), organisationContactDetails,
                    legalRepresentativeDetails.getEmailAddress(), legalRepresentativeDetails.getOriginalEmailAddress());

        organisationContactDetails.setContactDetailsCorrectConfirmation(YesOrNo.YES);

        claimPartyContactDetailsRepository.save(organisationContactDetails);
    }

    public LegalRepresentativeDetails retrieveLegalRepresentativeDetails(String organisationId,
                                                                         long caseReference,
                                                                         LegalRepresentativeDetails details) {
        Optional<ClaimPartyContactDetailsEntity> contactDetails =
            claimPartyContactDetailsRepository.findByOrganisationIdAndCaseReference(
            organisationId,
            caseReference
        );

        ClaimPartyContactDetailsEntity organisationContactDetails = contactDetails
            .orElseThrow(() -> new IllegalStateException("Cannot find LegalRepresentativeOrganisationContactDetails"));


        if (details == null) {
            details = LegalRepresentativeDetails.builder().build();
        }

        if (organisationContactDetails.getEmailAddress() != null) {
            details.setOriginalEmailAddress(organisationContactDetails.getEmailAddress());
        } else {
            String userEmail = securityContextService.getCurrentUserDetails().getSub();
            details.setOriginalEmailAddress(userEmail);
        }

        details
            .setLegalRepresentativeOrganisationAddress(mapAddressEntityToAddressUk(organisationContactDetails
                                                                                       .getAddress()));

        details
            .setFormattedContactAddress(formatContactAddress(details.getLegalRepresentativeOrganisationAddress()));

        if (details.getLegalRepresentativeOrganisationAddress() != null) {
            details.setOrganisationAddressFound(YesOrNo.YES);
        } else {
            details.setOrganisationAddressFound(YesOrNo.NO);
        }
        details.setOrganisationId(organisationId);

        return details;
    }

    private AddressEntity mapAddressUkToAddressEntity(AddressUK address) {
        return address != null
            ? addressMapper.toAddressEntityAndNormalise(address) : null;
    }

    private String formatContactAddress(AddressUK address) {
        return address != null
            ? addressFormatter.formatMediumAddress(address, BR_DELIMITER) : null;
    }

    private AddressUK mapAddressEntityToAddressUk(AddressEntity address) {
        return address != null
            ? addressMapper.toAddressUK(address) : null;
    }

    private void updateEmail(VerticalYesNo useEmail, ClaimPartyContactDetailsEntity contactDetails,
                             String newEmail, String originalEmail) {
        if (useEmail == null || contactDetails == null) {
            return;
        }

        String targetEmail = (useEmail == VerticalYesNo.NO) ? newEmail : originalEmail;
        contactDetails.setEmailAddress(targetEmail);
    }
}

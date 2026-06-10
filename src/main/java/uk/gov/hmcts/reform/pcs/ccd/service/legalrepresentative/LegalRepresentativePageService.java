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
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;

@Service
@Slf4j
@AllArgsConstructor
public class LegalRepresentativePageService {

    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final AddressMapper addressMapper;
    private final SecurityContextService securityContextService;
    private final AddressFormatter addressFormatter;

    @Transactional
    public void save(String organisationId, long caseReference, LegalRepresentativeDetails legalRepresentativeDetails) {
        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisation =
            legalRepresentativeOrganisationRepository
                .findByOrganisationIdAndCaseReference(organisationId, caseReference);

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            legalRepresentativeOrganisation.orElseGet(LegalRepresentativeOrganisationEntity::new);

        if (legalRepresentativeOrganisationEntity.getLegalRepresentativeList().isEmpty()) {

            LegalRepresentativeEntity legalRepresentative = LegalRepresentativeEntity.builder()
                .idamId(securityContextService.getCurrentUserId())
                .build();

            legalRepresentativeOrganisationEntity.addLegalRepresentative(legalRepresentative);
        }

        if (legalRepresentativeDetails.getDifferentPostalAddress() != null
            && legalRepresentativeDetails.getDifferentPostalAddress().equals(VerticalYesNo.YES)) {
            legalRepresentativeOrganisationEntity.setAddress(mapAddressUkToAddressEntity(legalRepresentativeDetails
                                                                .getUpdatedCorrespondenceAddress()));
        }

        if (legalRepresentativeDetails.getProvideContactPhoneNumber() != null
            && legalRepresentativeDetails.getProvideContactPhoneNumber().equals(VerticalYesNo.YES)) {
            legalRepresentativeOrganisationEntity.setPhone(legalRepresentativeDetails.getContactPhoneNumber());
        }

        if (legalRepresentativeDetails.getReference() != null && !legalRepresentativeDetails.getReference().isEmpty()) {
            legalRepresentativeOrganisationEntity.setContactReference(legalRepresentativeDetails.getReference());
        }

        if (legalRepresentativeDetails.getUseEmailAddress() != null
            && legalRepresentativeDetails.getUseEmailAddress().equals(VerticalYesNo.NO)) {
            legalRepresentativeOrganisationEntity.setEmail(legalRepresentativeDetails.getEmailAddress());
        }

        legalRepresentativeOrganisationEntity.setHasAmendedContactDetails(YesOrNo.YES);

        legalRepresentativeOrganisationRepository.save(legalRepresentativeOrganisationEntity);
    }

    public LegalRepresentativeDetails retrieveLegalRepresentativeDetails(String organisationId,
                                                                         long caseReference,
                                                                         LegalRepresentativeDetails details) {
        Optional<LegalRepresentativeOrganisationEntity> legalRepOrganisation = legalRepresentativeOrganisationRepository
            .findByOrganisationIdAndCaseReference(organisationId, caseReference);

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation = legalRepOrganisation
            .orElseGet(() -> LegalRepresentativeOrganisationEntity.builder().build());

        if (details == null) {
            details = LegalRepresentativeDetails.builder().build();
        }

        if (legalRepresentativeOrganisation.getEmail() != null) {
            details.setOriginalEmailAddress(legalRepresentativeOrganisation.getEmail());
        } else {
            String userEmail = securityContextService.getCurrentUserDetails().getSub();
            details.setOriginalEmailAddress(userEmail);
        }

        details
            .setLegalRepresentativeOrganisationAddress(mapAddressEntityToAddressUk(legalRepresentativeOrganisation.getAddress()));

        details
            .setFormattedContactAddress(formatContactAddress(details.getLegalRepresentativeOrganisationAddress()));

        if (details.getLegalRepresentativeOrganisationAddress() != null) {
            details.setOrganisationAddressFound(YesOrNo.YES);
        } else {
            details.setOrganisationAddressFound(YesOrNo.NO);
        }
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
}

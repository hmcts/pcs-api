package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class LegalRepresentativePageService {

    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final AddressMapper addressMapper;
    private final SecurityContextService securityContextService;

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
            legalRepresentativeOrganisationEntity.setAddress(mapAddress(legalRepresentativeDetails
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

        legalRepresentativeOrganisationRepository.save(legalRepresentativeOrganisationEntity);
    }

    private AddressEntity mapAddress(AddressUK address) {
        return address != null
            ? addressMapper.toAddressEntityAndNormalise(address) : null;
    }
}

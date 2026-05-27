package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class LegalRepresentativePageService {

    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public void save(UUID userIdamId, LegalRepresentativeDetails legalRepresentativeDetails) {
        Optional<LegalRepresentativeOrganisationEntity> legalRepresentative = legalRepresentativeOrganisationRepository
            .findByIdamId(userIdamId);

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            legalRepresentative.orElseGet(LegalRepresentativeOrganisationEntity::new);

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
            legalRepresentativeOrganisationEntity.setReference(legalRepresentativeDetails.getReference());
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

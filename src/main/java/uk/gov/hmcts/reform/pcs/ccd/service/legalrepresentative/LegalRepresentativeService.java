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
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class LegalRepresentativeService {

    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public void save(UUID userIdamId, LegalRepresentativeDetails legalRepresentativeDetails) {
        Optional<LegalRepresentativeEntity> legalRepresentative = legalRepresentativeRepository
            .findByIdamId(userIdamId);

        LegalRepresentativeEntity legalRepresentativeEntity =
            legalRepresentative.orElseGet(LegalRepresentativeEntity::new);

        if (legalRepresentativeDetails.getDifferentPostalAddress() != null
            && legalRepresentativeDetails.getDifferentPostalAddress().equals(VerticalYesNo.YES)) {
            legalRepresentativeEntity.setAddress(mapAddress(legalRepresentativeDetails
                                                                .getUpdatedCorrespondenceAddress()));
        }

        if (legalRepresentativeDetails.getProvideContactPhoneNumber() != null
            && legalRepresentativeDetails.getProvideContactPhoneNumber().equals(VerticalYesNo.YES)) {
            legalRepresentativeEntity.setPhone(legalRepresentativeDetails.getContactPhoneNumber());
        }

        if (legalRepresentativeDetails.getReference() != null && !legalRepresentativeDetails.getReference().isEmpty()) {
            legalRepresentativeEntity.setReference(legalRepresentativeDetails.getReference());
        }

        if (legalRepresentativeDetails.getUseEmailAddress() != null
            && legalRepresentativeDetails.getUseEmailAddress().equals(VerticalYesNo.NO)) {
            legalRepresentativeEntity.setEmail(legalRepresentativeDetails.getEmailAddress());
        }

        legalRepresentativeRepository.save(legalRepresentativeEntity);
    }

    private AddressEntity mapAddress(AddressUK address) {
        return address != null
            ? addressMapper.toAddressEntityAndNormalise(address) : null;
    }
}

package uk.gov.hmcts.reform.pcs.ccd.service.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertionStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAttributeAssertionRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

/**
 * Resolves a defending defendant's correspondence address, honouring a submitted CORRESPONDENCE_ADDRESS
 * assertion (an overridden address) before falling back to the party or property address. Shared by the
 * defence form and the bulk-print coversheet so the envelope matches the enclosed defence form.
 */
@Service
@Slf4j
public class DefenceCorrespondenceAddressResolver {

    private final PartyAttributeAssertionRepository assertionRepository;
    private final ObjectMapper objectMapper;
    private final AddressMapper addressMapper;

    public DefenceCorrespondenceAddressResolver(PartyAttributeAssertionRepository assertionRepository,
                                                ObjectMapper objectMapper,
                                                AddressMapper addressMapper) {
        this.assertionRepository = assertionRepository;
        this.objectMapper = objectMapper;
        this.addressMapper = addressMapper;
    }

    public AddressUK resolveCorrespondenceAddress(PartyEntity defendant, AddressEntity propertyAddress) {
        AddressUK assertedAddress = assertedCorrespondenceAddress(defendant);
        if (assertedAddress != null) {
            return assertedAddress;
        }
        AddressEntity fallback = defendant.getAddress() != null ? defendant.getAddress() : propertyAddress;
        return fallback == null ? null : addressMapper.toAddressUK(fallback);
    }

    private AddressUK assertedCorrespondenceAddress(PartyEntity defendant) {
        String assertedValue = assertionRepository.findByPartyIdAndAssertedByAndStatus(
                defendant.getId(),
                PartyAttributeAssertedBy.DEFENDANT,
                PartyAttributeAssertionStatus.SUBMITTED).stream()
            .filter(assertion -> assertion.getAttributesName() == PartyAttributeType.CORRESPONDENCE_ADDRESS)
            .map(assertion -> assertion.getAssertedValue())
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(null);
        if (assertedValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(assertedValue, AddressUK.class);
        } catch (Exception e) {
            log.error("Failed to parse defendant correspondence address assertion", e);
            return null;
        }
    }
}

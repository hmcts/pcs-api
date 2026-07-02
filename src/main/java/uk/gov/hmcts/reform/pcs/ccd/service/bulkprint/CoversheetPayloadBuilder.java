package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;

/**
 * Builds the coversheet payload from an already-resolved recipient name and address. Pack-agnostic: the
 * pack-specific candidate service resolves who the recipient is and which address applies, then calls this.
 */
@Service
public class CoversheetPayloadBuilder {

    public CoversheetPayload build(String recipientName, AddressUK address, String caseReference) {
        return CoversheetPayload.builder()
            .caseReference(caseReference)
            .recipientName(recipientName)
            .recipientAddressLine1(address.getAddressLine1())
            .recipientAddressLine2(address.getAddressLine2())
            .recipientAddressLine3(address.getAddressLine3())
            .recipientPostTown(address.getPostTown())
            .recipientCounty(address.getCounty())
            .recipientPostcode(address.getPostCode())
            .hasAddressLine2(StringUtils.isNotBlank(address.getAddressLine2()))
            .hasAddressLine3(StringUtils.isNotBlank(address.getAddressLine3()))
            .hasCounty(StringUtils.isNotBlank(address.getCounty()))
            .build();
    }
}

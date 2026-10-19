package uk.gov.hmcts.reform.pcs.ccd.service.coverletter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.document.model.coverletter.CoveringLetterPayload;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatLongDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.joinName;

@Service
public class CoveringLetterPayloadBuilder {

    static final String YOURS_SINCERELY = "Yours sincerely";
    static final String YOURS_FAITHFULLY = "Yours faithfully";

    public CoveringLetterPayload build(PartyEntity recipient, String recipientName, AddressUK address,
                                       String caseReference, String caseName, CourtVenue court,
                                       LocalDate letterDate) {
        List<String> courtLines = courtAddressLines(court.courtAddress());
        String courtAddressLine1 = courtLines.isEmpty() ? null : courtLines.getFirst();
        String courtAddressLine2 = courtLines.size() < 2
            ? null
            : String.join(", ", courtLines.subList(1, courtLines.size()));
        String recipientAddressLine2 = secondAddressLine(address);
        boolean organisation = StringUtils.isNotBlank(recipient.getOrgName());
        String personalName = organisation ? null : StringUtils.trimToNull(recipientName);

        return CoveringLetterPayload.builder()
            .caseReference(caseReference)
            .caseName(caseName)
            .letterDate(formatLongDate(letterDate))
            .signOff(namedPerson(recipient) ? YOURS_SINCERELY : YOURS_FAITHFULLY)
            .courtName(court.courtName())
            .courtAddressLine1(courtAddressLine1)
            .courtAddressLine2(courtAddressLine2)
            .courtPostcode(StringUtils.trimToNull(court.postcode()))
            .hasCourtAddressLine2(courtAddressLine2 != null)
            .recipientName(recipientName)
            .recipientPersonalName(personalName)
            .recipientOrganisationName(organisation ? recipient.getOrgName().trim() : null)
            .hasRecipientPersonalName(personalName != null)
            .hasRecipientOrganisationName(organisation)
            .recipientAddressLine1(address.getAddressLine1())
            .recipientAddressLine2(recipientAddressLine2)
            .recipientPostTown(StringUtils.trimToNull(address.getPostTown()))
            .recipientCounty(StringUtils.trimToNull(address.getCounty()))
            .recipientPostcode(StringUtils.trimToNull(address.getPostCode()))
            .hasRecipientAddressLine2(recipientAddressLine2 != null)
            .hasRecipientCounty(StringUtils.isNotBlank(address.getCounty()))
            .build();
    }

    private boolean namedPerson(PartyEntity recipient) {
        return StringUtils.isBlank(recipient.getOrgName())
            && recipient.getNameKnown() != VerticalYesNo.NO
            && StringUtils.isNotBlank(joinName(recipient.getFirstName(), recipient.getLastName()));
    }

    private String secondAddressLine(AddressUK address) {
        String line2 = StringUtils.trimToNull(address.getAddressLine2());
        String line3 = StringUtils.trimToNull(address.getAddressLine3());
        if (line2 == null) {
            return line3;
        }
        if (line3 == null) {
            return line2;
        }
        return line2 + ", " + line3;
    }

    private List<String> courtAddressLines(String courtAddress) {
        if (StringUtils.isBlank(courtAddress)) {
            return List.of();
        }
        return Arrays.stream(courtAddress.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toList();
    }
}

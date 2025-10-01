package uk.gov.hmcts.reform.pcs.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CaseDescriptionService {

    public String createCaseDescription(PCSCase pcsCase) {
        AddressUK propertyAddress = pcsCase.getPropertyAddress();
        String caseNumber = pcsCase.getHyphenatedCaseRef();
        return "Case number: " + caseNumber + "\nProperty address: " + getPropertyAddressAsString(propertyAddress);
    }

    private String getPropertyAddressAsString(AddressUK propertyAddress) {
        if (propertyAddress != null) {
            return Stream.of(propertyAddress.getAddressLine1(), propertyAddress.getAddressLine2(),
                             propertyAddress.getAddressLine3(), propertyAddress.getPostTown())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        } else {
            return "Not specified";
        }
    }

}

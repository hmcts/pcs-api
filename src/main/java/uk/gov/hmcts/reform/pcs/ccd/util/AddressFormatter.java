package uk.gov.hmcts.reform.pcs.ccd.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AddressFormatter {

    public String formatAddressWithCommas(AddressUK address) {
        return formatAddressWithDelimiter(address, ", ");
    }

    public String formatAddressWithHtmlLineBreaks(AddressUK address) {
        return formatAddressWithDelimiter(address, "<br>");
    }

    private static String formatAddressWithDelimiter(AddressUK address, String delimiter) {
        return Stream.of(address.getAddressLine1(), address.getPostTown(), address.getPostCode())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(delimiter));
    }

}

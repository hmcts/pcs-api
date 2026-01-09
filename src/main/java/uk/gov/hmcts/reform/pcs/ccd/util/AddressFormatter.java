package uk.gov.hmcts.reform.pcs.ccd.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AddressFormatter {

    public static final String COMMA_DELIMITER = ", ";
    public static final String BR_DELIMITER = "<br>";

    /**
     * Formats an {@link AddressUK} with the mandatory address fields, (address line 1, post-town and postcode).
     * @param address The address to format
     * @param delimiter The delimiter with which to join each part of the address
     * @return A formatted address String
     */
    public String formatShortAddress(AddressUK address, String delimiter) {
        Objects.requireNonNull(delimiter, "Delimiter must not be null");

        if (address == null) {
            return null;
        }

<<<<<<< HEAD
    private static String formatAddressWithDelimiter(AddressUK address, String delimiter) {
        if (address == null) {
            return "";
        }
=======
>>>>>>> 88d23464c3c49f3f58887fe0f0d05926df71a6c4
        return Stream.of(address.getAddressLine1(), address.getPostTown(), address.getPostCode())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(delimiter));
    }

    /**
     * Formats an {@link AddressUK} with the main address fields, (address line 1, address line 2,
     * post-town and postcode).
     * @param address The address to format
     * @param delimiter The delimiter with which to join each part of the address
     * @return A formatted address String
     */
    public String formatMediumAddress(AddressUK address, String delimiter) {
        Objects.requireNonNull(delimiter, "Delimiter must not be null");

        if (address == null) {
            return null;
        }

        return Stream.of(address.getAddressLine1(), address.getAddressLine2(),
                         address.getPostTown(), address.getPostCode())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(delimiter));
    }

}

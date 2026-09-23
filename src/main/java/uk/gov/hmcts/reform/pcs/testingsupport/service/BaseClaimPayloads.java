package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.io.InputStream;

/** The claim that testing support creates cases with, and the property address it creates them at. */
public final class BaseClaimPayloads {

    private BaseClaimPayloads() {
    }

    public static ObjectNode read(ObjectMapper objectMapper, LegislativeCountry legislativeCountry) {
        String path = "testing-support/Create-Case-" + legislativeCountry + "-Base.json";
        try (InputStream basePayload = BaseClaimPayloads.class.getClassLoader().getResourceAsStream(path)) {
            return (ObjectNode) objectMapper.readTree(basePayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read base payload JSON", e);
        }
    }

    public static AddressUK propertyAddress(LegislativeCountry legislativeCountry) {
        return switch (legislativeCountry) {
            case ENGLAND -> AddressUK.builder()
                .addressLine1("1 Second Avenue")
                .postTown("London")
                .county("Greater London")
                .postCode("W3 7RX")
                .country("United Kingdom")
                .build();

            case WALES -> AddressUK.builder()
                .addressLine1("2 Pentre Street")
                .postTown("Caerdydd")
                .postCode("CF11 6QX")
                .country("Deyrnas Unedig")
                .build();

            default -> throw new IllegalArgumentException(
                "Unsupported legislative country: " + legislativeCountry
            );
        };
    }
}

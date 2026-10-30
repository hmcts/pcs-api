package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalDocumentTypeTest {

    @Test
    void englandDocumentTypesShouldMatchApplicableAdditionalDocumentTypes() {
        Map<String, String> expectedDocumentTypes = documentTypesFor(LegislativeCountry.ENGLAND);
        Map<String, String> actualDocumentTypes = Arrays.stream(AdditionalDocumentTypeEngland.values())
                .collect(toMap(AdditionalDocumentTypeEngland::name, AdditionalDocumentTypeEngland::getLabel));

        assertThat(actualDocumentTypes).isEqualTo(expectedDocumentTypes);
    }

    @Test
    void walesDocumentTypesShouldMatchApplicableAdditionalDocumentTypes() {
        Map<String, String> expectedDocumentTypes = documentTypesFor(LegislativeCountry.WALES);
        Map<String, String> actualDocumentTypes = Arrays.stream(AdditionalDocumentTypeWales.values())
                .collect(toMap(AdditionalDocumentTypeWales::name, AdditionalDocumentTypeWales::getLabel));

        assertThat(actualDocumentTypes).isEqualTo(expectedDocumentTypes);
    }

    private Map<String, String> documentTypesFor(LegislativeCountry country) {
        return Arrays.stream(AdditionalDocumentType.values())
                .filter(documentType -> documentType.isApplicableFor(country))
                .collect(toMap(AdditionalDocumentType::name, AdditionalDocumentType::getLabel));
    }
}

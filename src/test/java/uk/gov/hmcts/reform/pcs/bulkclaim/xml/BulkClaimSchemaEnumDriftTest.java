package uk.gov.hmcts.reform.pcs.bulkclaim.xml;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.LanguageUsedType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.LegislativeCountryType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NoticeServiceMethodType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentArrearsOrBreachType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.TenancyTypeCode;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails when an enumeration in the bulk claim XSDs and the Java enum it maps to diverge.
 */
class BulkClaimSchemaEnumDriftTest {

    @Test
    void tenancyTypeShouldMatchEnglandAndWalesTypes() {
        List<String> javaNames = Stream.concat(
            Arrays.stream(TenancyLicenceType.values()).map(Enum::name),
            Arrays.stream(OccupationLicenceTypeWales.values()).map(Enum::name)
        ).distinct().toList();

        assertThat(xsdValues(TenancyTypeCode.values())).containsExactlyInAnyOrderElementsOf(javaNames);
    }

    @Test
    void tenancyTypeShouldMatchCombinedLicenceType() {
        assertThat(xsdValues(TenancyTypeCode.values()))
            .containsExactlyInAnyOrderElementsOf(names(CombinedLicenceType.values()));
    }

    @Test
    void noticeServiceMethodShouldMatchJavaEnum() {
        assertThat(xsdValues(NoticeServiceMethodType.values()))
            .containsExactlyInAnyOrderElementsOf(names(NoticeServiceMethod.values()));
    }

    @Test
    void languageUsedShouldMatchJavaEnum() {
        assertThat(xsdValues(LanguageUsedType.values()))
            .containsExactlyInAnyOrderElementsOf(names(LanguageUsed.values()));
    }

    @Test
    void rentArrearsOrBreachShouldMatchJavaEnum() {
        assertThat(xsdValues(RentArrearsOrBreachType.values()))
            .containsExactlyInAnyOrderElementsOf(names(RentArrearsOrBreachOfTenancy.values()));
    }

    @Test
    void legislativeCountryShouldBeSubsetOfJavaEnum() {
        assertThat(xsdValues(LegislativeCountryType.values()))
            .isSubsetOf(names(LegislativeCountry.values()));
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private static List<String> xsdValues(Enum<?>[] generated) {
        return Arrays.stream(generated).map(e -> {
            try {
                return (String) e.getClass().getMethod("value").invoke(e);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }).toList();
    }
}

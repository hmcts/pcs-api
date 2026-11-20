package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ParticularsRentedType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.TenancyTypeCode;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.TenancyDateRules;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental.RentalPreparerTestSupport.CLOCK;

class WalesOccupationContractMapperTest {

    private static final String PATH = "ClaimParticulars";
    private static final String ABSOLUTE = "/RentalClaimSet/RentalClaim[1]/" + PATH;

    private final WalesOccupationContractMapper mapper = new WalesOccupationContractMapper(new TenancyDateRules(CLOCK));
    private final BulkErrorCollector errors = BulkErrorCollector.forClaimPath("/RentalClaimSet/RentalClaim[1]");
    private final PCSCase target = PCSCase.builder().build();

    private static ParticularsRentedType particulars(TenancyTypeCode type, String otherDetails, LocalDate start) {
        ParticularsRentedType particulars = new ParticularsRentedType();
        particulars.setTenancyType(type);
        particulars.setOtherTenancyTypeDetails(otherDetails);
        particulars.setTenancyCommenceDate(start);
        return particulars;
    }

    private OccupationLicenceTypeWales apply(ParticularsRentedType particulars) {
        return mapper.apply(particulars, target, PATH, errors);
    }

    @Test
    void secureContractIsMappedWithItsStartDate() {
        OccupationLicenceTypeWales type = apply(particulars(TenancyTypeCode.SECURE_CONTRACT, null,
                                                            LocalDate.of(2020, 1, 1)));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(type).isEqualTo(OccupationLicenceTypeWales.SECURE_CONTRACT);
        assertThat(target.getOccupationLicenceDetailsWales().getOccupationLicenceTypeWales())
            .isEqualTo(OccupationLicenceTypeWales.SECURE_CONTRACT);
        assertThat(target.getOccupationLicenceDetailsWales().getLicenceStartDate())
            .isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(target.getOccupationLicenceDetailsWales().getLicenceDocuments()).isNull();
        assertThat(target.getTenancyLicenceDetails()).isNull();
    }

    @Test
    void standardContractWithoutStartDateIsValid() {
        OccupationLicenceTypeWales type = apply(particulars(TenancyTypeCode.STANDARD_CONTRACT, "ignored", null));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(type).isEqualTo(OccupationLicenceTypeWales.STANDARD_CONTRACT);
        assertThat(target.getOccupationLicenceDetailsWales().getOtherLicenceTypeDetails()).isNull();
    }

    @Test
    void otherContractNeedsDetails() {
        OccupationLicenceTypeWales type = apply(particulars(TenancyTypeCode.OTHER, null, null));

        assertThat(type).isEqualTo(OccupationLicenceTypeWales.OTHER);
        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code())
            .containsExactly(ABSOLUTE + "/OtherTenancyTypeDetails:REQUIRED");
    }

    @Test
    void otherContractDetailsAreLimitedToFiveHundredCharacters() {
        apply(particulars(TenancyTypeCode.OTHER, "x".repeat(501), null));

        assertThat(errors.errors()).extracting(error -> error.code()).containsExactly("TOO_LONG");
    }

    @Test
    void otherContractRecordsItsDetails() {
        apply(particulars(TenancyTypeCode.OTHER, "A licence agreement.", null));

        assertThat(errors.hasErrors()).isFalse();
        assertThat(target.getOccupationLicenceDetailsWales().getOtherLicenceTypeDetails())
            .isEqualTo("A licence agreement.");
    }

    @Test
    void startDateMustBeBeforeToday() {
        apply(particulars(TenancyTypeCode.STANDARD_CONTRACT, null, LocalDate.of(2026, 11, 19)));
        apply(particulars(TenancyTypeCode.STANDARD_CONTRACT, null, LocalDate.of(2026, 11, 20)));

        assertThat(errors.errors()).extracting(error -> error.path() + ":" + error.code()).containsExactly(
            ABSOLUTE + "/TenancyCommenceDate:NOT_IN_PAST", ABSOLUTE + "/TenancyCommenceDate:NOT_IN_PAST");
    }

    @Test
    void englandTenancyTypesAreRejected() {
        List<TenancyTypeCode> englandTypes = Arrays.stream(TenancyTypeCode.values())
            .filter(code -> code != TenancyTypeCode.SECURE_CONTRACT
                && code != TenancyTypeCode.STANDARD_CONTRACT && code != TenancyTypeCode.OTHER)
            .toList();

        for (TenancyTypeCode code : englandTypes) {
            assertThat(apply(particulars(code, null, null))).isNull();
        }

        assertThat(errors.errors()).hasSize(englandTypes.size())
            .extracting(error -> error.path()).containsOnly(ABSOLUTE + "/TenancyType");
        assertThat(target.getOccupationLicenceDetailsWales()).isNull();
    }
}

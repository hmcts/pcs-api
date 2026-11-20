package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantEligibilityRulesTest {

    private final ClaimantEligibilityRules underTest = new ClaimantEligibilityRules();

    @ParameterizedTest
    @CsvSource({
        "ENGLAND, PROVIDER_OF_SOCIAL_HOUSING, true",
        "ENGLAND, PRIVATE_LANDLORD, false",
        "ENGLAND, COMMUNITY_LANDLORD, false",
        "WALES, COMMUNITY_LANDLORD, true",
        "WALES, PROVIDER_OF_SOCIAL_HOUSING, false",
        "WALES, PRIVATE_LANDLORD, false",
        "SCOTLAND, PRIVATE_LANDLORD, true",
    })
    void shouldDecideWhetherClaimantTypeIsAllowedOnline(LegislativeCountry country, ClaimantType type,
                                                        boolean expected) {
        assertThat(underTest.isClaimantTypeAllowedOnline(country, type)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "ENGLAND, PROVIDER_OF_SOCIAL_HOUSING, false, false",
        "ENGLAND, PRIVATE_LANDLORD, true, false",
        "WALES, COMMUNITY_LANDLORD, false, false",
        "WALES, PRIVATE_LANDLORD, false, true",
        "SCOTLAND, PRIVATE_LANDLORD, false, false",
    })
    void shouldFlagClaimantTypeNotAllowedPerCountry(LegislativeCountry country, ClaimantType type,
                                                    boolean notAllowedEngland, boolean notAllowedWales) {
        assertThat(underTest.isClaimantTypeNotAllowedInEngland(country, type)).isEqualTo(notAllowedEngland);
        assertThat(underTest.isClaimantTypeNotAllowedInWales(country, type)).isEqualTo(notAllowedWales);
    }

    @ParameterizedTest
    @CsvSource({
        "ENGLAND, YES, false, true, false",
        "ENGLAND, NO, true, false, false",
        "WALES, YES, false, false, true",
        "WALES, NO, true, false, false",
        "SCOTLAND, YES, true, false, false",
    })
    void shouldRejectTrespasserClaimsInEnglandAndWales(LegislativeCountry country, VerticalYesNo trespassers,
                                                       boolean allowed, boolean notAllowedEngland,
                                                       boolean notAllowedWales) {
        assertThat(underTest.isClaimTypeAllowedOnline(country, trespassers)).isEqualTo(allowed);
        assertThat(underTest.isTrespasserClaimNotAllowedInEngland(country, trespassers))
            .isEqualTo(notAllowedEngland);
        assertThat(underTest.isTrespasserClaimNotAllowedInWales(country, trespassers)).isEqualTo(notAllowedWales);
    }

    @ParameterizedTest
    @CsvSource({"ENGLAND", "WALES"})
    void shouldAllowUnansweredTrespasserQuestion(LegislativeCountry country) {
        assertThat(underTest.isClaimTypeAllowedOnline(country, null)).isTrue();
    }
}

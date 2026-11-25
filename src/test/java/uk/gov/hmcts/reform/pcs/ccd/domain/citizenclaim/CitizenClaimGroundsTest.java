package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenClaimGroundsTest {

    @Test
    void shouldDropNullGroundsWhenCollectingThem() {
        CitizenClaimGrounds grounds = CitizenClaimGrounds.builder()
            .rentArrearsGrounds(setOf(null, PrivateLandlordAssuredGround.GROUND_8))
            .otherGrounds(setOf(PrivateLandlordAssuredGround.GROUND_1, null))
            .build();

        assertThat(grounds.allGrounds())
            .containsExactly(PrivateLandlordAssuredGround.GROUND_8, PrivateLandlordAssuredGround.GROUND_1);
        assertThat(new CitizenClaimGrounds().allGrounds()).isEmpty();
    }

    @Test
    void shouldFindTheWalesRentArrearsGrounds() {
        assertThat(wales(DiscretionaryGroundWales.RENT_ARREARS_S157).hasWalesRentArrearsGround()).isTrue();
        assertThat(wales(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181).hasWalesRentArrearsGround()).isTrue();
        assertThat(wales(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187).hasWalesRentArrearsGround()).isTrue();
        assertThat(wales(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199).hasWalesRentArrearsGround()).isFalse();
        assertThat(new CitizenClaimGrounds().hasWalesRentArrearsGround()).isFalse();
    }

    private static CitizenClaimGrounds wales(DiscretionaryGroundWales ground) {
        return CitizenClaimGrounds.builder().walesDiscretionaryGrounds(Set.of(ground)).build();
    }

    private static CitizenClaimGrounds wales(MandatoryGroundWales ground) {
        return CitizenClaimGrounds.builder().walesMandatoryGrounds(Set.of(ground)).build();
    }

    private static Set<PrivateLandlordAssuredGround> setOf(PrivateLandlordAssuredGround... grounds) {
        return new LinkedHashSet<>(Arrays.asList(grounds));
    }
}

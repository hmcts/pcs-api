package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ExemptLandlordCompatTest {

    @Test
    void shouldPreferExemptLandlordWhenBothPresentInResponses() {
        DefendantResponses responses = DefendantResponses.builder()
            .exemptLandlord(YesNoNotSure.NO)
            .landlordRegistered(YesNoNotSure.YES)
            .build();

        assertThat(ExemptLandlordCompat.resolveFromResponses(responses)).isEqualTo(YesNoNotSure.NO);
    }

    @Test
    void shouldFallBackToLandlordRegisteredWhenExemptLandlordMissing() {
        DefendantResponses responses = DefendantResponses.builder()
            .landlordRegistered(YesNoNotSure.NOT_SURE)
            .build();

        assertThat(ExemptLandlordCompat.resolveFromResponses(responses)).isEqualTo(YesNoNotSure.NOT_SURE);
    }

    @Test
    void shouldReadLandlordRegisteredFromEntityDuringCompatPhase() {
        DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .landlordRegistered(YesNoNotSure.YES)
            .build();

        assertThat(ExemptLandlordCompat.resolveFromEntity(entity)).isEqualTo(YesNoNotSure.YES);
    }
}

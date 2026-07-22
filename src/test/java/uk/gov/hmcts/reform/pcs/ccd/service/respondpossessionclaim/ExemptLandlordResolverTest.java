package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ExemptLandlordResolverTest {

    @Test
    void shouldReturnNullWhenResponsesAreNull() {
        assertThat(ExemptLandlordResolver.fromResponses(null)).isNull();
    }

    @Test
    void shouldReturnNullWhenEntityIsNull() {
        assertThat(ExemptLandlordResolver.fromEntity(null)).isNull();
    }

    @Test
    void shouldPreferExemptLandlordWhenBothPresentInResponses() {
        DefendantResponses responses = DefendantResponses.builder()
            .exemptLandlord(YesNoNotSure.NO)
            .landlordRegistered(YesNoNotSure.YES)
            .build();

        assertThat(ExemptLandlordResolver.fromResponses(responses)).isEqualTo(YesNoNotSure.NO);
    }

    @Test
    void shouldFallBackToLandlordRegisteredWhenExemptLandlordMissing() {
        DefendantResponses responses = DefendantResponses.builder()
            .landlordRegistered(YesNoNotSure.NOT_SURE)
            .build();

        assertThat(ExemptLandlordResolver.fromResponses(responses)).isEqualTo(YesNoNotSure.NOT_SURE);
    }

    @Test
    void shouldReadLandlordRegisteredFromEntityDuringCompatPhase() {
        DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .landlordRegistered(YesNoNotSure.YES)
            .build();

        assertThat(ExemptLandlordResolver.fromEntity(entity)).isEqualTo(YesNoNotSure.YES);
    }
}

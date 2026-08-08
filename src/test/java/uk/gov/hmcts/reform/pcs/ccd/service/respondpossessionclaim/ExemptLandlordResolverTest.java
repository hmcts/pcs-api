package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ExemptLandlordResolverTest {

    @Test
    void shouldReadExemptLandlordFromResponses() {
        DefendantResponses responses = DefendantResponses.builder()
            .exemptLandlord(YesNoNotSure.NO)
            .build();

        assertThat(ExemptLandlordResolver.fromResponses(responses)).isEqualTo(YesNoNotSure.NO);
    }

    @Test
    void shouldReturnNullWhenExemptLandlordMissingInResponses() {
        DefendantResponses responses = DefendantResponses.builder().build();

        assertThat(ExemptLandlordResolver.fromResponses(responses)).isNull();
    }

    @Test
    void shouldReadExemptLandlordFromEntity() {
        DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .exemptLandlord(YesNoNotSure.YES)
            .build();

        assertThat(ExemptLandlordResolver.fromEntity(entity)).isEqualTo(YesNoNotSure.YES);
    }

    @Test
    void shouldReturnNullWhenEntityHasNoExemptLandlord() {
        DefendantResponseEntity entity = DefendantResponseEntity.builder().build();

        assertThat(ExemptLandlordResolver.fromEntity(entity)).isNull();
    }
}

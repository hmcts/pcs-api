package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantOnlyDraftBuilderTest {

    private DefendantOnlyDraftBuilder defendantOnlyDraftBuilder;

    @BeforeEach
    void setUp() {
        defendantOnlyDraftBuilder = new DefendantOnlyDraftBuilder();
    }

    @Test
    void shouldCreateDefendantOnlyDraft() {
        // given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .build();

        PossessionClaimResponse response =
            PossessionClaimResponse.builder()
                .defendantContactDetails(contactDetails)
                .build();

        // when
        PossessionClaimResponse result = defendantOnlyDraftBuilder.createDefendantOnlyDraft(response);

        // then
        assertThat(result.getDefendantContactDetails()).isEqualTo(contactDetails);
    }
}

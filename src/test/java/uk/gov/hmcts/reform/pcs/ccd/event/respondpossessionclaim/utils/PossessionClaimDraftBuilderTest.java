package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

import static org.assertj.core.api.Assertions.assertThat;

class PossessionClaimDraftBuilderTest {

    private PossessionClaimDraftBuilder possessionClaimDraftBuilder;

    @BeforeEach
    void setUp() {
        possessionClaimDraftBuilder = new PossessionClaimDraftBuilder();
    }

    @Test
    void shouldBuildCaseWithDraft() {
        // given
        PossessionClaimResponse response =
            PossessionClaimResponse.builder()
                .build();

        PCSCase pcsCase = PCSCase.builder()
            .build();

        // when
        PCSCase result = possessionClaimDraftBuilder.buildCaseWithDraft(pcsCase, response);

        // then
        assertThat(result.getPossessionClaimResponse()).isEqualTo(response);
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);
    }
}

package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenClaimantDetailsTest {

    @Test
    void shouldTextThePhoneNumberOnlyWhenTheClaimantAgreedToCalls() {
        CitizenClaimClaimant claimant = CitizenClaimClaimant.builder()
            .contactByPhone(VerticalYesNo.YES)
            .phoneNumber("07700900000")
            .contactByText(VerticalYesNo.YES)
            .build();
        assertThat(claimant.effectiveTextNumber()).isEqualTo("07700900000");

        claimant.setTextMessageNumber("07700900111");
        assertThat(claimant.effectiveTextNumber()).isEqualTo("07700900111");

        claimant.setTextMessageNumber(" ");
        claimant.setContactByPhone(VerticalYesNo.NO);
        assertThat(claimant.effectiveTextNumber()).isNull();
    }
}

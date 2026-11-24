package uk.gov.hmcts.reform.pcs.notify.template;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import static org.assertj.core.api.Assertions.assertThat;

class GenAppNotificationCopyTest {

    @Test
    void adjournCopy() {
        GenAppNotificationCopy.Copy copy = GenAppNotificationCopy.forType(GenAppType.ADJOURN);
        assertThat(copy.applicationTypePhrase()).isEqualTo(" to adjourn (delay) the hearing");
        assertThat(copy.applicantSubject()).isEqualTo("Request to adjourn (delay) the hearing received");
        assertThat(copy.respondentSubject())
            .isEqualTo("A party has made an application to adjourn (delay) the hearing");
    }

    @Test
    void setAsideCopy() {
        GenAppNotificationCopy.Copy copy = GenAppNotificationCopy.forType(GenAppType.SET_ASIDE);
        assertThat(copy.applicationTypePhrase()).isEqualTo(" to cancel (set aside) the order");
        assertThat(copy.applicantSubject()).isEqualTo("Request to cancel (set aside) the order received");
        assertThat(copy.respondentSubject())
            .isEqualTo("A party has made an application to cancel (set aside) the order");
    }

    @Test
    void somethingElseCopy() {
        GenAppNotificationCopy.Copy copy = GenAppNotificationCopy.forType(GenAppType.SOMETHING_ELSE);
        assertThat(copy.applicationTypePhrase()).isEmpty();
        assertThat(copy.applicantSubject()).isEqualTo("Application received");
        assertThat(copy.respondentSubject()).isEqualTo("A party has made an application");
    }

    @Test
    void suspendFallsBackToDefaultCopy() {
        assertThat(GenAppNotificationCopy.forType(GenAppType.SUSPEND))
            .isEqualTo(GenAppNotificationCopy.forType(GenAppType.SOMETHING_ELSE));
    }
}

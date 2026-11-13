package uk.gov.hmcts.reform.pcs.notify.template;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import static org.assertj.core.api.Assertions.assertThat;

class GenAppNotificationCopyTest {

    @Test
    void applicationTypePhrase() {
        assertThat(GenAppNotificationCopy.applicationTypePhrase(GenAppType.ADJOURN))
            .isEqualTo(" to adjourn (delay) the hearing");
        assertThat(GenAppNotificationCopy.applicationTypePhrase(GenAppType.SET_ASIDE))
            .isEqualTo(" to cancel (set aside) the order");
        assertThat(GenAppNotificationCopy.applicationTypePhrase(GenAppType.SOMETHING_ELSE)).isEmpty();
        assertThat(GenAppNotificationCopy.applicationTypePhrase(GenAppType.SUSPEND)).isEmpty();
    }

    @Test
    void applicantSubject() {
        assertThat(GenAppNotificationCopy.applicantSubject(GenAppType.ADJOURN))
            .isEqualTo("Request to adjourn (delay) the hearing received");
        assertThat(GenAppNotificationCopy.applicantSubject(GenAppType.SET_ASIDE))
            .isEqualTo("Request to cancel (set aside) the order received");
        assertThat(GenAppNotificationCopy.applicantSubject(GenAppType.SOMETHING_ELSE))
            .isEqualTo("Application received");
    }

    @Test
    void respondentSubject() {
        assertThat(GenAppNotificationCopy.respondentSubject(GenAppType.ADJOURN))
            .isEqualTo("A party has made an application to adjourn (delay) the hearing");
        assertThat(GenAppNotificationCopy.respondentSubject(GenAppType.SET_ASIDE))
            .isEqualTo("A party has made an application to cancel (set aside) the order");
        assertThat(GenAppNotificationCopy.respondentSubject(GenAppType.SOMETHING_ELSE))
            .isEqualTo("A party has made an application");
    }
}

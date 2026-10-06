package uk.gov.hmcts.reform.pcs.ccd.event.bundling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;

import static org.assertj.core.api.Assertions.assertThat;

class AttachCaseBundleTest extends BaseEventTest {

    @BeforeEach
    void setUp() {
        setEventUnderTest(new AttachCaseBundle());
    }

    @Test
    void shouldNeverBeShownToUsers() {
        assertConfiguredAsNeverShow();
    }

    @Test
    void shouldOnlyLetTheSystemUserSubmitIt() {
        assertGrants(UserRole.SYSTEM_USER, Permission.CRU);
        assertThat(configuredEvent.getGrants().asMap())
            .allSatisfy((role, permissions) -> {
                if (role != UserRole.SYSTEM_USER) {
                    assertThat(permissions).doesNotContain(Permission.C, Permission.U);
                }
            });
    }

    @Test
    void shouldCarryTheBundleAttachment() {
        assertThat(getEventFieldIds()).containsExactly("caseBundleAttachment");
    }

    @Test
    void shouldPersistNothingOnSubmit() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isNull();
    }
}

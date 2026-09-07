package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentHistoryTabAccessTest {

    private final PaymentHistoryTabAccess underTest = new PaymentHistoryTabAccess();

    @Test
    void shouldGrantReadAccessToPaymentHistoryRoles() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        for (UserRole role : AccessGrants.PAYMENT_HISTORY_READ_ROLES) {
            assertThat(grants.get(role)).containsExactly(Permission.R);
        }
    }
}

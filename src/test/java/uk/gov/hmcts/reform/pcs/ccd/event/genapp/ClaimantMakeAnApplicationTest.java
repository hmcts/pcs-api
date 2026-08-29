package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.EventStates;
import uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication.StatementOfTruth;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ClaimantMakeAnApplicationTest extends BaseEventTest {

    @Mock
    private StartEventHandler startEventHandler;
    @Mock
    private SubmitEventHandler submitEventHandler;
    @Mock
    private StatementOfTruth statementOfTruth;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new ClaimantMakeAnApplication(startEventHandler, submitEventHandler, statementOfTruth));
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(EventStates.claimantMakeAnApplication());
    }

    @Test
    void shouldGrantCaseRoleAndGroupAccessClaimantRoles() {
        assertGrants(UserRole.CLAIMANT_SOLICITOR, Permission.CRUD);
        assertGrants(UserRole.GA_CLAIMANT_SOLICITOR, Permission.CRUD);
        assertGrants(UserRole.CLAIMANT, Permission.CRUD);
    }

    @Test
    void shouldNotGrantDefendantRoles() {
        assertThat(configuredEvent.getGrants().keySet())
            .doesNotContain(UserRole.DEFENDANT, UserRole.DEFENDANT_SOLICITOR, UserRole.GA_DEFENDANT_SOLICITOR);
    }
}

package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.AmendDocumentDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.SelectDocumentPage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AmendDocumentsTest extends BaseEventTest {

    @Mock
    private DocumentAmendSelectionService documentAmendSelectionService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new AmendDocuments(
            documentAmendSelectionService,
            new SelectDocumentPage(documentAmendSelectionService),
            new AmendDocumentDetailsPage()
        ));
    }

    @Test
    void shouldConfigureBatchOneEventAccessAndNavigation() {
        assertThat(configuredEvent.getName()).isEqualTo("Manage documents: Amend");
        assertThat(configuredEvent.getPreState()).containsExactlyInAnyOrder(
            State.CASE_ISSUED,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.CASE_PROGRESSION,
            State.ALL_FINAL_ORDERS_ISSUED,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.CLOSED
        );
        assertThat(configuredEvent.isShowSummary()).isTrue();
        assertThat(configuredEvent.getEndButtonLabel()).isEqualTo("Continue");
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_TEAM_LEADER))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U);
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_ADMIN))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U);
    }
}

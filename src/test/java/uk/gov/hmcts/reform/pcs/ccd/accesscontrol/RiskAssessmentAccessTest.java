package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RiskAssessmentAccessTest {

    @Test
    void shouldGrantOnlyReadAccessToTheConfirmedInternalRoles() {
        var grants = new RiskAssessmentInternalAccess().getGrants();

        assertThat(grants.keySet()).containsExactlyInAnyOrder(
            UserRole.JUDGE, UserRole.CIRCUIT_JUDGE, UserRole.FEE_PAID_JUDGE, UserRole.LEADERSHIP_JUDGE,
            UserRole.HEARING_CENTRE_ADMIN, UserRole.HEARING_CENTRE_TEAM_LEADER,
            UserRole.CTSC_ADMIN, UserRole.CTSC_TEAM_LEADER, UserRole.WLU_ADMIN, UserRole.WLU_TEAM_LEADER,
            UserRole.BAILIFF, UserRole.BAILIFF_ADMIN, UserRole.BAILIFF_MANAGER
        );
        grants.keySet().forEach(role -> assertThat(grants.get(role)).containsExactly(Permission.R));
    }

    @Test
    void shouldGrantOnlyReadAccessToClaimantsAndTheirCaseScopedRepresentatives() {
        var grants = new RiskAssessmentClaimantAccess().getGrants();

        assertThat(grants.keySet()).containsExactlyInAnyOrder(
            UserRole.CLAIMANT, UserRole.GA_CLAIMANT_SOLICITOR, UserRole.CLAIMANT_SOLICITOR
        );
        grants.keySet().forEach(role -> assertThat(grants.get(role)).containsExactly(Permission.R));
    }

    @Test
    void shouldKeepInternalLabelsAndHtmlHiddenFromAllClaimantSideRoles() throws Exception {
        for (String field : List.of("riskAssessmentInternalMarkdown", "riskAssessmentInternalMarkdownLabel")) {
            assertThat(readers(field)).doesNotContain(
                UserRole.CLAIMANT, UserRole.CITIZEN, UserRole.DEFENDANT, UserRole.DEFENDANT_SOLICITOR,
                UserRole.GA_DEFENDANT_SOLICITOR, UserRole.PCS_SOLICITOR, UserRole.GA_CLAIMANT_SOLICITOR,
                UserRole.CLAIMANT_SOLICITOR
            );
        }
        for (String field : List.of("riskAssessmentClaimantMarkdown", "riskAssessmentClaimantMarkdownLabel")) {
            assertThat(readers(field)).containsExactlyInAnyOrder(
                UserRole.CLAIMANT, UserRole.GA_CLAIMANT_SOLICITOR, UserRole.CLAIMANT_SOLICITOR
            );
        }
    }

    @Test
    void shouldNotGrantClaimantViewToGenericIdamOrDefendantSideRoles() {
        var grants = new RiskAssessmentClaimantAccess().getGrants();

        assertThat(grants.keySet()).doesNotContain(
            UserRole.CITIZEN, UserRole.PCS_SOLICITOR, UserRole.DEFENDANT,
            UserRole.DEFENDANT_SOLICITOR, UserRole.GA_DEFENDANT_SOLICITOR,
            UserRole.DUTY_ADVISOR_REQUEST, UserRole.ORGANISATION_CASE_ACCESS_ADMINISTRATOR
        );
    }

    private Set<HasRole> readers(String fieldName) throws Exception {
        CCD annotation = PCSCase.class.getDeclaredField(fieldName).getAnnotation(CCD.class);
        Class<? extends HasAccessControl>[] access = annotation.access();
        assertThat(access).hasSize(1);
        return access[0].getDeclaredConstructor().newInstance().getGrants().keySet();
    }
}

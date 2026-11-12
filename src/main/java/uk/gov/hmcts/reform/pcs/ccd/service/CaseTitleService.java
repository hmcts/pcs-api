package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Service
@AllArgsConstructor
public class CaseTitleService {

    private static final List<String> INTERNAL_ROLES = List.of(
        UserRole.CTSC_TEAM_LEADER.getRole(),
        UserRole.CTSC_ADMIN.getRole(),
        UserRole.HEARING_CENTRE_TEAM_LEADER.getRole(),
        UserRole.HEARING_CENTRE_ADMIN.getRole(),
        UserRole.WLU_TEAM_LEADER.getRole(),
        UserRole.WLU_ADMIN.getRole(),
        UserRole.FEE_PAID_JUDGE.getRole(),
        UserRole.LEADERSHIP_JUDGE.getRole(),
        UserRole.CIRCUIT_JUDGE.getRole(),
        UserRole.JUDGE.getRole(),
        UserRole.BAILIFF_ADMIN.getRole()
    );

    private final AddressFormatter addressFormatter;
    private final UserRoleService userRoleService;

    /**
     * Builds a markdown string for the case title which appears near the top of every page in the case overview.
     * and events in ExUI.
     *
     * @param pcsCase       The current case data
     * @param caseReference The current case reference
     * @return A markdown string for the case title
     */
    public String buildCaseTitle(PCSCase pcsCase, long caseReference) {

        if (isCurrentUserInternal(caseReference)) {
            return buildInternalCaseTitle(pcsCase);
        } else {
            return buildPublicCaseTitle(pcsCase);
        }
    }

    public String buildInternalCaseTitle(PCSCase pcsCase) {

        return """
            <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                Case number: ${[CASE_REFERENCE]}
            </p>
            <p class="govuk-!-font-size-24 govuk-!-margin-bottom-1">
                Property address: %s
            </p>
            <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                ${caseNameHmctsInternal}
            </p>""".formatted(addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER));

    }

    public String buildPublicCaseTitle(PCSCase pcsCase) {

        return """
            <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                Case number: ${[CASE_REFERENCE]}
            </p>
            <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                Property address: %s
            </p>""".formatted(addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER));
    }

    private boolean isCurrentUserInternal(long caseReference) {
        UserRoles currentUserCaseRoles = userRoleService.getCurrentUserCaseRoles(caseReference);
        return currentUserCaseRoles.roles().stream().anyMatch(INTERNAL_ROLES::contains);
    }

}

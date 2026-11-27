package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

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
    private final FeatureToggleService featureToggleService;

    /**
     * Builds a markdown string for the case title which appears near the top of every page in the case overview.
     * and events in ExUI.
     *
     * @param pcsCase       The current case data
     * @param caseReference The current case reference
     * @return A markdown string for the case title
     */
    public String buildCaseTitle(PCSCase pcsCase, long caseReference) {

        if (isCurrentUserInternal(caseReference)
            && featureToggleService.isEnabled(FeatureFlag.BAILIFF_MANAGEMENT_ENABLED)) {
            return buildInternalCaseTitle(pcsCase);
        } else {
            return buildDefaultCaseTitle(pcsCase);
        }
    }

    private String buildInternalCaseTitle(PCSCase pcsCase) {

        return """
            <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                Case number: ${[CASE_REFERENCE]}
            </p>
            <p class="govuk-!-font-size-24 govuk-!-margin-bottom-1">
                Property address: %s
            </p>
            <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                ${caseNameHmctsInternal}
            </p>%s""".formatted(addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER),
                                  caseBundleLink(pcsCase));
    }

    private String buildDefaultCaseTitle(PCSCase pcsCase) {

        return """
            <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                Case number: ${[CASE_REFERENCE]}
            </p>
            <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                Property address: %s
            </p>%s""".formatted(addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER),
                                caseBundleLink(pcsCase));
    }

    private boolean isCurrentUserInternal(long caseReference) {
        UserRoles currentUserCaseRoles = userRoleService.getCurrentUserCaseRoles(caseReference);
        return currentUserCaseRoles.roles().stream().anyMatch(INTERNAL_ROLES::contains);
    }

    private String caseBundleLink(PCSCase pcsCase) {
        // CaseBundlesView supplies the bundles only to judges targeted by case-bundle-view-enabled.
        var bundles = pcsCase.getCaseBundles();
        if (bundles == null || bundles.isEmpty()) {
            return "";
        }
        String url = bundles.getFirst().getValue().getUrl();
        return """

                <p class="govuk-body govuk-!-margin-top-2 govuk-!-margin-bottom-0">
                    <a class="govuk-link" href="/documentsv2/%s/binary" target="_blank" rel="noopener noreferrer">Whole \
                file download (opens in new tab)</a>
                </p>""".formatted(url.substring(url.lastIndexOf('/') + 1));
    }


}

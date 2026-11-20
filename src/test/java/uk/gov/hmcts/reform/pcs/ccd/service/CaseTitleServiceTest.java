package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseTitleServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private PCSCase pcsCase;

    private CaseTitleService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseTitleService(addressFormatter, userRoleService);
    }


    @Test
    void shouldGenerateCaseTitleWithCaseNameForInternalUsers() {
        // Given
        stubUserRole(UserRole.JUDGE);

        // When
        String actualCaseTitle = underTest.buildCaseTitle(pcsCase, CASE_REFERENCE);

        // Then
        String expectedCaseTitle = """
              <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                  Case number: ${[CASE_REFERENCE]}
              </p>
              <p class="govuk-!-font-size-24 govuk-!-margin-bottom-1">
                  Property address: null
              </p>
              <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                  ${caseNameHmctsInternal}
              </p>""";
        assertThat(actualCaseTitle).isEqualTo(expectedCaseTitle);
    }

    @Test
    void shouldGenerateCaseTitleWithoutCaseNameForOtherUsers() {
        // Given
        stubUserRole(UserRole.CLAIMANT);

        // When
        String actualCaseTitle = underTest.buildCaseTitle(pcsCase, CASE_REFERENCE);

        // Then
        String expectedCaseTitle = """
              <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                  Case number: ${[CASE_REFERENCE]}
              </p>
              <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                  Property address: null
              </p>""";
        assertThat(actualCaseTitle).isEqualTo(expectedCaseTitle);
    }

    private void stubUserRole(UserRole userRole) {
        UserRoles userRoles = new UserRoles("some-user-id", List.of(userRole.getRole()));
        when(userRoleService.getCurrentUserCaseRoles(CASE_REFERENCE))
            .thenReturn(userRoles);
    }

}

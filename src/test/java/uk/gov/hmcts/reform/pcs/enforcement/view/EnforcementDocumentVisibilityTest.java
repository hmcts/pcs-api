package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentVisibilityTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private EnforcementDocumentVisibility underTest;

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DEFENDANT", "DEFENDANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"})
    void shouldHideEnforcementDocumentsFromDefendants(UserRole defendantRole) {
        // Given
        ListValue<Document> claimForm = document("1", "claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE.getId());
        ListValue<Document> evidence = document("2", "evidence.pdf", FeatureCaseFileCategory.ENFORCEMENT.getId());
        PCSCase pcsCase = PCSCase.builder().allDocuments(List.of(claimForm, evidence)).build();
        givenCurrentUserHasRoles("citizen", defendantRole.getRole());

        // When
        underTest.hideFromDefendants(pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "CREATOR", "CLAIMANT_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT",
        "JUDGE", "CTSC_ADMIN", "HEARING_CENTRE_ADMIN", "PCS_CASE_WORKER", "SYSTEM_USER"
    })
    void shouldShowEnforcementDocumentsToEveryoneElse(UserRole role) {
        // Given
        ListValue<Document> claimForm = document("1", "claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE.getId());
        ListValue<Document> evidence = document("2", "evidence.pdf", FeatureCaseFileCategory.ENFORCEMENT.getId());
        PCSCase pcsCase = PCSCase.builder().allDocuments(List.of(claimForm, evidence)).build();
        givenCurrentUserHasRoles(role.getRole());

        // When
        underTest.hideFromDefendants(pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm, evidence);
    }

    @Test
    void shouldNotLookUpRolesWhenThereAreNoEnforcementDocuments() {
        // Given
        ListValue<Document> claimForm = document("1", "claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE.getId());
        PCSCase pcsCase = PCSCase.builder().allDocuments(List.of(claimForm)).build();

        // When
        underTest.hideFromDefendants(pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void shouldLeaveACaseWithNoDocumentsAlone() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.hideFromDefendants(pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).isNull();
        verifyNoInteractions(userRoleService);
    }

    private void givenCurrentUserHasRoles(String... roles) {
        when(userRoleService.getCurrentUserCaseRoles(CASE_REFERENCE))
            .thenReturn(new UserRoles("user-id", List.of(roles)));
    }

    private static ListValue<Document> document(String id, String filename, String categoryId) {
        return ListValue.<Document>builder()
            .id(id)
            .value(Document.builder()
                       .filename(filename)
                       .url("http://dm-store/documents/" + id)
                       .categoryId(categoryId)
                       .build())
            .build();
    }
}

package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffAllocationDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.StaffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@ExtendWith(MockitoExtension.class)
class FindBailiffPageTest extends BasePageTest {

    @Mock
    private BailiffDirectoryService bailiffDirectoryService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new FindBailiffPage(bailiffDirectoryService));
    }

    @Test
    void shouldAskForTheBailiffWithTheStaffUserTypeAhead() {
        Field<?, ?, ?, ?> bailiff = field("allocatedBailiff");

        assertThat(bailiff.getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(bailiff.getDisplayContextParameter()).isEqualTo("#ARGUMENT(CATEGORY-ADMIN)");
        assertThat(bailiff.getPage()).isEqualTo("findBailiff");
        assertThat(event.getFields().getPageLabels()).containsEntry("findBailiff", "Find the bailiff's name");
    }

    @Test
    void shouldCarryTheCaseNameForTheHeaderWithoutShowingIt() {
        Field<?, ?, ?, ?> caseName = field("bailiffAllocationCaseName");

        assertThat(caseName.getContext()).isEqualTo(DisplayContext.ReadOnly);
        assertThat(caseName.getShowCondition()).isEqualTo(NEVER_SHOW);
    }

    @Test
    void shouldShowTheEmailOnceItIsKnown() {
        Field<?, ?, ?, ?> email = field("allocatedBailiffEmail");

        assertThat(email.getContext()).isEqualTo(DisplayContext.ReadOnly);
        assertThat(email.getShowCondition()).isEqualTo("allocatedBailiff.idamId!=\"\"");
    }

    @Test
    void shouldAskForABailiffWhenNoneIsChosen() {
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseWith(null));

        assertThat(response.getErrors()).containsExactly("Choose a bailiff");
        verifyNoInteractions(bailiffDirectoryService);
    }

    @Test
    void shouldRefuseAUserWhoIsNotABailiff() {
        when(bailiffDirectoryService.findBailiff("alex-id")).thenReturn(Optional.empty());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseWith("alex-id"));

        assertThat(response.getErrors()).containsExactly("Choose a user who has the bailiff role");
    }

    @Test
    void shouldLookUpTheBailiffsEmailForCheckYourAnswers() {
        when(bailiffDirectoryService.findBailiff("sam-id"))
            .thenReturn(Optional.of(new BailiffUser("sam-id", "Sam Jones", "sam.jones@justice.gov.uk")));

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseWith("sam-id"));

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getBailiffAllocationDetails().getAllocatedBailiffEmail())
            .isEqualTo("sam.jones@justice.gov.uk");
    }

    @Test
    void shouldTreatABlankIdamIdAsNoChoice() {
        BailiffAllocationDetails details = BailiffAllocationDetails.builder().allocatedBailiff(new StaffUser(" "))
            .build();

        assertThat(FindBailiffPage.chosenUserId(details)).isNull();
        assertThat(FindBailiffPage.chosenUserId(null)).isNull();
    }

    private static PCSCase caseWith(String userId) {
        if (userId == null) {
            return PCSCase.builder().build();
        }
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .bailiffAllocationDetails(BailiffAllocationDetails.builder()
                                                                .allocatedBailiff(new StaffUser(userId))
                                                                .build())
                                  .build())
            .build();
    }

    private Field<?, ?, ?, ?> field(String id) {
        return allFields().filter(field -> id.equals(field.getId())).findFirst().orElseThrow();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Stream<Field<?, ?, ?, ?>> allFields() {
        Stream<FieldCollection> complexFields = event.getFields().getComplexFields().stream()
            .map(builder -> ((FieldCollectionBuilder) builder).build());
        return Stream.concat(Stream.of(event.getFields()), complexFields)
            .flatMap(collection -> ((FieldCollection) collection).getFields().stream())
            .map(builder -> (Field<?, ?, ?, ?>) ((Field.FieldBuilder) builder).build());
    }
}

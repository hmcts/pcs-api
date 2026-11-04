package uk.gov.hmcts.reform.pcs.enforcement.ccd.page;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction.ConfirmEvictionDetailsPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.BreathingSpacePage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.ConfirmIfDOBKnownPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.EvictionDelayWarningPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.LanguageUsedPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.LivingInThePropertyPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.MissedPaymentsPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantofrestitution.AnythingElseToHelpTheEvictionPlaceholder;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantofrestitution.DefendantAtPropertyPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantofrestitution.PeopleYouWantToEvictWarrantRestPlaceholder;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantofrestitution.ShareEvidenceWithJudgePage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.BreathingSpaceWritPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.ConfirmHCEOfficerPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.EnforcementOfficerSelectionPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.HCEOfficerDetailsPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.LanguageUsedWritPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.MissedPaymentsWritPage;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DeclarativeEnforcementPagesTest extends BasePageTest {

    private static Stream<Arguments> pages() {
        return Stream.of(
            arguments(new BreathingSpacePage(), "defendantInBreathingSpace"),
            arguments(new MissedPaymentsPage(), "missedPayments"),
            arguments(new LivingInThePropertyPage(), "livingInTheProperty"),
            arguments(new ConfirmIfDOBKnownPage(), "confirmDefendantsDOB"),
            arguments(new LanguageUsedPage(), "languageUsed"),
            arguments(new EvictionDelayWarningPage(), "evictionDelayWarning"),
            arguments(new BreathingSpaceWritPage(), "defendantInBreathingSpaceWrit"),
            arguments(new MissedPaymentsWritPage(), "missedPaymentsWrit"),
            arguments(new LanguageUsedWritPage(), "languageUsedWrit"),
            arguments(new EnforcementOfficerSelectionPage(), "enforcementOfficerSelection"),
            arguments(new ConfirmHCEOfficerPage(), "confirmHCEOfficer"),
            arguments(new HCEOfficerDetailsPage(), "hCEOfficerDetails"),
            arguments(new DefendantAtPropertyPage(), "defendantAtProperty"),
            arguments(new ShareEvidenceWithJudgePage(), "shareEvidenceWithJudge"),
            arguments(new PeopleYouWantToEvictWarrantRestPlaceholder(), "peopleYouWantToEvictWarrantRest"),
            arguments(new AnythingElseToHelpTheEvictionPlaceholder(), "anythingElseToHelpTheEviction"),
            arguments(new ConfirmEvictionDetailsPage(), "confirmEvictionDetails")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("pages")
    void shouldPlaceEveryFieldOnItsOwnPage(CcdPageConfiguration page, String pageId) {
        setPageUnderTest(page);

        List<Field<?, ?, ?, ?>> fields = placedFields();

        assertThat(fields).isNotEmpty();
        assertThat(fields).allSatisfy(field -> assertThat(field.getPage())
            .withFailMessage("%s is placed on page %s, not %s", field.getId(), field.getPage(), pageId)
            .isEqualTo(pageId));
        assertFieldsArePlacedBeforeTheLabelsThatShowThem();
    }
}

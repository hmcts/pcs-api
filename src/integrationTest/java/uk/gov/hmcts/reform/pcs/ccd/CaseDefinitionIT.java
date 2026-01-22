package uk.gov.hmcts.reform.pcs.ccd;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.CCDDefinitionGenerator;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
@SuppressWarnings({"rawtypes"})
class CaseDefinitionIT extends AbstractPostgresContainerIT {

    @Autowired
    private CCDDefinitionGenerator ccdDefinitionGenerator;

    @ParameterizedTest
    @MethodSource("saveAndResumeEventScenarios")
    void eventPagesShouldHaveSaveAndResumeHintText(EventId eventId, List<String> excludedPageIds) {
        Map<String, List<Field>> pageFieldsMap = getPageToFieldsMap(eventId);

        SoftAssertions softAssertions = new SoftAssertions();

        pageFieldsMap.entrySet()
            .stream()
            .filter(entry -> !excludedPageIds.contains(entry.getKey()))
            .forEach(entry -> {
                String pageId = entry.getKey();
                List<Field> pageFields = entry.getValue();

                boolean hintTextFound = containsSaveAndResumeHint(pageFields);
                softAssertions.assertThat(hintTextFound)
                    .withFailMessage("Page %s should have save and resume hint", pageId)
                    .isTrue();
            });

        softAssertions.assertAll();
    }

    private static Stream<Arguments> saveAndResumeEventScenarios() {
        return Stream.of(
            // Event ID, Excluded Page IDs
            arguments(
                EventId.resumePossessionClaim,
                List.of(
                    "startTheService",
                    "resumeClaim",
                    "claimantTypeNotEligibleEngland",
                    "claimantTypeNotEligibleWales",
                    "claimTypeNotEligibleEngland",
                    "claimTypeNotEligibleWales"
                )
            ),

            arguments(
                EventId.enforceTheOrder,
                List.of(
                    "startTheService",
                    "evictionDelayWarning",
                    "checkYourAnswersPlaceHolder",
                    "changeNameAddress"
                )

            )
        );
    }

    private Map<String, List<Field>> getPageToFieldsMap(EventId eventId) {
        ResolvedCCDConfig<PCSCase, State, UserRole> ccdConfig = getCCDConfig();

        Event<PCSCase, UserRole, State> event = ccdConfig.getEvents().get(eventId.name());
        assertThat(event).isNotNull();

        FieldCollection fieldCollection = event.getFields();

        return fieldCollection.getFields().stream()
            .map(Field.FieldBuilder::build)
            .collect(Collectors.groupingBy(Field::getPage));
    }

    @SuppressWarnings("unchecked")
    private ResolvedCCDConfig<PCSCase, State, UserRole> getCCDConfig() {
        return (ResolvedCCDConfig<PCSCase, State, UserRole>) ccdDefinitionGenerator.loadConfigs().getFirst();
    }

    private boolean containsSaveAndResumeHint(List<Field> pageFields) {
        return pageFields.stream()
            .anyMatch(field -> CommonPageContent.SAVE_AND_RETURN.equals(field.getLabel()));
    }


}

package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.MandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SecureOrFlexibleGroundsForPossessionTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new SecureOrFlexibleGroundsForPossession());
    }

    @ParameterizedTest
    @MethodSource("groundsScenarios")
    void shouldMergeDynamicMultiSelectListsIntoMultiSelectLists(
            List<DiscretionaryGrounds> discretionaryList1,
            List<DiscretionaryGrounds> discretionaryList2,
            List<MandatoryGrounds> mandatoryList1,
            List<MandatoryGrounds> mandatoryList2,
            boolean expectError,
            YesOrNo expectedShowReasonsPage) {

        // Given
        PCSCase caseData = PCSCase.builder()
                .secureOrFlexibleDiscretionaryGrounds(toDynamicMultiSelectList(discretionaryList1))
                .secureOrFlexibleDiscretionaryGroundsAlternativeAccommodation(
                        toDynamicMultiSelectList(discretionaryList2))
                .secureOrFlexibleMandatoryGrounds(toDynamicMultiSelectList(mandatoryList1))
                .secureOrFlexibleMandatoryGroundsAlternativeAccommodation(toDynamicMultiSelectList(mandatoryList2))
                .build();

        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
                getMidEventForPage(event, "secureOrFlexibleGroundsForPossession")
                        .handle(caseDetails, null);

        // Then
        if (expectError) {
            assertThat(response.getErrors()).containsExactly("Please select at least one ground");
        } else {
            Set<DiscretionaryGrounds> expectedDiscretionary = new HashSet<>(discretionaryList1);
            expectedDiscretionary.addAll(discretionaryList2);

            Set<MandatoryGrounds> expectedMandatory = new HashSet<>(mandatoryList1);
            expectedMandatory.addAll(mandatoryList2);

            assertThat(caseData.getSelectedSecureOrFlexibleDiscretionaryGrounds()).isEqualTo(expectedDiscretionary);
            assertThat(caseData.getSelectedSecureOrFlexibleMandatoryGrounds()).isEqualTo(expectedMandatory);
            assertThat(response.getErrors()).isNull();
            assertThat(caseData.getShowReasonsForGroundsPage()).isEqualTo(expectedShowReasonsPage);
        }
    }

    private <T extends HasLabel> DynamicMultiSelectList toDynamicMultiSelectList(List<T> grounds) {
        if (grounds == null || grounds.isEmpty()) {
            return DynamicMultiSelectList.builder().value(Collections.emptyList()).build();
        }
        List<DynamicListElement> elements = grounds.stream()
                .map(g -> new DynamicListElement(UUID.randomUUID(), g.getLabel()))
                .collect(Collectors.toList());

        return DynamicMultiSelectList.builder().value(elements).build();
    }

    private static Stream<Arguments> groundsScenarios() {
        return Stream.of(
                arguments(
                        List.of(DiscretionaryGrounds.RIOT_OFFENCE),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        YesOrNo.YES
                ),

                arguments(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        true,
                        null),

                arguments(
                        List.of(DiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE),
                        List.of(DiscretionaryGrounds.DOMESTIC_VIOLENCE),
                        List.of(), List.of(MandatoryGrounds.OVERCROWDING),
                        false,
                        YesOrNo.YES
                ),

                arguments(List.of(), List.of(),
                        List.of(MandatoryGrounds.PROPERTY_SOLD),
                        List.of(MandatoryGrounds.CHARITABLE_LANDLORD),
                        false,
                        YesOrNo.YES
                ),

                arguments(List.of(DiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        YesOrNo.NO
                )
        );
    }

}




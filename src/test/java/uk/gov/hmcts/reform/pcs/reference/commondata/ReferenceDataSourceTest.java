package uk.gov.hmcts.reform.pcs.reference.commondata;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDataSourceTest {

    @Test
    void shouldMapEveryHearingReferenceTableToItsCategory() {
        // given
        Map<String, String> expected = Map.ofEntries(
            Map.entry("judge_type", "JudgeType"),
            Map.entry("hearing_channel", "HearingChannel"),
            Map.entry("hearing_priority", "HearingPriority"),
            Map.entry("hearing_case_subtype", "caseSubType"),
            Map.entry("facilities", "Facilities"),
            Map.entry("auto_list_change_reasons", "AutoListChangeReasons"),
            Map.entry("additional_roles", "AdditionalRoles"),
            Map.entry("actual_part_heard_reason", "ActualPartHeardReasonCodes"),
            Map.entry("change_reasons", "ChangeReasons"),
            Map.entry("listing_status", "ListingStatus"),
            Map.entry("actual_cancellation_reason", "ActualCancellationReasonCodes"),
            Map.entry("case_management_cancellation_reason", "CaseManagementCancellationReasons"),
            Map.entry("hearing_type", "HearingType"),
            Map.entry("sign_language", "SignLanguage"),
            Map.entry("unavailable_type", "UnavailableType"),
            Map.entry("hearing_case_type", "caseType"),
            Map.entry("interpreter_language", "InterpreterLanguage"),
            Map.entry("hearing_subchannel", "HearingSubChannel"),
            Map.entry("custody_status", "CustodyStatus"),
            Map.entry("party_relationship_type", "PartyRelationshipType")
        );

        // when
        Map<String, String> actual = Arrays.stream(ReferenceDataSource.values())
            .collect(Collectors.toMap(ReferenceDataSource::getTableName, ReferenceDataSource::getCategoryId));

        // then
        assertThat(actual).isEqualTo(expected);
    }
}

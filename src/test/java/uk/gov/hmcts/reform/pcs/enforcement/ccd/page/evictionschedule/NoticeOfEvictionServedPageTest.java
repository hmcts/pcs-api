package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

class NoticeOfEvictionServedPageTest extends BasePageTest {

    private static final Instant NOW = Instant.parse("2026-07-08T09:00:00Z");

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NoticeOfEvictionServedPage(new EvictionScheduleValidator(Clock.fixed(NOW, UK_ZONE_ID))));
    }

    @Test
    void shouldAskWhenAndHowTheNoticeWasServedAndOfferABailiffNote() {
        Map<String, Field<?, ?, ?, ?>> fields = allFields()
            .collect(Collectors.toMap(Field::getId, Function.identity()));

        assertThat(fields.get("noticeOfEvictionServedAt").getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(fields.get("noticeOfEvictionServedHow").getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(fields.get("noticeOfEvictionAddBailiffNote").getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(fields.get("bailiffNoteText").getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(fields.get("bailiffNoteText").getShowCondition())
            .isEqualTo("noticeOfEvictionAddBailiffNote=\"Yes\"");
        assertThat(fields).doesNotContainKey("evictionDispenseWithService");
        assertThat(event.getFields().getPageLabels())
            .containsEntry("markNoticeOfEvictionServed", "Mark 'notice of eviction' as served");
    }

    @Test
    void shouldContinueWhenServedInThePast() {
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            callMidEventHandler(caseWith(LocalDateTime.parse("2026-07-07T16:00"), YesOrNo.YES, "Spoke to occupant"));

        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    void shouldStopWhenServedInTheFuture() {
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            callMidEventHandler(caseWith(LocalDateTime.parse("2026-07-08T16:00"), YesOrNo.NO, null));

        assertThat(response.getErrors())
            .containsExactly("The date and time you served the 'notice of eviction' cannot be in the future");
    }

    @Test
    void shouldStopWithNoAnswers() {
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).hasSize(3);
    }

    private static PCSCase caseWith(LocalDateTime servedAt, YesOrNo addNote, String note) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .bailiffNoteText(note)
                                  .evictionSchedule(EvictionSchedule.builder()
                                                        .noticeOfEvictionServedAt(servedAt)
                                                        .noticeOfEvictionServedHow("by hand")
                                                        .noticeOfEvictionAddBailiffNote(addNote)
                                                        .build())
                                  .build())
            .build();
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

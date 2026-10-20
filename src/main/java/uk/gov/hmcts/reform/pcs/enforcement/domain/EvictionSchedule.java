package uk.gov.hmcts.reform.pcs.enforcement.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.InternalTabAccess;

import java.time.LocalDateTime;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvictionSchedule {

    public static final int NOTICE_SERVED_HOW_MAX_LENGTH = 500;

    @CCD(searchable = false)
    private String evictionDetailsMarkup;

    @CCD(
        label = "Eviction date and time",
        hint = "Use the 24-hour clock. For example, 25 6 2026, 10 00",
        searchable = false
    )
    private LocalDateTime evictionDateTime;

    @CCD(label = "Has the court given permission to dispense with service?", searchable = false)
    private YesOrNo evictionDispenseWithService;

    @CCD(
        label = "When did you serve the 'notice of eviction'?",
        hint = "Use the 24-hour clock. For example, 7 7 2026, 16 00",
        searchable = false
    )
    private LocalDateTime noticeOfEvictionServedAt;

    @CCD(
        label = "How did you serve the 'notice of eviction'?",
        typeOverride = TextArea,
        max = NOTICE_SERVED_HOW_MAX_LENGTH,
        searchable = false
    )
    private String noticeOfEvictionServedHow;

    @CCD(label = "Do you want to add a bailiff note?", searchable = false)
    private YesOrNo noticeOfEvictionAddBailiffNote;

    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canScheduleEviction;

    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canRescheduleEviction;

    @CCD(searchable = false, access = {InternalTabAccess.class})
    private YesOrNo canMarkNoticeOfEvictionServed;
}

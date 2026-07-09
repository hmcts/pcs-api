package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;

import java.time.LocalDateTime;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicMultiSelectList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class Hearing {

    public static final String NOTES_LABEL = "Hearing notes";
    public static final String ADDITIONAL_INFORMATION_LABEL = "Enter any additional information";

    @CCD(label = "Which type of hearing is this?")
    private HearingType type;

    @CCD(
        label = "Enter the type of hearing this is",
        max = 100
    )
    private String otherHearingType;

    @CCD(
        label = "Wording for hearing notice",
        typeOverride = FixedList,
        typeParameterOverride = "HearingNoticeWording"
    )
    private HearingNoticeWording noticeWording;

    @CCD(
        label = "When is the hearing?",
        hint = "Enter date and time"
    )
    private LocalDateTime date;

    @CCD(
        label = "Hour",
        max = 23
    )
    private Integer durationHours;

    @CCD(
        label = "Minute",
        max = 59
    )
    private Integer durationMinutes;

    @CCD(
        label = NOTES_LABEL,
        typeOverride = TextArea
    )
    private String notes;

    @CCD(label = "Does a hearing notice need to be issued")
    private VerticalYesNo noticeIssued;

    @CCD(label = "Is the hearing without notice?")
    private VerticalYesNo isWithoutNotice;

    @CCD(
        label = ADDITIONAL_INFORMATION_LABEL,
        hint = "This information will be displayed on the hearing notice",
        typeOverride = TextArea
    )
    private String additionalInformation;
}

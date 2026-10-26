package uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class BreathingSpaceDetails {

    private static final int MAX_REFERENCE_NUMBER_LENGTH = 14;

    @CCD(
        label = "Reference Number",
        max = MAX_REFERENCE_NUMBER_LENGTH
    )
    private String referenceNumber;

    @CCD(
        label = "When did breathing space start?",
        hint = "You should enter the date it started, not the date you received notification of it.  "
            + "For example, 15 11 2020"
    )
    private LocalDate startDate;

    @CCD(label = "What type of Breathing Space is it?")
    private BreathingSpaceType type;

    @CCD(
        label = "When is the expected end date?",
        hint = "For example, 17 11 2020"
    )
    private LocalDate expectedEndDate;

}

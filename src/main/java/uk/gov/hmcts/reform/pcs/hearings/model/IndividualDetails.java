package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor 
public class IndividualDetails {

    private String firstName;

    private String lastName;

    private String preferredHearingChannel;

    private String interpreterLanguage;

    private List<String> reasonableAdjustments;

    private Boolean vulnerableFlag;

    private String vulnerabilityDetails;

    private List<String> hearingChannelEmail;

    private List<String> hearingChannelPhone;

    private String custodyStatus;

    private String otherReasonableAdjustmentDetails;

}
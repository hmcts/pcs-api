package uk.gov.hmcts.reform.pcs.hearings.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class ScreenNavigation {
    private String screenName;
    private String conditionKey;
    private List<Navigation> navigation;
}

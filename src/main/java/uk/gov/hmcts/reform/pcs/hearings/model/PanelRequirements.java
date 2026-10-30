package uk.gov.hmcts.reform.pcs.hearings.model;

import java.util.List;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder 
@Data
@NoArgsConstructor
@AllArgsConstructor 
public class PanelRequirements {

    private List<String> roleType;
}

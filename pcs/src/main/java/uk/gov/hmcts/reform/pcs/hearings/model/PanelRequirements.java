package uk.gov.hmcts.reform.pcs.hearings.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PanelRequirements {

    private List<String> roleType;

    private List<String> authorisationTypes;

    private List<String> authorisationSubType;

    private List<PanelPreference> panelPreferences;

    private List<String> panelSpecialisms;
}

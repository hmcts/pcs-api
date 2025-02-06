package uk.gov.hmcts.reform.pcs.hearings.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PanelPreference {

    private String memberID;

    private String memberType;

    private String requirementType;
}

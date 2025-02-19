package uk.gov.hmcts.reform.pcs.hearings.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CaseCategory {

    private String categoryType;

    private String categoryValue;

    private String categoryParent;
}

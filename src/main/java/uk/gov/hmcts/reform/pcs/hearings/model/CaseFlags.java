package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseFlags {

    private List<PartyFlags> flags;
    private String flagAmendURL;
}


package uk.gov.hmcts.reform.pcs.postcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCodeResponse {

    private int epimId;
    private List<CourtVenue> courtVenues;

}

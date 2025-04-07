package uk.gov.hmcts.reform.pcs.postcode.adapter;

import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;

import java.util.List;

public interface LocationReferenceDataAdapter {

    List<CourtVenue> fetchCountyCourts(int epimmsId);
}

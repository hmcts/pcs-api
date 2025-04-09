package uk.gov.hmcts.reform.pcs.postcode.adapter;

import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;

import java.util.List;

public interface LocationReferenceDataAdapter {

    List<CourtVenue> fetchCountyCourts(int epimmsId);

    Mono<List<CourtVenue>> fetchCountyCourt(int epimmsId);
}

package uk.gov.hmcts.reform.pcs.ccd.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.util.List;
import java.util.Optional;

/** The court managing a case, from location reference data; none when the case has none or it can't be found. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CaseCourts {

    private final LocationReferenceService locationReferenceService;

    public Optional<CourtVenue> of(PcsCaseEntity pcsCase) {
        if (pcsCase.getCaseManagementLocation() == null) {
            return Optional.empty();
        }
        try {
            return locationReferenceService.getCourtVenues(List.of(pcsCase.getCaseManagementLocation()))
                .stream().findFirst();
        } catch (Exception e) {
            log.warn("No court venue for case {}", pcsCase.getCaseReference(), e);
            return Optional.empty();
        }
    }
}

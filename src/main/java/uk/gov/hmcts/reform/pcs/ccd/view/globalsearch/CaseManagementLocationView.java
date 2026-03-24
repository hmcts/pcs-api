package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class CaseManagementLocationView {


    /**
     * Builds a formatted string for the case management location field
     * based on epimsId and regionId.
     *
     * @param pcsCase The current case data
     */
    public void setCaseManagementLocationField(final PCSCase pcsCase) {
        final Integer epimsId = pcsCase.getCaseManagementLocation();
        final Integer region = pcsCase.getRegionId();

        if (epimsId != null && region != null) {
            pcsCase.setCaseManagementLocationFormatted(getFormattedValue(region, epimsId));
        }
    }


    private String getFormattedValue(final Integer region, final Integer epimsId) {
        return String.format("{region:%s,baseLocation:%s}", region, epimsId);
    }
}

package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.CaseLocation;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CaseFieldsView {

    private final CaseNameFormatter caseNameFormatter;

    @Value("${globalsearch.caseManagementCategory}")
    private String caseManagementCategory;

    /**
     * Sets case fields for the pcsCase.
     * @param pcsCase The current case data
     */
    public void setCaseFields(final PCSCase pcsCase) {
        setCaseNameHmctsField(pcsCase);
        setCaseManagementLocationField(pcsCase);
        setCaseManagementCategory(pcsCase);
    }

    private void setCaseNameHmctsField(final PCSCase pcsCase) {
        final String formattedCaseName = caseNameFormatter.formatCaseName(pcsCase);

        pcsCase.setCaseNameHmctsRestricted(formattedCaseName);
        pcsCase.setCaseNameHmctsInternal(formattedCaseName);
        pcsCase.setCaseNamePublic(formattedCaseName);
    }


    private void setCaseManagementLocationField(final PCSCase pcsCase) {
        Integer epimsId = pcsCase.getCaseManagementLocationNumber();
        Integer region = pcsCase.getRegionId();

        if (epimsId != null && region != null) {
            pcsCase.setCaseManagementLocation(CaseLocation.builder()
                .baseLocation(String.valueOf(epimsId))
                .region(String.valueOf(region))
                .build());
        }
    }

    private void setCaseManagementCategory(PCSCase pcsCase) {
        DynamicListElement listElement = DynamicListElement.builder()
            .code(UUID.randomUUID())
            .label(caseManagementCategory)
            .build();
        List<DynamicListElement> caseManagementCategoryList = List.of(listElement);

        pcsCase.setCaseManagementCategory(DynamicList.builder()
            .value(listElement)
            .listItems(caseManagementCategoryList)
            .build());
    }
}

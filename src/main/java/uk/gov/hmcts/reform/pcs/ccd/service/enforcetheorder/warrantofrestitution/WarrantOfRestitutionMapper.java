package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;

@Component
@AllArgsConstructor
public class WarrantOfRestitutionMapper {

    public void prePopulateFieldsFromWarrantDetails(EnforcementOrder warrantEnforcementOrder,
                                                    EnforcementOrder current) {
        WarrantDetails warrantDetails = warrantEnforcementOrder.getWarrantDetails();

        WarrantOfRestitutionDetails warrantRestDetails = new WarrantOfRestitutionDetails();
        warrantRestDetails.setAnyRiskToBailiff(warrantDetails.getAnyRiskToBailiff());
        warrantRestDetails.setRiskCategories(warrantDetails.getRiskCategories());
        warrantRestDetails.setRiskDetails(warrantDetails.getRiskDetails());
        warrantRestDetails.setPropertyAccessDetails(warrantDetails.getPropertyAccessDetails());
        current.setWarrantOfRestitutionDetails(warrantRestDetails);

        RawWarrantDetails rawWarrantDetails = warrantEnforcementOrder.getRawWarrantDetails();

        RawWarrantRestDetails rawWarrantRestDetails = new RawWarrantRestDetails();
        rawWarrantRestDetails.setSelectedDefendants(rawWarrantDetails.getSelectedDefendants());
        rawWarrantRestDetails.setVulnerablePeoplePresent(rawWarrantDetails.getVulnerablePeoplePresent());
        rawWarrantRestDetails.setVulnerableAdultsChildren(rawWarrantDetails.getVulnerableAdultsChildren());
        current.setRawWarrantRestDetails(rawWarrantRestDetails);
    }
}

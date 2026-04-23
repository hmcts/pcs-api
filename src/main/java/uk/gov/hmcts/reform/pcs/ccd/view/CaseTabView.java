package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.CasePartiesTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;

@Component
public class CaseTabView {
    public void setCaseTabFields(PCSCase pcsCase) {
        setCasePartiesTabFields(pcsCase);
    }

    private void setCasePartiesTabFields(PCSCase pcsCase) {
        if (pcsCase.getAllClaimants() != null && !pcsCase.getAllClaimants().isEmpty()) {
            Party claimant = pcsCase.getAllClaimants().getFirst().getValue();
            ClaimantTabDetails claimantTabDetails = ClaimantTabDetails.builder()
                .name(claimant.getOrgName())
                .emailAddress(claimant.getEmailAddress())
                .serviceAddress(claimant.getAddress())
                .telephoneNumber(claimant.getPhoneNumber())
                .build();
            CasePartiesTab tab = CasePartiesTab.builder()
                .claimantDetails(claimantTabDetails)
                .build();
            pcsCase.setCasePartiesTab(tab);
        }
    }
}

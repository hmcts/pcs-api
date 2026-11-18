package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RepaymentPreference;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.client.CcdClient;

@Service
@RequiredArgsConstructor
public class EnforcementApplicationCreationService {

    private final CcdClient ccdClient;

    public void createMinimalWarrantApplication(long caseReference, String authorisation) {
        DynamicStringList enforcementTypeChoice = DynamicStringList.builder()
            .value(DynamicStringListElement.builder().code(SelectEnforcementType.WARRANT.name()).build())
            .build();

        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.CLAIMANT);
        statementOfTruth.setFullNameParty("Claimant party name");
        statementOfTruth.setPositionParty("Claimant party position");

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .repaymentCosts(RepaymentCosts.builder()
                                .repaymentChoice(RepaymentPreference.ALL)
                                .build())
            .statementOfTruth(statementOfTruth)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .chooseEnforcementType(enforcementTypeChoice)
            .warrantDetails(warrantDetails)
            .build();

        PCSCase eventData = PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();

        ccdClient.updateCase(EventId.enforceTheOrder, caseReference, eventData, authorisation);
    }

}

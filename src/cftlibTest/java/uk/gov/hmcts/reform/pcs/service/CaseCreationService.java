package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@Service
@RequiredArgsConstructor
public class CaseCreationService {

    private final CcdClient ccdClient;

    public long createMinimalCase(String authorisation) {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .postTown("London")
                                 .postCode("NW1 6XE")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(caseData, authorisation);

        long caseReference = caseDetails.getId();

        caseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .claimantInformation(ClaimantInformation.builder().claimantName("TreeTops Housing").build())
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Danny")
                            .lastName("Defendant")
                            .build())
            .noticeServed(YesOrNo.NO)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build();

        ccdClient.updateCase(resumePossessionClaim, caseReference, caseData, authorisation);

        return caseReference;
    }

}

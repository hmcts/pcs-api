package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInformationTabDetails;

@Component
public class DefendantInformationTabDetailsBuilder extends DefendantInformationBuilder {

    public DefendantInformationTabDetails buildDefendantOneDetails(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants())) {
            return null;
        }

        return createSummaryDefendantDetails(pcsCase.getAllDefendants().getFirst().getValue(), pcsCase);
    }

    private DefendantInformationTabDetails createSummaryDefendantDetails(Party defendant, PCSCase pcsCase) {
        AddressUK addressForService = getSummaryDefendantAddressForService(defendant, pcsCase);

        if (defendant.getNameKnown() != VerticalYesNo.YES && addressForService == null) {
            return null;
        }

        return DefendantInformationTabDetails.builder()
            .firstName(getDefendantFirstName(defendant))
            .lastName(getDefendantLastName(defendant))
            .addressForService(addressForService)
            .build();
    }
}

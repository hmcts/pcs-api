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

    public DefendantInformationTabDetails buildSummaryDefendantOneDetails(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants())) {
            return null;
        }

        return createSummaryDefendantDetails(pcsCase.getAllDefendants().getFirst().getValue(), pcsCase);
    }

    public DefendantInformationTabDetails buildDetailedDefendantDetails(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants())) {
            return null;
        }

        return createDetailedDefendantDetails(pcsCase.getAllDefendants().getFirst().getValue(), pcsCase);
    }

    private DefendantInformationTabDetails createSummaryDefendantDetails(Party defendant, PCSCase pcsCase) {
        AddressUK addressForService = getDefendantAddressForService(defendant, pcsCase);
        VerticalYesNo nameKnown = defendant.getNameKnown();
        VerticalYesNo addressKnown = defendant.getAddressKnown();

        if (nameKnown != VerticalYesNo.YES && addressForService == null) {
            return null;
        }

        return DefendantInformationTabDetails.builder()
            .nameKnown(nameKnown != null ? nameKnown.getLabel() : null)
            .firstName(getDefendantFirstName(defendant))
            .lastName(getDefendantLastName(defendant))
            .addressForServiceKnown(addressKnown != null ? addressKnown.getLabel() : null)
            .addressForService(addressForService)
            .build();
    }

    private DefendantInformationTabDetails createDetailedDefendantDetails(Party defendant, PCSCase pcsCase) {
        VerticalYesNo nameKnown = defendant.getNameKnown();
        VerticalYesNo addressKnown = defendant.getAddressKnown();

        DefendantInformationTabDetails defendantInformationTabDetails = DefendantInformationTabDetails.builder()
            .nameKnown(nameKnown != null ? nameKnown.getLabel() : null)
            .addressKnown(addressKnown != null ? addressKnown.getLabel() : null)
            .build();

        if (nameKnown == VerticalYesNo.YES) {
            defendantInformationTabDetails.setFirstName(getDefendantFirstName(defendant));
            defendantInformationTabDetails.setLastName(getDefendantLastName(defendant));
        }

        if (addressKnown == VerticalYesNo.YES) {
            AddressUK addressForService = getDefendantAddressForService(defendant, pcsCase);
            defendantInformationTabDetails.setAddressForService(addressForService);
        }

        return defendantInformationTabDetails;
    }
}

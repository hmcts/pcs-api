package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;

import java.util.List;

@Component
public class AdditionalDefendantInformationTabDetailsBuilder extends DefendantInformationBuilder {
    public List<ListValue<AdditionalDefendantInformationTabDetails>> buildAdditionalDefendantsDetails(
        PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants()) || pcsCase.getAllDefendants().size() < 2) {
            return null;
        }

        return pcsCase.getAllDefendants().stream()
            .skip(1)
            .map(ListValue::getValue)
            .map(defendant -> createAdditionalSummaryDefendantDetails(defendant, pcsCase))
            .filter(defendantDetails -> defendantDetails != null)
            .map(defendantDetails -> ListValue.<uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails>builder()
                .value(defendantDetails)
                .build())
            .toList();
    }

    private AdditionalDefendantInformationTabDetails createAdditionalSummaryDefendantDetails(Party defendant,
                                                                                             PCSCase pcsCase) {
        AddressUK addressForService = getSummaryDefendantAddressForService(defendant, pcsCase);

        if (defendant.getNameKnown() != VerticalYesNo.YES && addressForService == null) {
            return null;
        }

        return uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails.builder()
            .firstName(getDefendantFirstName(defendant))
            .lastName(getDefendantLastName(defendant))
            .addressForService(addressForService)
            .build();
    }
}

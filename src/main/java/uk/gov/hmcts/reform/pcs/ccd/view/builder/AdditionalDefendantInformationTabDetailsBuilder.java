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
import java.util.Objects;

@Component
public class AdditionalDefendantInformationTabDetailsBuilder extends DefendantInformationBuilder {
    public List<ListValue<AdditionalDefendantInformationTabDetails>> buildSummaryAdditionalDefendantsDetails(
        PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants()) || pcsCase.getAllDefendants().size() < 2) {
            return null;
        }

        return pcsCase.getAllDefendants().stream()
            .skip(1)
            .map(ListValue::getValue)
            .map(defendant -> createAdditionalSummaryDefendantDetails(defendant, pcsCase))
            .filter(Objects::nonNull)
            .map(defendantDetails ->
                     ListValue.<AdditionalDefendantInformationTabDetails>builder()
                        .value(defendantDetails)
                        .build())
            .toList();
    }

    public List<ListValue<AdditionalDefendantInformationTabDetails>> buildDetailedAdditionalDefendantsDetails(
        PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllDefendants()) || pcsCase.getAllDefendants().size() < 2) {
            return null;
        }

        return pcsCase.getAllDefendants().stream()
            .skip(1)
            .map(ListValue::getValue)
            .map(defendant -> createAdditionalDetailedDefendantDetails(defendant, pcsCase))
            .filter(Objects::nonNull)
            .map(defendantDetails ->
                     ListValue.<AdditionalDefendantInformationTabDetails>builder()
                        .value(defendantDetails)
                        .build())
            .toList();
    }

    private AdditionalDefendantInformationTabDetails createAdditionalSummaryDefendantDetails(Party defendant,
                                                                                             PCSCase pcsCase) {
        AddressUK addressForService = getDefendantAddressForService(defendant, pcsCase);

        if (defendant.getNameKnown() != VerticalYesNo.YES && addressForService == null) {
            return null;
        }

        return AdditionalDefendantInformationTabDetails.builder()
            .firstName(getDefendantFirstName(defendant))
            .lastName(getDefendantLastName(defendant))
            .addressForService(addressForService)
            .build();
    }

    private AdditionalDefendantInformationTabDetails createAdditionalDetailedDefendantDetails(Party defendant,
                                                                                             PCSCase pcsCase) {
        VerticalYesNo nameKnown = defendant.getNameKnown();
        VerticalYesNo addressKnown = defendant.getAddressKnown();

        AdditionalDefendantInformationTabDetails additionalDefendantInformationTabDetails =
            AdditionalDefendantInformationTabDetails.builder()
                .nameKnown(nameKnown != null ? nameKnown.getLabel() : null)
                .addressKnown(addressKnown != null ? addressKnown.getLabel() : null)
                .build();

        if (nameKnown == VerticalYesNo.YES) {
            additionalDefendantInformationTabDetails.setFirstName(getDefendantFirstName(defendant));
            additionalDefendantInformationTabDetails.setLastName(getDefendantLastName(defendant));
        }

        if (addressKnown == VerticalYesNo.YES) {
            AddressUK addressForService = getDefendantAddressForService(defendant, pcsCase);
            additionalDefendantInformationTabDetails.setAddressForService(addressForService);
        }

        return additionalDefendantInformationTabDetails;
    }
}

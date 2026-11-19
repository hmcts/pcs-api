package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PartyExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ContactDetailsType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.DefendantType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageDefendantType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NameType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps rental or mortgage defendants (a mortgage defendant is a {@link DefendantType} that also carries the
 * occupier flag) to defendant 1 and the additional defendants of a {@link PCSCase}.
 */
@Component
public class BulkDefendantMapper {

    private final BulkAddressMapper addressMapper;

    public BulkDefendantMapper(BulkAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    /**
     * The mapped defendants.
     *
     * @param extras one entry per defendant in the same order as defendant 1 followed by the additional ones
     */
    public record DefendantsMapping(DefendantDetails defendant1,
                                    VerticalYesNo addAnotherDefendant,
                                    List<ListValue<DefendantDetails>> additionalDefendants,
                                    List<PartyExtras> extras) {

        public void applyTo(PCSCase pcsCase) {
            pcsCase.setDefendant1(defendant1);
            pcsCase.setAddAnotherDefendant(addAnotherDefendant);
            pcsCase.setAdditionalDefendants(additionalDefendants);
        }
    }

    /**
     * Maps the defendants in document order.
     *
     * @param elementName the repeating element name used in error paths, for example {@code Defendant}
     */
    public DefendantsMapping map(List<? extends DefendantType> defendants,
                                 String elementName,
                                 BulkErrorCollector errors) {

        List<DefendantDetails> details = new ArrayList<>();
        List<PartyExtras> extras = new ArrayList<>();

        for (int i = 0; i < defendants.size(); i++) {
            DefendantType defendant = defendants.get(i);
            String path = BulkErrorCollector.indexed(elementName, i);
            details.add(mapDefendant(defendant, path, errors));
            extras.add(mapExtras(defendant));
        }

        if (details.isEmpty()) {
            errors.add(elementName + "[1]", BulkErrorCollector.REQUIRED, "At least one defendant is required");
            return new DefendantsMapping(null, VerticalYesNo.NO, List.of(), extras);
        }

        List<DefendantDetails> additional = details.subList(1, details.size());
        return new DefendantsMapping(
            details.getFirst(),
            VerticalYesNo.from(!additional.isEmpty()),
            additional.isEmpty() ? null : ListValueUtils.wrapListItems(List.copyOf(additional)),
            extras
        );
    }

    private DefendantDetails mapDefendant(DefendantType defendant, String path, BulkErrorCollector errors) {
        NameType name = defendant.getName();
        errors.requiredWithMaxLength(path + "/Name/FirstName", name.getFirstName(), BulkLimits.NAME,
                                     "Defendant first name");
        errors.requiredWithMaxLength(path + "/Name/LastName", name.getLastName(), BulkLimits.NAME,
                                     "Defendant last name");

        DefendantDetails.DefendantDetailsBuilder builder = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName(name.getFirstName())
            .lastName(name.getLastName());

        ContactDetailsType contact = defendant.getContactDetails();
        boolean sameAsProperty = Boolean.TRUE.equals(defendant.isAddressSameAsProperty());
        AddressUK correspondence = contact != null
            ? addressMapper.toValidAddress(contact.getAddress(), path + "/ContactDetails/Address", errors) : null;

        if (sameAsProperty) {
            builder.addressKnown(VerticalYesNo.YES).addressSameAsPossession(VerticalYesNo.YES);
        } else if (correspondence != null) {
            builder.addressKnown(VerticalYesNo.YES)
                .addressSameAsPossession(VerticalYesNo.NO)
                .correspondenceAddress(correspondence);
        } else {
            builder.addressKnown(VerticalYesNo.NO);
        }
        return builder.build();
    }

    private static PartyExtras mapExtras(DefendantType defendant) {
        NameType name = defendant.getName();
        ContactDetailsType contact = defendant.getContactDetails();
        return new PartyExtras(
            name.getTitle(),
            name.getInitials(),
            name.getOtherNames(),
            contact != null ? contact.getDX() : null,
            contact != null ? contact.getFaxNumber() : null,
            defendant instanceof MortgageDefendantType mortgageDefendant ? mortgageDefendant.isIsOccupant() : null
        );
    }
}

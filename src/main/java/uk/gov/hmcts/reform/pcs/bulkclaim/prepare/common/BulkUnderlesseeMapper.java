package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.UnderlesseeOrMortgageeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class BulkUnderlesseeMapper {

    private final BulkAddressMapper addressMapper;

    public BulkUnderlesseeMapper(BulkAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public record UnderlesseeMapping(VerticalYesNo hasUnderlesseeOrMortgagee,
                                     UnderlesseeMortgageeDetails underlesseeOrMortgagee1,
                                     VerticalYesNo addAdditionalUnderlesseeOrMortgagee,
                                     List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeOrMortgagee) {

        public void applyTo(PCSCase pcsCase) {
            pcsCase.setHasUnderlesseeOrMortgagee(hasUnderlesseeOrMortgagee);
            pcsCase.setUnderlesseeOrMortgagee1(underlesseeOrMortgagee1);
            pcsCase.setAddAdditionalUnderlesseeOrMortgagee(addAdditionalUnderlesseeOrMortgagee);
            pcsCase.setAdditionalUnderlesseeOrMortgagee(additionalUnderlesseeOrMortgagee);
        }
    }

    public UnderlesseeMapping map(List<UnderlesseeOrMortgageeType> underlessees,
                                  String elementName,
                                  BulkErrorCollector errors) {
        if (underlessees.isEmpty()) {
            return new UnderlesseeMapping(VerticalYesNo.NO, null, null, null);
        }

        List<UnderlesseeMortgageeDetails> details = new ArrayList<>();
        for (int i = 0; i < underlessees.size(); i++) {
            details.add(mapOne(underlessees.get(i), BulkErrorCollector.indexed(elementName, i), errors));
        }

        List<UnderlesseeMortgageeDetails> additional = details.subList(1, details.size());
        return new UnderlesseeMapping(
            VerticalYesNo.YES,
            details.getFirst(),
            VerticalYesNo.from(!additional.isEmpty()),
            additional.isEmpty() ? null : ListValueUtils.wrapListItems(List.copyOf(additional))
        );
    }

    private UnderlesseeMortgageeDetails mapOne(UnderlesseeOrMortgageeType underlessee,
                                               String path,
                                               BulkErrorCollector errors) {
        UnderlesseeMortgageeDetails.UnderlesseeMortgageeDetailsBuilder builder = UnderlesseeMortgageeDetails.builder();

        if (StringUtils.isNotBlank(underlessee.getName())) {
            errors.maxLength(path + "/Name", underlessee.getName(), BulkLimits.NAME, "Underlessee or mortgagee name");
            builder.nameKnown(VerticalYesNo.YES).name(underlessee.getName());
        } else {
            builder.nameKnown(VerticalYesNo.NO);
        }

        if (underlessee.getAddress() != null) {
            builder.addressKnown(VerticalYesNo.YES)
                .address(addressMapper.toValidAddress(underlessee.getAddress(), path + "/Address", errors));
        } else {
            builder.addressKnown(VerticalYesNo.NO);
        }
        return builder.build();
    }
}

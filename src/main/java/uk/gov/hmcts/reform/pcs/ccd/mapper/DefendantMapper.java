package uk.gov.hmcts.reform.pcs.ccd.mapper;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
public class DefendantMapper {

    private final ModelMapper modelMapper;

    public List<Defendant> mapFromDefendantDetails(List<ListValue<DefendantDetails>> defendants) {
        if (defendants == null) {
            return Collections.emptyList();
        }
        List<Defendant> result = new ArrayList<>();
        for (ListValue<DefendantDetails> item : defendants) {
            DefendantDetails details = item.getValue();
            if (details != null) {
                Defendant defendant = modelMapper.map(details, Defendant.class);
                defendant.setId(item.getId());
                if (details.getAddressSameAsPossession() == null) {
                    defendant.setAddressSameAsPossession(false);
                }
                result.add(defendant);
            }
        }
        return result;
    }

}

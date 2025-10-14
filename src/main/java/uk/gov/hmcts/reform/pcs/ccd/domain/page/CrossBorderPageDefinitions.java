package uk.gov.hmcts.reform.pcs.ccd.domain.page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossBorderPageDefinitions {

    @CCD(searchable = false)
    private YesOrNo showCrossBorderPage;

    @CCD(typeOverride = DynamicRadioList)
    @External
    private DynamicStringList crossBorderCountriesList;

    @CCD(searchable = false)
    @External
    private String crossBorderCountry1;

    @CCD(searchable = false)
    @External
    private String crossBorderCountry2;
}

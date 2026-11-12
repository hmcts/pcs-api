package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimTenancy {

    private CitizenClaimTenancyType tenancyType;

    private String otherTenancyTypeDetails;

    private LocalDate tenancyStartDate;

    private VerticalYesNo tenancyAgreementAvailable;

    @CCD(typeOverride = TextArea)
    private String noTenancyAgreementReason;

    private List<ListValue<Document>> tenancyAgreementDocuments;

}

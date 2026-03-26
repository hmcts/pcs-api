package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.List;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PossessionClaimResponse {

    @CCD(
        access = {CitizenAccess.class},
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Text"
    )
    private List<ListValue<String>> claimantOrganisations;

    @CCD(access = {CitizenAccess.class})
    private Party claimantEnteredDefendantDetails;

    @CCD(access = {CitizenAccess.class})
    private DefendantContactDetails defendantContactDetails;

    @CCD(access = {CitizenAccess.class})
    private DefendantResponses defendantResponses;

    @CCD(ignore = true)
    private List<String> clearFields;

}


package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.MoneyGBP;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PCSCase {

    @CCD(
        label = "Applicant's forename",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String applicantForename;

    @CCD(
        label = "Applicant's surname",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String applicantSurname;

    @CCD(
        label = "Property address",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private AddressUK propertyAddress;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "PossessionGround",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<PossessionGround> groundsForPossession;

    @CCD(
        label = "Tenancy Agreement",
        hint = "What type of tenancy agreement is in place?",
        typeOverride = FixedRadioList,
        typeParameterOverride = "TenancyAgreementType",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private TenancyAgreementType tenancyAgreementType;

    @CCD(
        label = "Tenancy start date",
        hint = "What date did the tenancy begin?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private LocalDate tenancyStartDate;

    @CCD(
        label = "Have you attempted mediation with the defendants?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo mediationAttempted;

    @CCD(
        label = "Have you tried to reach a settlement with the defendants?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo settlementAttempted;

    @CCD(
        label = "How much is the rent?",
        typeOverride = MoneyGBP,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String rentAmountPence;

    @CCD(
        label = "How frequently should rent be paid?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private RentFrequency rentFrequency;

    @CCD(
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @JsonUnwrapped
    @Builder.Default
    private MarkdownFields markdownFields = new MarkdownFields();

    @CCD(
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @Builder.Default
    private ControlFlags controlFlags = new ControlFlags();

    @CCD(
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo continueWithDraft;

}

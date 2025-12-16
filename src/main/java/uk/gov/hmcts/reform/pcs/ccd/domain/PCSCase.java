package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.WaysToPay;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;


/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    // Field label constants - shared between domain annotations and validation
    public static final String NOTICE_EMAIL_EXPLANATION_LABEL = "Explain how it was served by email";
    public static final String NOTICE_OTHER_EXPLANATION_LABEL = "Explain what the other means were";
    public static final String OTHER_GROUND_DESCRIPTION_LABEL = "Enter your grounds for possession";

    @CCD(
        access = {DefendantAccess.class}
    )
    private YesOrNo submitDraftAnswers;

    @CCD(
        searchable = false
    )
    @External
    private String feeAmount;

    private YesOrNo hasUnsubmittedCaseData;

    @CCD(label = "Do you want to resume your claim using your saved answers?")
    private YesOrNo resumeClaimKeepAnswers;

    @JsonUnwrapped
    private ClaimantInformation claimantInformation;

    @CCD(
        label = "Property address",
        access = {CitizenAccess.class}
    )
    @External
    private AddressUK propertyAddress;

    private String formattedPropertyAddress;

    @CCD(searchable = false)
    private YesOrNo showCrossBorderPage;

    @CCD(searchable = false)
    private YesOrNo showPropertyNotEligiblePage;

    @CCD(
        typeOverride = DynamicRadioList
    )
    @External
    private DynamicStringList crossBorderCountriesList;

    @CCD(
        searchable = false
    )
    @External
    private String crossBorderCountry1;

    @CCD(
        searchable = false
    )
    @External
    private String crossBorderCountry2;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class}
    )
    @External
    private String userPcqId;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class}
    )
    private YesOrNo userPcqIdSet;

    @CCD(
        label = "Case management location"
    )
    private Integer caseManagementLocation;

    @CCD(label = "Party")
    private List<ListValue<Party>> parties;

    @JsonUnwrapped
    private ClaimantContactPreferences claimantContactPreferences;

    @CCD(
        label = "Do you want to ask for your costs back?",
        hint = "You do not need to provide the exact amount at this stage, but a judge will request a schedule "
            + "of costs at the hearing"
    )
    private VerticalYesNo claimingCostsWanted;

    @CCD(
        label = "Have you followed the pre-action protocol?"
    )
    private VerticalYesNo preActionProtocolCompleted;

    @CCD(
        label = "Are you claiming possession because of rent arrears?",
        hint = "You’ll be able to add additional grounds later if you select yes"
    )
    private YesOrNo claimDueToRentArrears;

    // Rent arrears grounds checkboxes
    @CCD(
        label = "What are your grounds for possession?",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsGround"
    )
    private Set<RentArrearsGround> rentArrearsGrounds;

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsGround"
    )
    private Set<RentArrearsGround> copyOfRentArrearsGrounds;

    @CCD
    private YesOrNo overrideResumedGrounds;

    @CCD(
        label = "Do you have any other additional grounds for possession?"
    )
    private YesOrNo hasOtherAdditionalGrounds;

    // Additional grounds checkboxes - Mandatory
    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsMandatoryGrounds"
    )
    private Set<RentArrearsMandatoryGrounds> rentArrearsMandatoryGrounds;

    // Additional grounds checkboxes - Discretionary
    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsDiscretionaryGrounds"
    )
    private Set<RentArrearsDiscretionaryGrounds> rentArrearsDiscretionaryGrounds;

    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredAdditionalMandatoryGrounds"
    )
    private Set<AssuredAdditionalMandatoryGrounds> assuredAdditionalMandatoryGrounds;

    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AssuredAdditionalDiscretionaryGrounds"
    )
    private Set<AssuredAdditionalDiscretionaryGrounds> assuredAdditionalDiscretionaryGrounds;

    @JsonUnwrapped
    private RentArrearsGroundsReasons rentArrearsGroundsReasons;

    private YesOrNo showRentArrearsGroundReasonPage;

    @CCD(
        label = "Have you attempted mediation with the defendants?"
    )
    private VerticalYesNo mediationAttempted;

    @CCD(
        label = "Give details about the attempted mediation and what the outcome was",
        hint = "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String mediationAttemptedDetails;

    @CCD(
        label = "Have you tried to reach a settlement with the defendants?"
    )
    private VerticalYesNo settlementAttempted;

    @CCD(
        label = "Explain what steps you’ve taken to reach a settlement",
        hint = "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String settlementAttemptedDetails;

    @CCD(
        label = "Have you served notice to the defendants?"
    )
    private YesOrNo noticeServed;

    @JsonUnwrapped(prefix = "eng")
    @CCD
    private NoticeServedDetails noticeServedDetails;

    private String caseTitleMarkdown;

    private LegislativeCountry legislativeCountry;

    @CCD(
        label = "Who is the claimant in this case?",
        hint = "If you’re a legal representative, you should select the type of claimant you’re representing",
        typeOverride = DynamicRadioList
    )
    private DynamicStringList claimantType;

    @CCD(searchable = false)
    private YesOrNo showClaimantTypeNotEligibleEngland;

    @CCD(searchable = false)
    private YesOrNo showClaimantTypeNotEligibleWales;

    @CCD(
        label = "Is this a claim against trespassers?"
    )
    private VerticalYesNo claimAgainstTrespassers;

    @CCD(searchable = false)
    private YesOrNo showClaimTypeNotEligibleEngland;

    @CCD(searchable = false)
    private YesOrNo showClaimTypeNotEligibleWales;

    @JsonUnwrapped(prefix = "wales")
    @CCD
    private WalesHousingAct walesHousingAct;

    @CCD(label = "Are you also making a claim for an order imposing a prohibited conduct standard contract?")
    private VerticalYesNo prohibitedConductWalesClaim;

    @CCD(
        label = "Why are you making this claim?",
        hint = "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String prohibitedConductWalesWhyMakingClaim;

    @CCD
    private PeriodicContractTermsWales periodicContractTermsWales;

    @JsonUnwrapped(prefix = "rentDetails_")
    @CCD
    private RentSection rentSection;

    private RentPaymentFrequency rentSectionPaymentFrequency;

    @CCD(searchable = false)
    private YesOrNo showPostcodeNotAssignedToCourt;

    @CCD(searchable = false)
    private String postcodeNotAssignedView;

    /**
     * The primary defendant in the case.
     */
    @CCD
    private DefendantDetails defendant1;

    @CCD(label = "Do you need to add another defendant?")
    private VerticalYesNo addAnotherDefendant;

    /**
     * List of additional defendants added by the user, after the primary defendant.
     */
    @CCD(
        label = "Add additional defendant",
        hint = "Add an additional defendant to the case"
    )
    private List<ListValue<DefendantDetails>> additionalDefendants;

    /**
     * Combined list of all defendants in the case (i.e. primary defendant + additional defendants).
     */
    private List<ListValue<DefendantDetails>> allDefendants;

    @JsonUnwrapped(prefix = "tenancy_")
    @CCD
    private TenancyLicenceDetails tenancyLicenceDetails;

    @CCD(searchable = false)
    private String nextStepsMarkdown;

    // --- Rent arrears (statement upload + totals + third party payments) ---
    @CCD(
        label = "Add document",
        hint = "Upload a document to the system",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Document"
    )
    private List<ListValue<Document>> rentStatementDocuments;

    @CCD(
        label = "Total rent arrears",
        min = 0,
        typeOverride = FieldType.MoneyGBP
    )
    private String totalRentArrears;

    @CCD(
        label = "For the period shown on the rent statement, have any rent payments been paid by someone "
            + "other than the defendants?",
        hint = "This could include payments from Universal Credit, Housing Benefit or any other contributions "
            + "made by a government department, like the Department for Work and Pensions (DWP)"
    )
    private VerticalYesNo thirdPartyPayments;

    @CCD(
        label = "Where have the payments come from?",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "ThirdPartyPaymentSource"
    )
    private List<ThirdPartyPaymentSource> thirdPartyPaymentSources;

    @CCD(
        label = "Payment source"
    )
    private String thirdPartyPaymentSourceOther;

    @CCD
    private YesOrNo showIntroductoryDemotedOtherGroundReasonPage;

    @JsonUnwrapped(prefix = "introGrounds_")
    @CCD
    private IntroductoryDemotedOtherGroundsForPossession introductoryDemotedOrOtherGroundsForPossession;

    @JsonUnwrapped
    @CCD
    private IntroductoryDemotedOtherGroundReason introductoryDemotedOtherGroundReason;

    @JsonUnwrapped
    private SecureOrFlexiblePossessionGrounds secureOrFlexiblePossessionGrounds;

    @CCD(
        label = "What does your ground 1 claim involve?",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "RentArrearsOrBreachOfTenancy"
    )
    private Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreachOfTenancy;

    @CCD(searchable = false)
    private YesOrNo showBreachOfTenancyTextarea;

    @CCD(searchable = false)
    private YesOrNo showReasonsForGroundsPage;

    @JsonUnwrapped(prefix = "wales")
    @CCD
    private GroundsReasonsWales groundsReasonsWales;


    @JsonUnwrapped
    @CCD
    private SecureOrFlexibleGroundsReasons secureOrFlexibleGroundsReasons;

    @CCD(
        label = "Do you want the court to make a judgment for the outstanding arrears?",
        searchable = false
    )
    private VerticalYesNo arrearsJudgmentWanted;

    @JsonUnwrapped(prefix = "noRentArrears_")
    private NoRentArrearsGroundsOptions noRentArrearsGroundsOptions;

    @JsonUnwrapped
    private NoRentArrearsReasonForGrounds noRentArrearsReasonForGrounds;

    private YesOrNo showRentSectionPage;

    @CCD(searchable = false)
    private YesOrNo showRentArrearsPage;

    @CCD(
        label = "Which language did you use to complete this service?",
        hint = "If someone else helped you to answer a question in this service, "
            + "ask them if they answered any questions in Welsh. We’ll use this to "
            + "make sure your claim is processed correctly"
    )
    private LanguageUsed languageUsed;

    @JsonUnwrapped
    private DefendantCircumstances defendantCircumstances;

    @CCD(
        label = "In the alternative to possession, would you like to claim suspension of right to buy"
            + " or demotion of tenancy?",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "AlternativesToPossession"
    )
    private Set<AlternativesToPossession> alternativesToPossession;

    @JsonUnwrapped
    private SuspensionOfRightToBuy suspensionOfRightToBuy;

    @JsonUnwrapped
    private DemotionOfTenancy demotionOfTenancy;

    private AdditionalReasons additionalReasonsForPossession;

    @JsonUnwrapped
    @CCD
    private ClaimantCircumstances claimantCircumstances;

    @CCD(
        label = "Do you want to upload any additional documents?",
        hint = "You can either upload documents now or closer to the hearing date. "
            + "Any documents you upload now will be included in the pack of documents a judge will "
            + "receive before the hearing (the bundle)"
    )
    private VerticalYesNo wantToUploadDocuments;

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system"
    )
    private List<ListValue<AdditionalDocument>> additionalDocuments;

    @CCD(
        label = "Are you planning to make an application at the same time as your claim?",
        hint = "After you’ve submitted your claim, there will be instructions on how to make an application"
    )
    private VerticalYesNo applicationWithClaim;

    @CCD(
        label = "What would you like to do next?",
        typeOverride = FieldType.FixedRadioList,
        typeParameterOverride = "CompletionNextStep"
    )
    private CompletionNextStep completionNextStep;

    @JsonUnwrapped(prefix = "groundsForPossessionWales_")
    private GroundsForPossessionWales groundsForPossessionWales;

    @JsonUnwrapped
    private SuspensionOfRightToBuyDemotionOfTenancy  suspensionOfRightToBuyDemotionOfTenancy;

    @JsonUnwrapped(prefix = "wales")
    private WalesNoticeDetails walesNoticeDetails;

    @JsonUnwrapped(prefix = "secureContract_")
    private SecureContractGroundsForPossessionWales secureContractGroundsForPossessionWales;

    @CCD(
        label = "Estate management grounds",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "EstateManagementGroundsWales"
    )
    private Set<EstateManagementGroundsWales> estateManagementGroundsWales;

    @CCD(searchable = false)
    private YesOrNo showReasonsForGroundsPageWales;

    @JsonUnwrapped
    @CCD
    private OccupationLicenceDetailsWales occupationLicenceDetailsWales;

    @JsonUnwrapped
    private EnforcementOrder enforcementOrder;

    @CCD(label = "Is there an underlessee or mortgagee entitled to claim relief against forfeiture?")
    private VerticalYesNo hasUnderlesseeOrMortgagee;

    @CCD
    private UnderlesseeMortgageeDetails underlesseeOrMortgagee1;

    @CCD(label = "Do you need to add another underlessee or mortgagee?")
    private VerticalYesNo addAdditionalUnderlesseeOrMortgagee;

    @CCD(
        label = "Add underlessee or mortgagee",
        hint = "Add an underlessee or mortgagee to the case"
    )
    private List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeOrMortgagee;

    @CCD(
        searchable = false,
        label = "Ways to pay"
    )
    private WaysToPay waysToPay;

    @CCD
    private StatementOfTruthDetails statementOfTruth;

    @CCD(searchable = false)
    private YesOrNo showPreActionProtocolPageWales;

    @CCD(searchable = false)
    private YesOrNo showASBQuestionsPageWales;

    @JsonUnwrapped(prefix = "wales")
    @CCD
    private ASBQuestionsDetailsWales asbQuestionsWales;

    @CCD(
        access = {DefendantAccess.class}
    )
    private DefendantResponse defendantResponse;

    @CCD(searchable = false)
    private String formattedDefendantNames;

}

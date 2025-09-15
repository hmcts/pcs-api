package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private final YesOrNo decentralised = YesOrNo.YES;

    private YesOrNo hasUnsubmittedCaseData;

    @CCD(label = "Do you want to resume your claim using your saved answers?")
    private YesOrNo resumeClaimKeepAnswers;

    @CCD(
        label = "Claimant Name",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String claimantName;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo isClaimantNameCorrect;

    @CCD(
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String overriddenClaimantName;

    @CCD(
        label = "Property address",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private AddressUK propertyAddress;

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private YesOrNo showCrossBorderPage;

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private YesOrNo showPropertyNotEligiblePage;
    @CCD(
        typeOverride = DynamicRadioList,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private DynamicStringList crossBorderCountriesList;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String crossBorderCountry1;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String crossBorderCountry2;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class}
    )
    @External
    private String userPcqId;

    @CCD(searchable = false, access = {CitizenAccess.class})
    private YesOrNo userPcqIdSet;

    @CCD(
        label = "Case management location",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private Integer caseManagementLocation;

    @CCD(
        label = "Payment status",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private PaymentStatus paymentStatus;

    @CCD(
        label = "Amount to pay",
        hint = "£400",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private PaymentType paymentType;

    @CCD(label = "Party")
    private List<ListValue<Party>> parties;

    @CCD(typeOverride = FieldType.Email)
    private String claimantContactEmail;

    @CCD(label = "Do you want to use this email address for notifications?")
    private VerticalYesNo isCorrectClaimantContactEmail;

    @CCD(label = "Enter email address", typeOverride = FieldType.Email)
    private String overriddenClaimantContactEmail;

    private String formattedClaimantContactAddress;

    @CCD(label = "Do you want documents to be sent to this address?")
    private VerticalYesNo isCorrectClaimantContactAddress;

    @CCD(label = "Enter address details")
    private AddressUK overriddenClaimantContactAddress;

    @CCD(label = "Do you want to provide a contact phone number? (Optional)")
    private VerticalYesNo claimantProvidePhoneNumber;

    @CCD(label = "Enter phone number", typeOverride = FieldType.PhoneUK)
    private String claimantContactPhoneNumber;

    @CCD(
        label = "Have you followed the pre-action protocol?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo preActionProtocolCompleted;

    @CCD(
        label = "Are you claiming possession because of rent arrears?",
        hint = "You'll be able to add additional grounds later if you select yes.",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo groundsForPossession;

    // Rent arrears grounds checkboxes
    @CCD(
        label = "What are your grounds for possession?",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "RentArrearsGround",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<RentArrearsGround> rentArrearsGrounds;

    @CCD(
        label = "Do you have any other additional grounds for possession?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo hasOtherAdditionalGrounds;

    // Additional grounds checkboxes - Mandatory
    @CCD(
        label = "Mandatory grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "MandatoryGround",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<MandatoryGround> mandatoryGrounds;

    // Additional grounds checkboxes - Discretionary
    @CCD(
        label = "Discretionary grounds",
        hint = "Select all that apply",
        typeOverride = MultiSelectList,
        typeParameterOverride = "DiscretionaryGround",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<DiscretionaryGround> discretionaryGrounds;

    @CCD(
        label = "Have you attempted mediation with the defendants?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo mediationAttempted;

    @CCD(
        label = "Give details about the attempted mediation and what the outcome was",
        hint = "You can enter up to 250 characters",
        access = {CitizenAccess.class, CaseworkerAccess.class},
        max = 250,
        typeOverride = TextArea
    )
    private String mediationAttemptedDetails;

    @CCD(
        label = "Have you tried to reach a settlement with the defendants?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo settlementAttempted;

    @CCD(
        label = "Explain what steps you've taken to reach a settlement",
        hint = "You can enter up to 250 characters",
        access = {CitizenAccess.class, CaseworkerAccess.class},
        max = 250,
        typeOverride = TextArea
    )
    private String settlementAttemptedDetails;

    @CCD(
        label = "Have you served notice to the defendants?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo noticeServed;

    private String pageHeadingMarkdown;

    private String claimPaymentTabMarkdown;

    private LegislativeCountry legislativeCountry;

    @CCD(
        label = "Who is the claimant in this case?",
        hint = "If you’re a legal representative, you should select the type of claimant you’re representing.",
        typeOverride = DynamicRadioList,
        access = {CaseworkerAccess.class}
    )
    private DynamicStringList claimantType;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimantTypeNotEligibleEngland;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimantTypeNotEligibleWales;

    @CCD(
        label = "Is this a claim against trespassers?",
        access = CaseworkerAccess.class
    )
    private VerticalYesNo claimAgainstTrespassers;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimTypeNotEligibleEngland;

    @CCD(searchable = false, access = CaseworkerAccess.class)
    private YesOrNo showClaimTypeNotEligibleWales;

    @CCD(
        label = "How much is the rent?",
        typeOverride = FieldType.MoneyGBP,
        min = 0,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String currentRent;

    @CCD(
        label = "How frequently should rent be paid?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private RentPaymentFrequency rentFrequency;

    @CCD(
        label = "Enter frequency",
        hint = "Please specify the frequency",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String otherRentFrequency;

    @CCD(
        label = "Enter the amount per day that unpaid rent should be charged at",
        typeOverride = FieldType.MoneyGBP,
        min = 0,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String dailyRentChargeAmount;

    @CCD(
        label = "Is the amount per day that unpaid rent should be charged at correct?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo rentPerDayCorrect;

    @CCD(
        label = "Enter amount per day that unpaid rent should be charged at",
        typeOverride = FieldType.MoneyGBP,
        min = 0,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String amendedDailyRentChargeAmount;

    @CCD(
        typeOverride = FieldType.MoneyGBP,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String calculatedDailyRentChargeAmount;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private String formattedCalculatedDailyRentChargeAmount;

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private YesOrNo showPostcodeNotAssignedToCourt;

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private String postcodeNotAssignedView;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private DefendantDetails defendant1;

    @CCD(access = {CitizenAccess.class, CaseworkerAccess.class})
    private List<ListValue<DefendantDetails>> defendants;
    
    // Notice Details fields
    @CCD(
        label = "How did you serve the notice?",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private NoticeServiceMethod noticeServiceMethod;

    // Date fields for different service methods
    @CCD(
        label = "Date the document was posted",
        hint = "For example, 16 4 2021",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private LocalDate noticePostedDate;

    @CCD(
        label = "Date the document was delivered",
        hint = "For example, 16 4 2021",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private LocalDate noticeDeliveredDate;

    @CCD(
        label = "Date and time the document was handed over",
        hint = "For example, 16 4 2021, 11 15",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private LocalDateTime noticeHandedOverDateTime;

    @CCD(
        label = "Date and time the document was handed over",
        hint = "For example, 16 4 2021, 11 15",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private LocalDateTime noticeEmailSentDateTime;

    @CCD(
        label = "Date and time email or message sent",
        hint = "For example, 16 4 2021, 11 15",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private LocalDateTime noticeOtherElectronicDateTime;

    @CCD(
        label = "Date and time the document was handed over",
        hint = "For example, 16 4 2021, 11 15",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private LocalDateTime noticeOtherDateTime;

    // Text fields for different service methods
    @CCD(
        label = "Name of person the document was left with",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String noticePersonName;

    @CCD(
        label = "Explain how it was served by email",
        access = {CitizenAccess.class, CaseworkerAccess.class},
        max = 250,
        typeOverride = TextArea
    )
    private String noticeEmailExplanation;

    @CCD(
        label = "Explain what the other means were",
        access = {CitizenAccess.class, CaseworkerAccess.class},
        max = 250,
        typeOverride = TextArea
    )
    private String noticeOtherExplanation;

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Document",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<ListValue<Document>> noticeDocuments;

    @CCD(
        label = "What type of tenancy or licence is in place?",
        access = {CaseworkerAccess.class}
    )
    private TenancyLicenceType typeOfTenancyLicence;

    @CCD(
        label = "Give details of the type of tenancy or licence agreement that's in place",
        typeOverride = TextArea,
        access = {CaseworkerAccess.class}
    )
    private String detailsOfOtherTypeOfTenancyLicence;

    @CCD(
        label = "What date did the tenancy or licence begin?",
        hint = "For example, 16 4 2021",
        access = {CaseworkerAccess.class}
    )
    private LocalDate tenancyLicenceDate;

    @CCD(
        label = "Add document",hint = "Upload a document to the system",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<ListValue<Document>> tenancyLicenceDocuments;

    @CCD(searchable = false)
    private String nextStepsMarkdown;

    // --- Rent arrears (statement upload + totals + third party payments) ---
    @CCD(
        label = "Add document",
        hint = "Upload a document to the system",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Document",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<ListValue<Document>> rentStatementDocuments;

    @CCD(
        label = "Total rent arrears",
        min = 0,
        typeOverride = FieldType.MoneyGBP,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String totalRentArrears;

    @CCD(
        label = "For the period shown on the rent statement, have any rent payments been paid by someone "
            + "other than the defendants?",
        hint = "This could include payments from Universal Credit, Housing Benefit or any other contributions "
            + "made by a government department, like the Department for Work and Pensions (DWP).",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private VerticalYesNo thirdPartyPayments;

    @CCD(
        label = "Where have the payments come from?",
        hint = "Select all that apply",
        typeOverride = FieldType.MultiSelectList,
        typeParameterOverride = "ThirdPartyPaymentSource",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private java.util.List<ThirdPartyPaymentSource> thirdPartyPaymentSources;

    @CCD(
        label = "Payment source",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String thirdPartyPaymentSourceOther;

}

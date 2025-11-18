/**
 * Data structure to store all answers collected during the case creation journey
 * This data will be used to validate the Check Your Answers (CYA) page
 */
export interface CYAData {
  // Address Information
  propertyAddress?: {
    buildingStreet?: string;
    townCity?: string;
    postcode?: string;
    country?: string;
  };

  // Claimant Information
  claimantType?: string;
  claimantName?: string;
  contactPreferences?: {
    emailNotifications?: string;
    correspondenceAddress?: string;
    phoneNumber?: string;
  };

  // Defendant Information
  defendantDetails?: {
    name?: string;
    firstName?: string;
    lastName?: string;
    correspondenceAddress?: string;
    additionalDefendants?: Array<{
      name?: string;
      correspondenceAddress?: string;
    }>;
  };

  // Tenancy/Licence Details
  tenancyLicenceDetails?: {
    type?: string;
    startDate?: string;
    files?: string[];
  };

  // Grounds for Possession
  groundsForPossession?: {
    hasGrounds?: string;
    rentArrearsGrounds?: string[];
    otherGrounds?: string[];
    mandatoryGrounds?: string[];
    discretionaryGrounds?: string[];
    reasons?: Record<string, string>;
  };

  // Pre-Action Protocol
  preActionProtocol?: string;

  // Mediation and Settlement
  mediationAndSettlement?: {
    attemptedMediation?: string;
    attemptedMediationDetails?: string;
    settlement?: string;
    settlementDetails?: string;
  };

  // Notice Details
  noticeDetails?: {
    servedNotice?: string;
    howServed?: string;
    serviceDate?: string;
    serviceTime?: string;
    explanation?: string;
    files?: string[];
  };

  // Rent Details
  rentDetails?: {
    amount?: string;
    frequency?: string;
    dailyAmount?: string;
    unpaidRentAmountPerDay?: string;
  };

  // Rent Arrears Details
  rentArrearsDetails?: {
    amountOnStatement?: string;
    rentPaidByOthers?: string;
    paymentSources?: string[];
    files?: string[];
  };

  // Money Judgment
  moneyJudgment?: string;

  // Claimant Circumstances
  claimantCircumstances?: {
    hasCircumstances?: string;
    details?: string;
  };

  // Defendant Circumstances
  defendantCircumstances?: {
    hasCircumstances?: string;
    details?: string;
  };

  // Alternatives to Possession
  alternativesToPossession?: {
    options?: string[];
    housingAct?: Array<{
      question?: string;
      option?: string;
    }>;
    statementOfExpressTerms?: string;
    suspensionReason?: string;
    demotionReason?: string;
  };

  // Claiming Costs
  claimingCosts?: string;

  // Additional Reasons for Possession
  additionalReasonsForPossession?: string;

  // Underlessee or Mortgagee
  underlesseeOrMortgagee?: {
    entitledToClaim?: string;
    details?: Array<{
      name?: string;
      address?: string;
    }>;
  };

  // Documents
  documents?: {
    wantToUpload?: string;
    uploadedDocuments?: Array<{
      type?: string;
      fileName?: string;
      description?: string;
    }>;
  };

  // Applications
  applications?: string;

  // Language Used
  languageUsed?: string;

  // Completing Your Claim
  completingYourClaim?: string;

  // Statement of Truth
  statementOfTruth?: {
    completedBy?: string;
    fullName?: string;
    positionOrOffice?: string;
    nameOfFirm?: string;
  };
}

// Global instance to store CYA data during test execution
export let cyaData: CYAData = {};

// Function to reset CYA data (useful for test cleanup)
export function resetCYAData(): void {
  cyaData = {};
}

// Helper function to format date
export function formatDate(day: string, month: string, year: string): string {
  return `${day}/${month}/${year}`;
}

// Helper function to format time
export function formatTime(hour: string, minute: string, second?: string): string {
  if (second) {
    return `${hour}:${minute}:${second}`;
  }
  return `${hour}:${minute}`;
}


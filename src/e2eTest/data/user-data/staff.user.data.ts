/** One IdAM-backed staff user entry (global setup creates each in turn). */
export type StaffEntity = {
  email: string;
  uid?: string | undefined;
};

export const staff = {
  CTSCAdmin: {
    email: 'PCS-CTSCAdmin01@justice.gov.uk',
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  CTSCAdminTaskSupervisor: {
    email: 'PCS-CTSCAdminTaskSupervisor01@justice.gov.uk',
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  CTSCAdminCaseAllocator: {
    email: 'PCS-CTSCAdminCaseAllocator01@justice.gov.uk',
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  CTSCAdminCaseAllocAndTaskSup: {
    email: 'PCS-CTSCAdminCaseAllocAndTaskSup01@justice.gov.uk',
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  }
} as const satisfies Record<string, StaffEntity>;

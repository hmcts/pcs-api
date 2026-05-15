export type StaffEntity = {
  email: string;
  uid?: string | undefined;
};

/** IdAM user ids from `staff` for AM org-role mapping (deduped, order preserved). */
export function staffUidsForOrgMapping(): string[] {
  return [
    ...new Set(
      Object.values(staff)
        .map((s) => s.uid)
        .filter((id): id is string => typeof id === 'string' && id.length > 0)
    )
  ];
}

export const staff = {
  CTSCAdmin: {
    email: 'PCS-CTSCAdmin01@justice.gov.uk',
    uid: process.env.PCS_CTSC_ADMIN_01_UID
  }
} as const satisfies Record<string, StaffEntity>;

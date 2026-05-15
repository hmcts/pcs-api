import type {StaffEntity} from "@data/user-data/staff.user.data";
import {IdamUtils} from "@hmcts/playwright-common";

export async function createTempUsers(
  staffUsers: Record<string, StaffEntity>
): Promise<void> {
  for (const entity of Object.values(staffUsers)) {
    await createTempUser(entity);
  }
}

function isIdamUserAlreadyExistsError(error: unknown): boolean {
  return error instanceof Error && /Status Code:\s*409\b/.test(error.message);
}

async function createTempUser(body: StaffEntity): Promise<void> {
  if (!body.uid) {
    throw new Error(
      `staff.user.data: missing uid for ${body.email} (set uid on the staff entry, e.g. PCS_SOLICITOR_AUTOMATION_UID).`
    );
  }

  const token = process.env.BEARER_TOKEN as string;
  const password = process.env.IDAM_PCS_USER_PASSWORD as string;
  const email = body.email;
  const [forename, surnameWithDomain] = email.split('-');
  const surname = surnameWithDomain.split('@')[0];
  const roleNames: string[] = ['caseworker'];

  try {
    await new IdamUtils().createUser({
      bearerToken: token,
      password,
      user: {
        id: body.uid,
        email,
        forename,
        surname,
        roleNames
      }
    });
  } catch (error: unknown) {
    if (isIdamUserAlreadyExistsError(error)) {
      console.log(
        `IdAM user already exists (409), continuing: ${email} (uid: ${body.uid})`
      );
      return;
    }
    throw error;
  }
}

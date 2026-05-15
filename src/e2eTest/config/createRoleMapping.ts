import {amOrgRoleMappingServiceApiData} from "@data/api-data";
import Axios from "axios";

export async  function createOrgMapping(userType: string): Promise<void> {
  const baseURL = process.env.AM_ORG_ROLE_MAPPING;
  if (!baseURL) {
    throw new Error(
      'AM_ORG_ROLE_MAPPING is not set (set ENVIRONMENT to aat|demo|perftest|ithc, or export AM_ORG_ROLE_MAPPING).'
    );
  }
  if (!process.env.BEARER_TOKEN_AM || !process.env.SERVICE_AUTH_TOKEN_AM) {
    throw new Error('createOrgMapping requires BEARER_TOKEN_AM and SERVICE_AUTH_TOKEN_AM (run after token setup).');
  }

  const payload = amOrgRoleMappingServiceApiData.createOrgMappingPayload();
  if (payload.userIds.length === 0) {
    console.warn('createOrgMapping: no uids in staff.user.data; skipping AM createOrgMapping.');
    return;
  }

  const client = Axios.create(amOrgRoleMappingServiceApiData.amOrgRoleMappingServiceApiInstance());
  const url = amOrgRoleMappingServiceApiData.createOrgMappingApiEndpoint(userType);

  console.log(`createOrgMapping POST ${url} body: ${JSON.stringify(payload)}`);

  try {
    await client.post(url, payload);
  } catch (error: unknown) {
    const err = error as {
      code?: string;
      response?: { status?: number; data?: unknown };
    };
    const status = err.response?.status;

    if (status === 404) {
      console.log(`createOrgMapping: user not found (404), continuing. body=${JSON.stringify(payload)}`);
      return;
    }
  }
}

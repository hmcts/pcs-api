import {cleanupTempUsers} from "@utils/helpers/idam-helpers/idam.helper";

async function globalTeardownConfig() {
  await cleanupTempUsers();
}

export default globalTeardownConfig;

import {createTempUser} from "@utils/helpers/idam-helpers/idam.helper";

async function globalSetupConfig() {
  await createTempUser('caseworker', ['caseworker', 'caseworker-pcs']);
}
export default globalSetupConfig;

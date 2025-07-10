import {createTempUser} from "@utils/helpers/idam-helpers/idam.helper";

async function globalSetupConfig() {
  await createTempUser('exuiUser', ['caseworker', 'caseworker-pcs']);
}
export default globalSetupConfig;

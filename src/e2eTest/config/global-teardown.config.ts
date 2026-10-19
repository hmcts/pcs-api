import { deleteUploadedTestDocuments } from "@utils/common/deleteDocument.util";

async function globalTeardownConfig(): Promise<void> {
  console.log('\n*** GLOBAL TEARDOWN RUNNING ***\n');
  await deleteUploadedTestDocuments();
  console.log('\n✅ GLOBAL TEARDOWN COMPLETED\n');
}
export default globalTeardownConfig;

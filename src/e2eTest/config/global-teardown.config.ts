import { deleteUploadedTestDocuments } from "@utils/common/deleteDocument.util";
import { isLocalStack, localDocumentStoreIsUp } from "@config/local-stack.config";

async function globalTeardownConfig(): Promise<void> {
  console.log('\n*** GLOBAL TEARDOWN RUNNING ***\n');
  // A local run only uploads documents when dm-store is up, so only then is there anything to delete.
  if (!isLocalStack() || (await localDocumentStoreIsUp())) {
    await deleteUploadedTestDocuments();
  }
  console.log('\n✅ GLOBAL TEARDOWN COMPLETED\n');
}
export default globalTeardownConfig;

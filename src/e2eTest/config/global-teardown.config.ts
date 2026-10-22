import { deleteUploadedTestDocuments } from "@utils/common/deleteDocument.util";
import { isLocalStack, localDocumentStoreIsUp } from "@config/local-stack.config";

async function globalTeardownConfig(): Promise<void> {
  console.log('\n*** GLOBAL TEARDOWN RUNNING ***\n');
  if (!isLocalStack() || (await localDocumentStoreIsUp())) {
    await deleteUploadedTestDocuments();
  }
  console.log('\n✅ GLOBAL TEARDOWN COMPLETED\n');
}
export default globalTeardownConfig;

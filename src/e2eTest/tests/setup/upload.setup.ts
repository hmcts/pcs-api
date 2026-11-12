import { uploadTestDocuments } from "@utils/common/uploadDocument.utils";
import { test as setup } from '@playwright/test';


setup('Bootstrap API test dependencies by uploading reusable CCD documents', async () => {
    await setup.step('Upload test documents and generate manifest', async () => {
      await uploadTestDocuments();
    });
  }
);


import { CcdDocument } from "./uploadDocument.utils";
import fs from 'fs';
import path from 'path';

// eslint-disable-next-line import/no-named-as-default
import Axios from 'axios';

const MANIFEST_PATH = path.join(__dirname, '../../.auth/test-documents.json');

export async function deleteTestDocument(document: CcdDocument): Promise<void> {
  try {

    const api = Axios.create({
      headers: {
        Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
        ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      },
    });

    await api.delete(document.document_url);

    console.log(`✅ Deleted ${document.document_filename}`);
  } catch (error) {
    if (Axios.isAxiosError(error)) {
      console.error('Status:', error.response?.status);
      console.error('Body:', error.response?.data);
    }

  }
}

export async function deleteUploadedTestDocuments(): Promise<void> {
  if (!fs.existsSync(MANIFEST_PATH)) {
    return;
  }

  //const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf8')) as Record<string, CcdDocument>;

  // await Promise.all(
  //   Object.values(manifest).map(deleteTestDocument)
  // );

  fs.unlinkSync(MANIFEST_PATH);

  console.log('\n✅ Test document manifest removed\n');
}
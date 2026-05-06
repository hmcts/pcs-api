import * as fs from 'fs';
import * as path from 'path';

export enum RuntimeUserAlias {
  PCS_CLAIMANT_SOLICITOR = 'PCS_CLAIMANT_SOLICITOR',
}

export type ProvisionedSolicitorCredentials = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  uid: string;
};

const RUNTIME_FILE = 'runtime-solicitor.json';

export function getRuntimeSolicitorJsonPath(): string {
  return path.join(__dirname, '../../.auth', RUNTIME_FILE);
}

export function writeRuntimeSolicitorCredentials(creds: ProvisionedSolicitorCredentials): void {
  const filePath = getRuntimeSolicitorJsonPath();
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, JSON.stringify(creds, null, 2), 'utf-8');
}

export function readRuntimeSolicitorCredentials(): ProvisionedSolicitorCredentials | null {
  const filePath = getRuntimeSolicitorJsonPath();
  if (!fs.existsSync(filePath)) {
    return null;
  }
  return JSON.parse(fs.readFileSync(filePath, 'utf-8')) as ProvisionedSolicitorCredentials;
}

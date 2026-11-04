import { Page } from '@playwright/test';
import Axios from 'axios';
import { actionRetries, SHORT_TIMEOUT, VERY_SHORT_TIMEOUT } from 'playwright.config';
import {
  createCaseEventTokenApiData,
  fetchPINsApiData,
  getCaseApiData,
  submitCaseApiData,
  validateAccessCodeApiData,
} from '@data/api-data';
import { actionRecord, IAction } from '@utils/interfaces';

async function waitUntilCaseIssued(): Promise<void> {
  const getCaseApi = Axios.create(createCaseEventTokenApiData.createCaseEventTokenApiInstance());
  for (let attempt = 1; attempt <= actionRetries; attempt++) {
    const response = await getCaseApi.get(getCaseApiData.getCaseApiEndPoint());
    if (String(response.data?.state).trim().toUpperCase() === 'CASE_ISSUED') return;
    if (attempt === actionRetries) throw new Error(`Case is not ISSUED. Last observed status: ${response.data?.state ?? 'UNKNOWN'}`);
    await new Promise(resolve => setTimeout(resolve, SHORT_TIMEOUT));
  }
}

export class FetchPINsAndValidateAccessCodeAPIAction implements IAction {
  async execute(page: Page, action: string, fieldName?: actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['fetchPINsAPI', () => this.fetchPINsAPI(fieldName)],
      ['validateAccessCodeAPI', () => this.validateAccessCodeAPI(fieldName)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async fetchPINsAPI(options?: actionRecord): Promise<void> {
    const fetchPinsApi = Axios.create(fetchPINsApiData.fetchPINSApiInstance());
    await waitUntilCaseIssued();
    for (let attempt = 1; attempt <= actionRetries * 2 + 4; attempt++) {
      const response = await fetchPinsApi.get(fetchPINsApiData.fetchPINsApiEndPoint());
      if (Object.keys(response.data ?? {}).length > 0) {
        const defendantIndex = Number(options?.defendantIndex ?? 1);
        process.env.VALIDATE_ACCESS_CODE = this.getDefendantPin(response.data, defendantIndex);
        const linkedParty = response.data[process.env.VALIDATE_ACCESS_CODE];
        process.env.CITIZEN_DEFENDANT_RANK = this.getDefendantRank(linkedParty);
        return;
      }
      await new Promise(resolve => setTimeout(resolve, SHORT_TIMEOUT));
    }
    throw new Error('PINs were not generated after multiple retries once case reached CASE_ISSUED');
  }

  private getDefendantPin(pinResponse: Record<string, any>, defendantIndex: number): string {
    const payload = submitCaseApiData.submitCasePayloadCaseFileView();
    const defendants = [payload.defendant1, ...(payload.additionalDefendants ?? []).map((defendant: any) => defendant.value)];
    const defendant = defendants[defendantIndex - 1];
    if (!defendant) throw new Error(`Defendant ${defendantIndex} is not present in the submit payload`);
    const matchingPin = Object.entries(pinResponse).find(([, party]) =>
      party?.firstName?.trim().toLowerCase() === defendant.firstName.trim().toLowerCase()
      && party?.lastName?.trim().toLowerCase() === defendant.lastName.trim().toLowerCase()
    )?.[0];
    if (!matchingPin) throw new Error(`No PIN found for Defendant ${defendantIndex}: ${defendant.firstName} ${defendant.lastName}`);
    return matchingPin;
  }

  private getDefendantRank(linkedParty: Record<string, any>): string {
    const payload = submitCaseApiData.submitCasePayloadCaseFileView();
    const defendants = [
      payload.defendant1,
      ...(payload.additionalDefendants ?? []).map((defendant: any) => defendant.value),
    ];
    const index = defendants.findIndex(defendant =>
      defendant?.firstName?.trim().toLowerCase() === linkedParty?.firstName?.trim().toLowerCase()
      && defendant?.lastName?.trim().toLowerCase() === linkedParty?.lastName?.trim().toLowerCase()
    );
    return `Defendant ${index >= 0 ? index + 1 : 1}`;
  }

  private async validateAccessCodeAPI(options?: actionRecord): Promise<void> {
    const validateApi = Axios.create(validateAccessCodeApiData.validateAccessCodeApiInstance());
    const accessCode = process.env.VALIDATE_ACCESS_CODE;
    if (!accessCode) throw new Error('No access code available for validation');
    for (let attempt = 1; attempt <= actionRetries; attempt++) {
      try {
        const response = await validateApi.post(validateAccessCodeApiData.validateAccessCodeApiEndPoint(), { accessCode });
        if (response.status === 200) {
          return;
        }
      } catch (error) {
        if (attempt === actionRetries) throw error;
      }
      await new Promise(resolve => setTimeout(resolve, VERY_SHORT_TIMEOUT));
    }
    throw new Error('Validate access code API failed after multiple retries');
  }

}

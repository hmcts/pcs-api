import Axios from 'axios';
import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { Page } from '@playwright/test';
import { createCaseApiData, createCaseEventTokenApiData, submitCaseApiData, submitCaseEventTokenApiData, caseUserRoleDeletionApiData } from '@data/api-data';
import { user } from '@data/user-data';
import { caseNumber } from './createCase.action';

export let caseInfo: { id: string; fid: string; state: string } = { id: '', fid: '', state: '' };

export class CreateCaseAPIAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCaseAPI', () => this.createCaseAPI(fieldName)],
      ['submitCaseAPI', () => this.submitCaseAPI(fieldName)],
      ['deleteCaseUsers', () => this.deleteCaseUsers()]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async createCaseAPI(caseData: actionData): Promise<void> {
    const createCaseApi = Axios.create(createCaseEventTokenApiData.createCaseEventTokenApiInstance());
    process.env.CREATE_EVENT_TOKEN = (await createCaseApi.get(createCaseEventTokenApiData.createCaseEventTokenApiEndPoint)).data.token;
    const createCasePayloadData = typeof caseData === "object" && "data" in caseData ? caseData.data : caseData;
    const createResponse = await createCaseApi.post(createCaseApiData.createCaseApiEndPoint, {
      data: createCasePayloadData,
      event: { id: createCaseApiData.createCaseEventName },
      event_token: process.env.CREATE_EVENT_TOKEN,
    });
    process.env.CASE_NUMBER = createResponse.data.id;
    caseInfo.id = createResponse.data.id;
    caseInfo.fid = createResponse.data.id.replace(/(.{4})(?=.)/g, "$1-");
    caseInfo.state = createResponse.data.state;
  }

  private async submitCaseAPI(caseData: actionData): Promise<void> {
    const submitCaseApi = Axios.create(submitCaseEventTokenApiData.submitCaseEventTokenApiInstance());
    process.env.SUBMIT_EVENT_TOKEN = (await submitCaseApi.get(submitCaseEventTokenApiData.submitCaseEventTokenApiEndPoint())).data.token;
    const submitCasePayloadData = typeof caseData === "object" && "data" in caseData ? caseData.data : caseData;
    try {
      const submitResponse = await submitCaseApi.post(submitCaseApiData.submitCaseApiEndPoint(), {
        data: submitCasePayloadData,
        event: { id: submitCaseApiData.submitCaseEventName },
        event_token: process.env.SUBMIT_EVENT_TOKEN,
      });
      caseInfo.id = submitResponse.data.id;
      caseInfo.fid = submitResponse.data.id.replace(/(.{4})(?=.)/g, "$1-");
      caseInfo.state = submitResponse.data.state;
    } catch (error: any) {
      const status = error?.response?.status;
      if (status === 404) {
        console.error(submitCaseApiData.submitCasePayload);
        throw new Error(`Submission failed: endpoint not found (404).please check the payload above \n ${error}`);

      }
      if (!status) {
        throw new Error('Submission failed: no response from server.');
      }
      throw new Error(`Submission failed with status ${status}.`);
    }
  }

  private async deleteCaseUsers(): Promise<void> {
    const userId = user.claimantSolicitor.uid;
    let caseId = (caseInfo.id || process.env.CASE_NUMBER || caseNumber || '').replace(/-/g, '');

    if (!caseId) {
      console.warn('No case ID available for case user removal. Skipping...');
      return;
    }

    if (!userId) {
      console.warn('No user ID available for case user removal. Skipping...');
      return;
    }

    const deleteCaseUsersApi = Axios.create(caseUserRoleDeletionApiData.deleteCaseUsersApiInstance());
    const caseRole = '[CREATOR]';

    try {
      const payload = caseUserRoleDeletionApiData.deleteCaseUsersPayload(caseId, userId, caseRole);
      await deleteCaseUsersApi.delete(caseUserRoleDeletionApiData.deleteCaseUsersApiEndPoint, { data: payload });
      console.log(`\n✅ CASE USER CLEANUP:`);
      console.log(`   Successfully removed case user: ${userId} with role ${caseRole} from case ${caseId}`);
    } catch (error: any) {
      const status = error?.response?.status;
      const errorMessage = `Case ID: ${caseId}, User ID: ${userId}`;

      if (status === 404) {
        console.warn(`Case user removal failed: case or user not found (404). ${errorMessage}`);
      } else if (status === 403) {
        console.warn(`Case user removal failed: insufficient permissions (403). ${errorMessage}`);
      } else if (!status) {
        console.error('Case user removal failed: no response from server.');
      } else {
        console.error(`Case user removal failed with status ${status}. ${errorMessage}`);
      }
    }
  }
}

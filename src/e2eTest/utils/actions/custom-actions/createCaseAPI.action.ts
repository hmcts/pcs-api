import Axios from 'axios';
import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { Page } from '@playwright/test';
import { createCaseApiData, createCaseEventTokenApiData, submitCaseApiData, submitCaseEventTokenApiData, caseUserRoleDeletionApiData } from '@data/api-data';
import { user } from '@data/user-data';
import { caseNumber } from './createCase.action';
import { enforceWarrantApiData } from '@data/api-data/enforceTheOrderWarrant.api.data';
import { enforceOrderEventTokenApiData } from '@data/api-data/enforceOrderEventToken.api.data';

export let caseInfo: { id: string; fid: string; state: string } = { id: '', fid: '', state: '' };

export class CreateCaseAPIAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCaseAPI', () => this.createCaseAPI(fieldName)],
      ['submitCaseAPI', () => this.submitCaseAPI(fieldName)],
      ['deleteCaseRole', () => this.deleteCaseRole(fieldName)],
      ['enforceCaseAPI', () => this.enforceCaseAPI(fieldName)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async createCaseAPI(caseData: actionData): Promise<void> {
    const createCaseApi = Axios.create(createCaseEventTokenApiData.createCaseEventTokenApiInstance());
    try {
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
    } catch (error: any) {
      const status = error?.response?.status;
      const responseBody = error?.response?.data;

      console.error("=== ERROR RESPONSE ===");
      console.error("HTTP Status:", status);
      console.error("Exception:", responseBody?.exception);
      console.error("Error:", responseBody?.error);
      console.error("Message:", responseBody?.message);
      console.error("Path:", responseBody?.path);
      console.error("Timestamp:", responseBody?.timestamp);
      console.error("Full response body:", JSON.stringify(responseBody, null, 2));

      if (!status) {
        throw new Error(`Case creation failed: no response from server`);
      }
      throw new Error(`Case creation failed with status ${status}.Response received is ${responseBody?.message}}`);
    }
  }

  private async submitCaseAPI(caseData: actionData): Promise<void> {
    const submitCaseApi = Axios.create(submitCaseEventTokenApiData.submitCaseEventTokenApiInstance());
    try {
      process.env.SUBMIT_EVENT_TOKEN = (await submitCaseApi.get(submitCaseEventTokenApiData.submitCaseEventTokenApiEndPoint())).data.token;
      const submitCasePayloadData = typeof caseData === "object" && "data" in caseData ? caseData.data : caseData;
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
      const responseBody = error?.response?.data;
      if (status === 404) {
        console.error(submitCaseApiData.submitCasePayload);
        throw new Error(`Submission failed: endpoint not found (404).please check the payload above \n ${error}`);
      }
      console.error("=== ERROR RESPONSE ===");
      console.error("HTTP Status:", status);
      console.error("Exception:", responseBody?.exception);
      console.error("Error:", responseBody?.error);
      console.error("Message:", responseBody?.message);
      console.error("Path:", responseBody?.path);
      console.error("Timestamp:", responseBody?.timestamp);
      console.error("Full response body:", JSON.stringify(responseBody, null, 2));

      if (!status) {
        throw new Error('Submission failed: no response from server.');
      }
      throw new Error(`Submission failed with status ${status}.Response received is ${responseBody?.message}}`);
    }
  }

  private async enforceCaseAPI(caseData: actionData): Promise<void> {
    const enforceCaseApi = Axios.create(enforceOrderEventTokenApiData.submitCaseEventTokenApiInstance());
    try {
      process.env.ENFORCE_EVENT_TOKEN = (await enforceCaseApi.get(enforceOrderEventTokenApiData.enforceEventTokenApiEndPoint())).data.token;
      const enforceCasePayloadData = typeof caseData === "object" && "data" in caseData ? caseData.data : caseData;
      const enforceResponse = await enforceCaseApi.post(enforceWarrantApiData.enforceCaseApiEndPoint(), {
        data: enforceCasePayloadData,
        event: { id: enforceWarrantApiData.enforceCaseEventName },
        event_token: process.env.ENFORCE_EVENT_TOKEN,
      });
      caseInfo.id = enforceResponse.data.id;
      caseInfo.fid = enforceResponse.data.id.replace(/(.{4})(?=.)/g, "$1-");
      caseInfo.state = enforceResponse.data.state;
    } catch (error: any) {
      const status = error?.response?.status;
      const responseBody = error?.response?.data;
      if (status === 404) {
        console.error(enforceWarrantApiData.enforceCasePayloadYesJourney);
        throw new Error(`Enforce the order failed: endpoint not found (404).please check the payload above \n ${error}`);
      }
      console.error("=== ERROR RESPONSE ===");
      console.error("HTTP Status:", status);
      console.error("Exception:", responseBody?.exception);
      console.error("Error:", responseBody?.error);
      console.error("Message:", responseBody?.message);
      console.error("Path:", responseBody?.path);
      console.error("Timestamp:", responseBody?.timestamp);
      console.error("Full response body:", JSON.stringify(responseBody, null, 2));

      if (!status) {
        throw new Error('Enforce the order failed: no response from server.');
      }
      throw new Error(`Enforce the order failed with status ${status}.Response received is ${responseBody?.message}}`);
    }
  }

  private async deleteCaseRole(roleData: actionData): Promise<void> {
    const userId = user.claimantSolicitor.uid;
    let caseId = (caseInfo.id || process.env.CASE_NUMBER || caseNumber || '').replace(/-/g, '');
    const caseRole = typeof roleData === 'string' ? roleData : String(roleData);

    if (!caseId) {
      console.warn('No case ID available for case user removal.');
      return;
    }

    if (!userId) {
      console.warn('No user ID available for case user removal.');
      return;
    }

    const deleteCaseUsersApi = Axios.create(caseUserRoleDeletionApiData.deleteCaseUsersApiInstance());

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
        console.warn('Case user removal failed: no response from server.');
      } else {
        console.warn(`Case user removal failed with status ${status}. ${errorMessage}`);
      }
    }
  }
}
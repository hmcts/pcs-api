import Axios from 'axios';
import {actionData, actionRecord, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {createCaseApiData, createCaseEventTokenApiData, submitCaseApiData, submitCaseEventTokenApiData} from '@data/api-data';

export let caseInfo: { id: string; fid: string; state: string } = { id: '', fid: '', state: '' };

export class CreateCaseAPIAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCaseAPI', () => this.createCaseAPI(fieldName)],
      ['submitCaseAPI', () => this.submitCaseAPI(fieldName)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async createCaseAPI(caseData: actionData): Promise<void> {
    const createCaseApi = Axios.create(createCaseEventTokenApiData.createCaseApiInstance());
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
    const submitCaseApi = Axios.create(submitCaseEventTokenApiData.createCaseApiInstance());
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
  }
}

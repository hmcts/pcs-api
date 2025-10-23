import { IAction } from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {accessTokenApiData} from "@data/api-data/accessToken.api.data";
import {IdamUtils} from "@hmcts/playwright-common";

export class getAccessTokenAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    process.env.IDAM_WEB_URL = accessTokenApiData.idamUrl;
    process.env.IDAM_TESTING_SUPPORT_URL = accessTokenApiData.idamTestingSupportUrl;
    process.env.CREATE_USER_BEARER_TOKEN = await new IdamUtils().generateIdamToken({
      grantType: 'client_credentials',
      clientId: 'pcs-api',
      clientSecret: process.env.PCS_API_IDAM_SECRET as string,
      scope: 'profile roles'
    });
  };
}

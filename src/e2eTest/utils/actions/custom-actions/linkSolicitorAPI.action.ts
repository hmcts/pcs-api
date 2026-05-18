import { Page } from '@playwright/test';
import Axios from 'axios';


import { IAction } from '../../interfaces';
import { linkSolicitorTokenApiData } from '@data/api-data/linkSolicitorEventToken.api.data';
import { user } from '@data/user-data';
import { linkSolicitorApiData } from '@data/api-data/linkSolicitor.api.data';

export let pins: string[] = [];
export let firstName: string = '';
export let lastName: string = '';
export let address: string = '';

export class LinkSolicitorAPIAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['linkSolicitorAPI', () => this.linkSolicitorAPI()],

    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async linkSolicitorAPI(): Promise<void> {
    console.log(`${process.env.MANAGE_CASE_BASE_URL}/testing-support/link-defendant-solicitor-to-party/${process.env.CASE_NUMBER}/${process.env.Defendant_ID}`)
    await this.generateSolicitorAccessToken();
    console.log('token1:' + process.env.SOLICITOR_ACCESS_TOKEN)
    const linkSolicitorApi = Axios.create(linkSolicitorTokenApiData.linkSolicitorTokenApiInstance());
    const LINK_EVENT_TOKEN = (await linkSolicitorApi.get(linkSolicitorTokenApiData.linkSolicitorTokenApiEndPoint())).data.token;
    //console.log('data is :'+JSON.stringify(LINK_EVENT_TOKEN));


    console.log('token2:' + LINK_EVENT_TOKEN)
    // process.env.SOLICITOR_ACCESS_TOKEN = (await linkSolicitorApi.get(linkSolicitorTokenApiData.linkSolicitorApiEndPoint())).data.token;;

    const response = await linkSolicitorApi.post(linkSolicitorApiData.linkSolicitorApiEndPoint(), {
      event: { id: linkSolicitorApiData.makeAnApplicationEventName },
      event_token: LINK_EVENT_TOKEN,

    });
    console.log(response.status);


  }

  private async generateSolicitorAccessToken(): Promise<void> {
    const { IdamUtils } = await import('@hmcts/playwright-common');
    process.env.SOLICITOR_ACCESS_TOKEN = await new IdamUtils().generateIdamToken({
      username: user.defendantSolicitor.email,
      password: process.env.IDAM_PCS_USER_PASSWORD,
      grantType: 'password',
      clientId: 'pcs-api',
      clientSecret: process.env.PCS_API_IDAM_SECRET as string,
      scope: 'profile openid roles',
    });
  }


}



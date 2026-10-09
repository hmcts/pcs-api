import {Page} from '@playwright/test';
import {addressDetails} from '@data/page-data';
import {contactDetailsLR} from '@data/page-data-figma';
import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {performAction, performActions} from '@utils/controller';
import Axios from "axios";
import {midEventLRRespondPossessionClaimApiData} from "@data/api-data/respondPossessionClaimMidEventLR.api.data";
import {actionRetries, VERY_SHORT_TIMEOUT} from "../../../../playwright.config";
import {submitPossessionClaimResponseApiDataForLR} from "@data/api-data/respondPossessionClaimSubmitLR.api.data";
import {
  respondPossessionClaimSolicitorEventTokenApiData
} from "@data/api-data/respondPossessionClaimEventToken.api.data";

export class RespondToAClaimAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectRespondToClaimContactPreferences', () => this.selectContactPreferences(fieldName as actionRecord)],
      ['midEventRespondPossessionClaimLRAPI', () => this.midEventRespondPossessionClaimLRAPI()],
      ['submitPossessionClaimResponseLRAPI', () => this.submitPossessionClaimResponseLRAPI()],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectContactPreferences(preferences: actionRecord) {
    const prefData = preferences as {
      notifications: string;
      correspondenceAddress: string;
      phoneNumber?: string;
      representativeReference?: string;
    };
    if (prefData.representativeReference) {
      await performAction(
        'inputText',
        contactDetailsLR.defendantLegalRepresentativeReferenceTextLabel,
        prefData.representativeReference
      );
    }
    await performAction('clickRadioButton', {
      question: contactDetailsLR.doYouWantToUseQuestion,
      option: preferences.notifications
    });
    if (preferences.notifications === contactDetailsLR.noRadioOption) {
      await performAction('inputText', contactDetailsLR.enterEmailAddressHiddenTextLabel, contactDetailsLR.enterEmailAddressTextInput);
    }
    await performAction('clickRadioButton', {
      question: contactDetailsLR.doYouWantToEnterDifferentPostalAddressQuestion,
      option: preferences.correspondenceAddress
    });
    if (preferences.correspondenceAddress === contactDetailsLR.yesRadioOption) {
      await performActions(
        'Find Address based on postcode',
        ['inputText', contactDetailsLR.enterAUkPostcodeHiddenTextLabel, addressDetails.englandCourtAssignedPostcodeTextInput],
        ['clickButton', contactDetailsLR.findAddressHiddenButton],
        ['select', contactDetailsLR.selectAnAddressHiddenSelectLabel, addressDetails.addressIndex]
      );
    }
    if (prefData.phoneNumber) {
      await performAction('clickRadioButton', {
        question: contactDetailsLR.doYouWantToProvideQuestion,
        option: prefData.phoneNumber
      });
      if (prefData.phoneNumber === contactDetailsLR.yesRadioOption) {
        await performAction('inputText', contactDetailsLR.enterPhoneNumberHiddenTextLabel, contactDetailsLR.enterPhoneNumberTextInput);
      }
    }
    await performAction('clickButton', contactDetailsLR.saveAndContinueButton);
  }


  private async midEventRespondPossessionClaimLRAPI(): Promise<void> {
    const midEventHeader = midEventLRRespondPossessionClaimApiData.midEventLRRespondPossessionClaimApiInstance();
    const validateApi = Axios.create(midEventHeader);

    // Prime the draft: the START event creates the party-keyed draft row that the
    // mid-event save requires. Without it the callback throws UnsubmittedDataException.
    await validateApi.get(`/cases/${process.env.CASE_NUMBER}/event-triggers/respondPossessionClaim`);

    const maxRetries = actionRetries;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      const midEventPayload = midEventLRRespondPossessionClaimApiData.midEventLRRespondPossessionClaimPayload();
      try {
        const response = await validateApi.post(
          midEventLRRespondPossessionClaimApiData.midEventLRRespondPossessionClaimApiEndPoint(),
          midEventPayload
        );

        console.log(`\n✅ MID EVENT LEGAL REPRESENTATIVE RESPONSE SUCCESSFUL:`);
        console.log(`Status Code: ${response.status}`);
        console.log(`Mid event Successful for legal representative response to Case ${process.env.CASE_NUMBER}`);

        break;
      } catch (error: unknown) {
        if (Axios.isAxiosError(error)) {
          const status = error.response?.status;
          const responseBody = error.response?.data;

          console.error('=== ERROR RESPONSE ===');
          console.error('HTTP Status:', status);
          console.error('Exception:', responseBody?.exception);
          console.error('Error:', responseBody?.error);
          console.error('Message:', responseBody?.message);
          console.error('Path:', responseBody?.path);
          console.error('Timestamp:', responseBody?.timestamp);
          console.error('Callback errors:', responseBody?.callbackErrors);

          if (status === 404) {
            throw error;
          }

          if (attempt === maxRetries) {
            throw error;
          }

          console.warn(`⚠️ Retry attempt ${attempt} failed. Retrying...`);

          await new Promise(resolve => setTimeout(resolve, VERY_SHORT_TIMEOUT));

          continue;
        }

        if (attempt === maxRetries) {
          throw new Error('Validate Respond Possession Claim failed due to an unexpected error.');
        }

        console.warn(`⚠️ Retry attempt ${attempt} failed. Retrying...`);

        await new Promise(resolve => setTimeout(resolve, VERY_SHORT_TIMEOUT));
      }
    }
  }

  private async submitPossessionClaimResponseLRAPI(): Promise<void> {
    const submitPossessionClaimResponseApi = Axios.create(
      submitPossessionClaimResponseApiDataForLR.submitPossessionClaimResponseApiInstance()
    );

    const startEvent = (
      await submitPossessionClaimResponseApi.get(
        respondPossessionClaimSolicitorEventTokenApiData.respondPossessionClaimSolicitorApiEndPoint()
      )
    ).data;
    const RESPONDCLAIM_EVENT_TOKEN = startEvent.token;
    // pcs-api rejects a submit whose draftVersion isn't the stored one.
    const draftVersion = startEvent.case_details?.case_data?.possessionClaimResponse?.draftVersion;

    const maxRetries = actionRetries;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        const submitResponseLR = await submitPossessionClaimResponseApi.post(
          submitPossessionClaimResponseApiDataForLR.submitPossessionClaimResponseApiEndPoint(),
          submitPossessionClaimResponseApiDataForLR.submitPossessionClaimResponsePayload(
            RESPONDCLAIM_EVENT_TOKEN,
            draftVersion
          )
        );

        console.log('\n✅ SUBMIT LEGAL REPRESENTATIVE RESPONSE SUCCESSFUL:');
        console.log(`Status Code: ${submitResponseLR.status}`);
        console.log(`Successfully submitted legal representative response for Case number ${process.env.CASE_NUMBER}`);

        break;
      } catch (error: unknown) {
        if (Axios.isAxiosError(error)) {
          const status = error.response?.status;
          const responseBody = error.response?.data;

          console.error('=== ERROR RESPONSE ===');
          console.error('HTTP Status:', status);
          console.error('Exception:', responseBody?.exception);
          console.error('Error:', responseBody?.error);
          console.error('Message:', responseBody?.message);
          console.error('Path:', responseBody?.path);
          console.error('Timestamp:', responseBody?.timestamp);
          console.error('Callback errors:', responseBody?.callbackErrors);
          console.error('Draft version sent:', draftVersion);

          if (status === 404) {
            throw error;
          }

          if (attempt === maxRetries) {
            throw error;
          }

          console.warn(`⚠️ Retry attempt ${attempt} failed. Retrying...`);

          await new Promise(resolve => setTimeout(resolve, VERY_SHORT_TIMEOUT));

          continue;
        }

        if (attempt === maxRetries) {
          throw new Error('Submitting possession claim response failed due to an unexpected error.');
        }

        console.warn(`⚠️ Retry attempt ${attempt} failed. Retrying...`);

        await new Promise(resolve => setTimeout(resolve, VERY_SHORT_TIMEOUT));
      }
    }
  }
}

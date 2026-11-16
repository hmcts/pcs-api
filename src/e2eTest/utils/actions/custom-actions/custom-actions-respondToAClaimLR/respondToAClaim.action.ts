import {expect, Page} from '@playwright/test';
import {addressDetails, home} from '@data/page-data';
import {contactDetailsLR} from '@data/page-data-figma';
import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {performAction, performActions, performValidation} from '@utils/controller';
import {confirmDeleteDraftResponse, deleteDraftResponse} from "@data/page-data-figma/page-data-legalRepresentative";
import { caseInfo } from '../createCaseAPI.action';
import {getCaseTypeId} from "@utils/common/caseType.utils";
import {VERY_LONG_TIMEOUT} from "../../../../playwright.config";
export let addressInfo: { buildingStreet: string; addressLine2: string; townCity: string; country: string; engOrWalPostcode: string; };

export class RespondToAClaimAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectRespondToClaimContactPreferences', () => this.selectContactPreferences(fieldName as actionRecord)],
      ['navigateToSummaryPage', () => this.navigateToSummaryPage(page)],
      ['deleteDraftResponseOption', () => this.deleteDraftResponseOption(fieldName as actionRecord)],
      ['confirmDeleteDraftResponse',() => this.confirmDeleteDraftResponse()],
      ['getAddressInfo',()=> this.getAddressInfo(fieldName as actionRecord)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async navigateToSummaryPage(page: Page) {
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`, { waitUntil: 'domcontentloaded' });
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
    await page.waitForLoadState();
    await page.locator('.spinner-container').waitFor({ state: 'detached' });
    await performValidation('mainHeader', home.caseSummary);
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


  private async deleteDraftResponseOption(deleteResponse: actionRecord){
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: deleteResponse.question,
      option: deleteResponse.option,
    });
    await performAction('clickButton', deleteDraftResponse.continueButton);
  }

  private async confirmDeleteDraftResponse(){
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`});
    await performValidation('text', { elementType: 'inlineText', text: confirmDeleteDraftResponse.yourDraftResponseDeletedParagraph });
    await performAction('clickButton', confirmDeleteDraftResponse.closeAndReturnToCaseDetailsButton);
  }

  private async getAddressInfo(address: actionRecord) {
    let createCasePayLoad = address.data as Record<string, any>;
    addressInfo = {
      buildingStreet: createCasePayLoad.propertyAddress.AddressLine1,
      addressLine2: createCasePayLoad.propertyAddress.AddressLine2,
      townCity: createCasePayLoad.propertyAddress.PostTown,
      country: createCasePayLoad.propertyAddress.Country,
      engOrWalPostcode: createCasePayLoad.propertyAddress.PostCode
    };

  }
}

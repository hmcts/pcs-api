
import { expect, Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { createCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { performAction, performActions, performValidation } from '@utils/controller-caseManagement';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary, home } from '@data/page-data';
import {
  changeCaseState, confirmCaseStateChange, enterGenappApplication, enterGenAppapplicationFee,
  enterGenAppConsentAndNotice, enterGenAppHearingDate, selectDocument
} from '@data/page-data-figma/page-data-caseManagement-figma';
import { caseInfo } from '../createCaseAPI.action';
import { CaseManagementCommonUtils } from './caseManagementUtils.action';


export const addressInfo = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  addressLine2: createCaseApiData.createCasePayload.propertyAddress.AddressLine2,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};
const cyaMap = new Map<string, string>();
export let allPartyDetails: string[] = [];

export class CaseManagementAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['navigateToSummaryPage', () => this.navigateToSummaryPage(page)],
      ['selectAnEvent', () => this.selectAnEvent(fieldName as actionRecord)],
      ['selectDocumentToAmend', () => this.selectDocumentToAmend(fieldName as actionRecord)],
      ['changeCaseState', () => this.changeCaseState(fieldName as actionRecord)],
      ['confirmCaseStateChange', () => this.confirmCaseStateChange()],
      ['getAllPartyDetails', () => this.getAllPartyDetails(fieldName as actionRecord)],
      ['enterApplicationDetails', () => this.enterApplicationDetails(fieldName as actionRecord)],
      ['confirmIfCourtHearingInNext14Days', () => this.confirmIfCourtHearingInNext14Days(fieldName as actionRecord)],
      ['enterApplicationFeeDetails', () => this.enterApplicationFeeDetails(fieldName as actionRecord)],
      ['enterApplicationConsentAndNotice', () => this.enterApplicationConsentAndNotice(fieldName as actionRecord)],
      ['inputErrorValidation', () => this.inputErrorValidation(page, fieldName as actionRecord)],

    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
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

  private async selectAnEvent(event: actionRecord) {
    await performAction('select', caseSummary.nextStepEventList, event.eventType);
    await performAction('clickButton', caseSummary.go);
  }

  private async selectDocumentToAmend(selectDoc: actionRecord) {
    await performAction('select', selectDoc.question, selectDoc.option);
    await performAction('clickRadioButton', { question: selectDoc.question1, option: selectDoc.option1 });
    await performAction('reTryOnCallBackError', selectDocument.continueButton, selectDoc.nextPage as string);
  }

  private async changeCaseState(caseState: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('select', caseState.question, caseState.option);
    await performAction('reTryOnCallBackError', changeCaseState.continueButton, caseState.nextPage as string);
  }

  private async confirmCaseStateChange(): Promise<void> {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', confirmCaseStateChange.mainHeader);
    await performAction('clickButton', confirmCaseStateChange.closeAndReturnToCaseOverviewButton);
  }

  private async getAllPartyDetails(allPartiesDetails: actionRecord) {

    let originalDefendantDetails: string[] = [];
    const payLoad = allPartiesDetails.payLoad as Record<string, any>;
    if (allPartiesDetails.defendant1NameKnown === 'YES') {
      originalDefendantDetails.push(
        `${payLoad.defendant1.firstName} ${payLoad.defendant1.lastName} - Defendant 1`
      );
    } else {
      originalDefendantDetails.push(
        `null null`
      );
    }

    if (allPartiesDetails.additionalDefendants === 'YES') {

      for (const [index, defendant] of payLoad.additionalDefendants.entries()) {
        if (defendant.value.nameKnown === 'YES') {
          originalDefendantDetails.push(`${defendant.value.firstName} ${defendant.value.lastName} - Defendant ${index + 2}`);
        } else {
          originalDefendantDetails.push(
            `null null`
          );
        }
      }
    }

    allPartyDetails = [...new Set(originalDefendantDetails.filter(n => n.trim().toLowerCase() !== "null null")),
      ...originalDefendantDetails.filter(n => n.trim().toLowerCase() === "null null")
    ];
    allPartyDetails.push(`${payLoad.claimantName} - Claimant 1`);
  }

  private async enterApplicationDetails(appDetails: actionRecord) {
    let date = CaseManagementCommonUtils.getRandomDate(appDetails.dateType as string);
    await performAction('clickRadioButton', { question: appDetails.question1, option: appDetails.option1 });

    await performActions('Enter Date',
      ['inputText', appDetails.label1, date.split('/')[0]],
      ['inputText', appDetails.label2, date.split('/')[1]],
      ['inputText', appDetails.label3, date.split('/')[2]]);
    await performAction('clickRadioButton', { question: appDetails.question2, option: appDetails.option2 });
    if (appDetails.option2 === 'Something else') {
      performAction('inputText', appDetails.label, CaseManagementCommonUtils.generateRandomString(appDetails.input as number))
    }
    await performAction('reTryOnCallBackError', enterGenappApplication.continueButton, appDetails.nextPage as string);
  }

  private async confirmIfCourtHearingInNext14Days(courtHearing: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', {
      question: courtHearing.question,
      option: courtHearing.option,
    });
    await performAction('reTryOnCallBackError', enterGenAppHearingDate.continueButton, courtHearing.nextPage as string);
  }

  private async enterApplicationFeeDetails(fee: actionRecord) {

    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', {
      question: fee.question1,
      option: fee.option1,
    });
    if (fee.option1 === 'Yes') {
      await performAction('inputText', fee.label1, CaseManagementCommonUtils.getRandomNumberAsString(1, 500));
    }
    await performAction('clickRadioButton', {
      question: fee.question2,
      option: fee.option2,
    });

    if (fee.option2 === 'Yes') {
      await performAction('inputText', fee.label2, CaseManagementCommonUtils.generateRandomString(fee.input as number));
    }
    if (fee.option1 === 'No') {
      await performValidation('text', {
        elementType: 'paragraph',
        text: enterGenAppapplicationFee.yourMustRequestPaymentHiddenParagraph
      });
      await performAction('clickButton', enterGenAppapplicationFee.continueButton);
      await performValidation('errorMessage', { header: enterGenAppapplicationFee.eventCouldNotBeCreatedErrorMessageHeader, message: enterGenAppapplicationFee.yourMustRequestPaymentHiddenParagraph });

    } else {
      await performAction('reTryOnCallBackError', enterGenAppHearingDate.continueButton, fee.nextPage as string);
    }

  }

  private async enterApplicationConsentAndNotice(confirmApplicationConsent: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: confirmApplicationConsent.question1,
      option: confirmApplicationConsent.option1,
    });
    if(confirmApplicationConsent.option1 == 'No') {
      await performAction('clickRadioButton', {
        question: confirmApplicationConsent.question2,
        option: confirmApplicationConsent.option2,
      });
    }
    await performAction('reTryOnCallBackError', enterGenAppConsentAndNotice.continueButton, confirmApplicationConsent.nextPage as string);

  }

  private async inputErrorValidation(page: Page, validationArr: actionRecord) {

    if (Array.isArray(validationArr.inputArray)) {
      for (const item of validationArr.inputArray) {
        switch (validationArr.validationType) {
          case 'radioOptions':
            await performAction('clickButton', validationArr.button);
            await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errInlineMessage);
            await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option });
            break;

          case 'checkBox':
            await performAction('clickButton', validationArr.button);
            await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errMessage);
            await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            await performAction('check', validationArr.checkBox);
            break;

          case 'checkBoxPageLevel':
            await performAction('clickButton', validationArr.button);
            await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            await performAction('check', validationArr.checkBox);
            break;

          case 'dropDown':
            await performAction('clickButton', validationArr.button);
            await expect(async () => {
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            }).toPass({
              timeout: VERY_LONG_TIMEOUT,
            });
            await performAction('select', validationArr.dropQn, validationArr.option);
            break;

          case 'textField':
            if (item.type === 'valid') {
              await performAction('inputText', validationArr.label, CaseManagementCommonUtils.generateRandomString(item.input));
            } else {
              await performAction('inputText', validationArr.label, CaseManagementCommonUtils.generateRandomString(item.input));
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                if (item.type === 'moreThanMax') {
                  await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
                } else {
                  await performValidation('inputError', validationArr.label, item.errMessage);
                  await performValidation('errorMessage', validationArr.label, item.errMessage);
                }
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
            }
            break;

          case 'dateField':
            let date: string = CaseManagementCommonUtils.getRandomDate(item.type as string);
            const enterDate = () =>
              performActions(
                'Enter Date',
                ['inputText', validationArr.label1, date.split('/')[0]],
                ['inputText', validationArr.label2, date.split('/')[1]],
                ['inputText', validationArr.label3, date.split('/')[2]]
              );

            if (item.type === 'empty') {
              await performAction('clickButton', validationArr.button);
              await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errInlineMessage);
              await performValidation('errorMessage', validationArr.header1, item.errMessage);
            } else if (item.type === 'past') {
              await enterDate();
            } else if (item.type === 'invalid') {
              await enterDate();
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', validationArr.header1, item.errMessage);
            }
            else {
              await enterDate();
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
            }
            break;

          case 'moneyField':
            await performAction('inputText', validationArr.label, item.input);
            await expect(async () => {
              await performAction('clickButton', validationArr.button);
              //await performValidation('errorMessage', { header: !validationArr?.header ? validationArr.header = 'The event could not be created' : validationArr.header, message: item.errMessage });
              await performValidation('inputError', validationArr.label, item.errMessage);
            }).toPass({
              timeout: VERY_LONG_TIMEOUT,
            });
            break;

          default:
            throw new Error(`Validation type :"${validationArr.validationType}" is not valid`);
        }
      }
    }
    if (validationArr.buttonRemove) {
      await performAction('removeFile');
      await page.waitForTimeout(6000);
    }
  }
}

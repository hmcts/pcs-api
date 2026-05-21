import { areThereAnyReasonsThatThisApplicationShouldNotBeShared, checkYourAnswersGenApps, chooseAnApplication, doYouWantToUploadDocumentToSupportYourApplication, haveTheOtherPartiesAgreedToThisApplication, haveYouAlreadyAppliedForHelpWithFees, helpPayingTheFee, isTheCourtHearingInTheNext14Days, whatOrderDoYouWantTheCourtToMakeAndWhy, whichLanguageDidYouUseToCompleteThisService } from '@data/page-data-figma/page-data-genApps-figma';
import { Page, expect, test } from '@playwright/test';
import { compareMaps } from '@utils/common/compareMaps.util';
import { performAction, performValidation } from '@utils/controller-genApps';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { FieldsStore } from './recordAnsweredFields.action';
import { generateRandomString, stringToCamelCase } from '@utils/common/string.utils';
import { defaultJourney, journeys } from '@utils/common/journeyMappingGenApps';
import { selectParty } from '@data/page-data-figma/page-data-genApps-figma/makeAnApplicationselectParty.page.data';
import { caseInfo } from '../createCaseAPI.action';
import { createCaseApiData } from '@data/api-data';


export const addressInfo = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  addressLine2: createCaseApiData.createCasePayload.propertyAddress.AddressLine2,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};
const cyaMap = new Map<string, string>();
export let defendantDetails: string[] = [];

export class GenAppsAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['chooseAnApplication', () => this.chooseAnApplication(fieldName as actionRecord)],
      ['confirmIfCourtHearingInNext14Days', () => this.confirmIfCourtHearingInNext14Days(fieldName as actionRecord)],
      ['doYouNeedHelpPayingFee', () => this.doYouNeedHelpPayingFee(fieldName as actionRecord)],
      ['confirmYouHaveAppliedForFeeHelp', () => this.confirmYouHaveAppliedForFeeHelp(fieldName as actionRecord)],
      ['confirmOtherPartiesAgreed', () => this.confirmOtherPartiesAgreed(fieldName as actionRecord)],
      ['confirmOrderDoYouWant', () => this.confirmOrderDoYouWant(fieldName as actionRecord)],
      [
        'reasonsApplicationShouldNotBeShared',
        () => this.reasonsApplicationShouldNotBeShared(fieldName as actionRecord),
      ],
      ['selectLanguageUsedToComplete', () => this.selectLanguageUsedToComplete(fieldName as actionRecord)],
      ['selectStatementOfTruth', () => this.selectStatementOfTruth(fieldName as actionRecord)],
      ['inputErrorValidationGenApp', () => this.inputErrorValidationGenApp(fieldName as actionRecord)],
      ['retrieveCYATableData', () => this.retrieveCYATableData(page)],
      ['validateCYA', () => this.validateCYA()],
      ['reviewCYA', () => this.reviewCYA(page, fieldName as actionData)],
      ['reviewAndUpdateCYA', () => this.reviewAndUpdateCYA(page, fieldName as actionRecord)],
      ['getDefendantDetails', () => this.getDefendantDetails(fieldName as actionRecord)],
      ['selectApplicant', () => this.selectApplicant(fieldName as actionRecord)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async getDefendantDetails(defendantsDetails: actionRecord) {

    let originalDefendantDetails: string[] = [];
    const payLoad = defendantsDetails.payLoad as Record<string, any>;
    if (defendantsDetails.defendant1NameKnown === 'YES') {
      originalDefendantDetails.push(
        `${payLoad.defendant1.firstName} ${payLoad.defendant1.lastName}`
      );
    } else {
      originalDefendantDetails.push(
        `null null`
      );
    }

    if (defendantsDetails.additionalDefendants === 'YES') {

      for (const defendant of payLoad.additionalDefendants) {
        if (defendant.value.nameKnown === 'YES') {
          originalDefendantDetails.push(`${defendant.value.firstName} ${defendant.value.lastName}`);
        } else {
          originalDefendantDetails.push(
            `null null`
          );
        };
      };
    }
    defendantDetails = [...new Set(originalDefendantDetails.filter(n => n.trim().toLowerCase() !== "null null")),
    ...originalDefendantDetails.filter(n => n.trim().toLowerCase() === "null null")
    ];

  }

  private async selectApplicant(applicant: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('recordUserEntry', applicant);
    await performAction('clickRadioButton', { question: applicant.question, option: applicant.option });
    await performAction('clickButton', selectParty.continueButton);
  }

  private async chooseAnApplication(chooseApp: actionRecord) {
    await performAction('recordUserEntry', chooseApp);
    await performAction('clickRadioButton', {
      question: chooseApp.question,
      option: chooseApp.option,
    });
    await performAction('clickButton', chooseAnApplication.continueButton);
  }

  private async confirmIfCourtHearingInNext14Days(courtHearing: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('recordUserEntry', courtHearing);
    await performAction('clickRadioButton', {
      question: courtHearing.question,
      option: courtHearing.option,
    });
    await performAction('clickButton', isTheCourtHearingInTheNext14Days.continueButton);
  }

  private async doYouNeedHelpPayingFee(feeHelp: actionRecord) {
    await performAction('recordUserEntry', feeHelp);
    await performAction('clickRadioButton', {
      question: feeHelp.question,
      option: feeHelp.option,
    });
    await performAction('clickButton', helpPayingTheFee.continueButton);
  }

  private async confirmYouHaveAppliedForFeeHelp(confirmFeeHelp: actionRecord) {
    await performAction('recordUserEntry', confirmFeeHelp);
    await performAction('clickRadioButton', {
      question: confirmFeeHelp.question,
      option: confirmFeeHelp.option,
    });
    if (confirmFeeHelp.option === 'Yes') {
      const userInput =
        typeof confirmFeeHelp.input === 'number'
          ? generateRandomString(confirmFeeHelp.input)
          : (confirmFeeHelp.input as string);
      await performAction('inputText', confirmFeeHelp.label, userInput);
      FieldsStore.update(confirmFeeHelp.label as string, userInput);
      FieldsStore.rename(confirmFeeHelp.label as string, 'What is your Help with Fees reference number?');
    } else {
      FieldsStore.delete('What is your Help with Fees reference number?');
    }
    await performAction('clickButton', haveYouAlreadyAppliedForHelpWithFees.continueButton);
  }

  private async confirmOtherPartiesAgreed(confirmOtherParty: actionRecord) {
    await performAction('recordUserEntry', confirmOtherParty);
    await performAction('clickRadioButton', {
      question: confirmOtherParty.question,
      option: confirmOtherParty.option,
    });
    await performAction('clickButton', haveTheOtherPartiesAgreedToThisApplication.continueButton);
  }

  private async reasonsApplicationShouldNotBeShared(reason: actionRecord) {
    await performAction('recordUserEntry', reason);
    await performAction('clickRadioButton', {
      question: reason.question,
      option: reason.option,
    });
    if (reason.option === 'Yes') {
      const userInput =
        typeof reason.input === 'number' ? generateRandomString(reason.input) : (reason.input as string);
      await performAction('inputText', reason.label, userInput);
      FieldsStore.update(reason.label as string, userInput);
    } else {
      FieldsStore.delete(reason.label as string);
    }
    await performAction('clickButton', areThereAnyReasonsThatThisApplicationShouldNotBeShared.continueButton);
  }

  private async confirmOrderDoYouWant(confirmOrder: actionRecord) {
    await performAction('recordUserEntry', confirmOrder);
    const userInput =
      typeof confirmOrder.input === 'number'
        ? generateRandomString(confirmOrder.input)
        : (confirmOrder.input as string);
    await performAction('inputText', confirmOrder.label, userInput);
    FieldsStore.rename(confirmOrder.label as string, 'What order do you want the court to make and why?');
    FieldsStore.update('What order do you want the court to make and why?', userInput);
    await performAction('clickButton', whatOrderDoYouWantTheCourtToMakeAndWhy.continueButton);
  }

  private async selectLanguageUsedToComplete(selectLanguageData: actionRecord) {
    await performAction('recordUserEntry', selectLanguageData);
    await performAction('clickRadioButton', {
      question: selectLanguageData.question,
      option: selectLanguageData.option,
    });
    await performAction('clickButton', whichLanguageDidYouUseToCompleteThisService.continueButton);
  }

  private async selectStatementOfTruth(sot: actionRecord) {
    await performAction('check', {
      question: sot.question,
      option: sot.option,
    });
    await performAction('inputText', sot.label, sot.input);

    const key = isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion as string;

    const isKeyPresent = FieldsStore.has(key);
    const value = isKeyPresent ? FieldsStore.get(key) : undefined;

    const button =
      isKeyPresent && value === 'No'
        ? checkYourAnswersGenApps.submitHiddenButton
        : checkYourAnswersGenApps.continueToPaymentHiddenButton;
    await performAction('clickButton', button);
  }

  private async inputErrorValidationGenApp(validationArr: actionRecord) {
    const inputs = Array.isArray(validationArr.inputArray) ? validationArr.inputArray : [validationArr.inputArray];

    for (const item of inputs) {
      switch (validationArr.validationType) {
        case 'radioOptions':
          await performAction('clickButton', validationArr.button);
          await performValidation(
            'errorMessage',
            !validationArr?.header ? (validationArr.header = 'There is a problem') : validationArr.header,
            item.errMessage
          );
          await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option });
          break;

        case 'textField':
          await performAction('inputText', validationArr.label, generateRandomString(item.input));
          await performAction('clickButton', validationArr.button);
          await performValidation('errorMessage', validationArr.label, item.errMessage);
          break;

        case 'checkBox':
          await performAction('clickButton', validationArr.button);
          await performValidation(
            'errorMessage',
            !validationArr?.header ? (validationArr.header = 'There is a problem') : validationArr.header,
            item.errMessage
          );
          await performAction('check', { question: validationArr.question, option: validationArr.option });
          break;
      }
    }
  }

  private async retrieveCYATableData(page: Page) {
    const tables = page.locator(`//dl`);
    const tableCount = await tables.count();

    if (tableCount === 0) {
      throw new Error(`CYA table not found. Exiting...`);
    }

    for (let i = 0; i < tableCount; i++) {
      const curTable = tables.nth(i);

      if (!(await curTable.isVisible())) {
        throw new Error('table not found');
      }

      const rows = curTable.locator('.govuk-summary-list__row');
      const rowCount = await rows.count();
      if (rowCount === 0) {
        continue;
      }

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);

        if (!(await row.isVisible())) {
          continue;
        }

        const keyQns = row.locator('dt.govuk-summary-list__key');
        const valAns = row.locator('dd.govuk-summary-list__value');

        const keyText = (await keyQns.first().innerText()).trim();
        const valText = (await valAns.first().innerText()).trim().replace(/\r?\n+/g, ',');
        if (keyText && keyText.length > 0) {
          cyaMap.set(keyText ?? '', valText ?? '');
        }
      }
    }

    await test.step('Retrieved CYA values can be found in the console logs', async () => {
      console.log('\nThe Data Retrieved From Check Your Answers Page Are As Follows');
      const lines: string[] = [];
      for (const [key, value] of cyaMap.entries()) {
        const line = `• Key: "${key}" → Value: "${value}"`;
        console.log('============================================================');
        console.log(line);
        lines.push(line);
      }
    });
  }

  private async validateCYA() {
    const misMatchMap = compareMaps(cyaMap, FieldsStore.getAll(), {
      name1: 'CYA',
      name2: 'FieldStore',
    });

    await test.step('CYA Validation Started and the results are present in the console logs', async () => {
      if (misMatchMap.size > 0) {
        console.log(`\n❌ Differences found: ${misMatchMap.size}`);
        for (const [key, val] of misMatchMap) {
          const expectedValue = val.a === undefined ? '<missing>' : String(val.a);
          const actualValue = val.b === undefined ? '<missing>' : String(val.b);
          console.log('============================================================');
          console.log(`• key: "${String(key)}" → Expected: ${expectedValue} | Actual: ${actualValue}`);
        }
        console.log(`\n**********  END OF CYA FAILURE LIST. ***************`);
        throw new Error(`CYA validations failed for ${misMatchMap.size} ${misMatchMap.size === 1 ? 'item' : 'items'}`);
      } else {
        console.log('\n✅ CHECK YOUR ANSWERS VALIDATION PASSED!\n');
      }
    });
    cyaMap.clear();
  }

  private async reviewCYA(page: Page, startPage: actionData) {
    const row = page.locator('.govuk-summary-list__row').nth(0);
    const questionText = await row.locator('dt').innerText();

    const changeLink = row.getByRole('link', { name: 'Change' });

    const href = await changeLink.getAttribute('href');
    expect(href, `Missing href for question: ${questionText}`).toBeTruthy();

    await Promise.all([page.waitForURL(new RegExp(href!)), changeLink.click()]);

    const pagesForThisQuestion = journeys[String(startPage)] ?? defaultJourney;

    await this.followJourneyBackToCya(page, pagesForThisQuestion);

    await expect(page).toHaveURL(/check-your-answers/);
  }

  private async reviewAndUpdateCYA(page: Page, review: actionRecord) {
    const rows = page.locator('.govuk-summary-list__row');
    const rowCount = await rows.count();

    for (let i = 0; i < rowCount; i++) {
      const row = page.locator('.govuk-summary-list__row').nth(i);
      const questionText = await row.locator('dt').innerText();
      if (questionText === (review.changeOption as string)) {
        const changeLink = row.getByRole('link', { name: 'Change' });

        const href = await changeLink.getAttribute('href');
        expect(href, `Missing href for question: ${questionText}`).toBeTruthy();
        await Promise.all([page.waitForURL(new RegExp(href!)), changeLink.click()]);
        break;
      }
    }
    await this.updatePreviouslyAnsweredPage(page);
    const pagesForThisQuestion = journeys[String(review.journey)] ?? defaultJourney;

    await this.followJourneyBackToCya(page, pagesForThisQuestion);

    await expect(page).toHaveURL(/check-your-answers/);
  }

  private async updatePreviouslyAnsweredPage(page: Page) {
    const currentPage = stringToCamelCase(
      await page
        .locator(
          'legend h1.govuk-fieldset__heading, h1.govuk-heading-xl, h1.govuk-heading-l, h1.govuk-heading-m, legend.govuk-fieldset__legend--l'
        )
        .first()
        .innerText()
    );

    switch (currentPage) {
      case 'isTheCourtHearingInTheNext14Days': {
        const option1 =
          FieldsStore.get(isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion as string) === 'Yes'
            ? isTheCourtHearingInTheNext14Days.noRadioOption
            : isTheCourtHearingInTheNext14Days.yesRadioOption;
        await performAction('confirmIfCourtHearingInNext14Days', {
          question: isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion,
          option: option1,
        });
        if (option1 === 'Yes') {
          await performValidation('mainHeader', helpPayingTheFee.mainHeader);
          await performAction('doYouNeedHelpPayingFee', {
            question: helpPayingTheFee.doYouNeedHelpPayingTheFeeQuestion,
            option: helpPayingTheFee.iDoNotNeedHelpPayingTheFeeRadioOption,
          });
        } else {
          await performValidation('mainHeader', haveTheOtherPartiesAgreedToThisApplication.mainHeader);
          FieldsStore.deleteKeys([
            helpPayingTheFee.doYouNeedHelpPayingTheFeeQuestion,
            haveYouAlreadyAppliedForHelpWithFees.haveYouAlreadyAppliedForHelpQuestion,
            'What is your Help with Fees reference number?',
          ]);
        }

        break;
      }
      case 'doYouNeedHelpPayingTheFeeForThisApplication': {
        const feeOption1 =
          FieldsStore.get(helpPayingTheFee.doYouNeedHelpPayingTheFeeQuestion as string) ===
            'I need help paying the fee'
            ? helpPayingTheFee.iDoNotNeedHelpPayingTheFeeRadioOption
            : helpPayingTheFee.iNeedHelpPayingTheFeeRadioOption;
        await performAction('doYouNeedHelpPayingFee', {
          question: helpPayingTheFee.doYouNeedHelpPayingTheFeeQuestion,
          option: feeOption1,
        });
        if (feeOption1 !== 'I need help paying the fee') {
          FieldsStore.deleteKeys([
            haveYouAlreadyAppliedForHelpWithFees.haveYouAlreadyAppliedForHelpQuestion,
            'What is your Help with Fees reference number?',
          ]);
        } else {
          await performValidation('mainHeader', haveYouAlreadyAppliedForHelpWithFees.mainHeader);
          await performAction('confirmYouHaveAppliedForFeeHelp', {
            question: haveYouAlreadyAppliedForHelpWithFees.haveYouAlreadyAppliedForHelpQuestion,
            option: haveYouAlreadyAppliedForHelpWithFees.yesRadioOption,
            label: haveYouAlreadyAppliedForHelpWithFees.hwfReferenceHiddenTextLabel,
            input: haveYouAlreadyAppliedForHelpWithFees.hwfReferenceTextInput,
          });
        }
        await performValidation('mainHeader', haveTheOtherPartiesAgreedToThisApplication.mainHeader);
        break;
      }
      case 'whatOrderDoYouWantTheCourtToMakeAndWhy': {
        FieldsStore.delete('What order do you want the court to make and why?');
        await performAction('confirmOrderDoYouWant', {
          label: whatOrderDoYouWantTheCourtToMakeAndWhy.explainWhatYouWantTextLabel,
          input: whatOrderDoYouWantTheCourtToMakeAndWhy.whatYouWantTheCourtToDoTextInput,
        });
        await performValidation('mainHeader', doYouWantToUploadDocumentToSupportYourApplication.mainHeader);
        break;
      }
      case 'haveYouAlreadyAppliedForHelpWithYourApplicationFee': {
        FieldsStore.delete('What is your Help with Fees reference number?');
        await performAction('confirmYouHaveAppliedForFeeHelp', {
          question: haveYouAlreadyAppliedForHelpWithFees.haveYouAlreadyAppliedForHelpQuestion,
          option: haveYouAlreadyAppliedForHelpWithFees.yesRadioOption,
          label: haveYouAlreadyAppliedForHelpWithFees.hwfReferenceHiddenTextLabel,
          input: haveYouAlreadyAppliedForHelpWithFees.hwfReferenceTextInput,
        });
        await performValidation('mainHeader', haveTheOtherPartiesAgreedToThisApplication.mainHeader);
        break;
      }

      default:
        break;
    }
  }

  private async followJourneyBackToCya(page: Page, allowedPages: string[]) {
    const cyaUrlPart = '/check-your-answers';

    for (let i = 0; i < allowedPages.length; i++) {
      const currentUrl = page.url();

      if (currentUrl.includes(cyaUrlPart)) {
        return;
      }
      const expectedPage = allowedPages[i];
      const onAllowedPage = currentUrl.includes(expectedPage);

      expect(onAllowedPage, `Unexpected page. Expected: ${expectedPage}, Actual: ${currentUrl}`).toBeTruthy();

      const navButton = !currentUrl.includes('ask') ? 'Continue' : 'Start now';
      await performAction('clickButton', navButton);
    }

    throw new Error('Exceeded maximum steps before reaching CYA');
  }
}

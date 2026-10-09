import {Page} from '@playwright/test';
import {addressDetails} from '@data/page-data';
import {contactDetailsLR} from '@data/page-data-figma';
import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {performAction, performActions} from '@utils/controller';

export class RespondToAClaimAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectRespondToClaimContactPreferences', () => this.selectContactPreferences(fieldName as actionRecord)],
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
}

import {IValidation} from "@utils/interfaces/validation.interface";
import {TextValidation} from "@utils/validations/element-validations/text.validation";
import {BannerAlertValidation} from "@utils/validations/element-validations/bannerAlert.validation";
import {VisibilityValidation} from "@utils/validations/element-validations/visibility.validation";
import {FormLabelValueValidation} from "@utils/validations/element-validations/formLabelValue.validation";
import {OptionListValidation} from "@utils/validations/element-validations/optionList.validation";
import {MainHeaderValidation} from "@utils/validations/element-validations/pageHeader.validation";
import {ErrorMessageValidation} from "@utils/validations/element-validations/error-message.validation";
import {RadioButtonValidation} from "@utils/validations/element-validations/radioButton.validation";
import {CYAValidation} from "@utils/validations/element-validations/cya-validation.validation";
import {AddressCYAValidation} from "@utils/validations/element-validations/address-cya-validation.validation";

export class ValidationRegistry {
  private static validations: Map<string, IValidation> = new Map([
    ['text', new TextValidation()],
    ['bannerAlert', new BannerAlertValidation()],
    ['formLabelValue', new FormLabelValueValidation()],
    ['optionList', new OptionListValidation()],
    ['mainHeader', new MainHeaderValidation()],
    ['errorMessage', new ErrorMessageValidation()],
    ['radioButtonChecked', new RadioButtonValidation()],
    ['elementToBeVisible', new VisibilityValidation()],
    ['elementNotToBeVisible', new VisibilityValidation()],
    ['waitUntilElementDisappears', new VisibilityValidation()],
    ['validateCYA', new CYAValidation()],
    ['validateAddressCYA', new AddressCYAValidation()]
  ]);

  static getValidation(validationType: string): IValidation {
    const validation = this.validations.get(validationType);
    if (!validation) {
      throw new Error(`Validation '${validationType}' is not registered. Available validations: ${Array.from(this.validations.keys()).join(', ')}`);
    }
    return validation;
  }

  static getAvailableValidations(): string[] {
    return Array.from(this.validations.keys());
  }
}

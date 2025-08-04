import {IValidation} from "@utils/interfaces/validation.interface";
import {TextValidation} from "@utils/validations/element-validations/text.validation";
import {ValueValidation} from "@utils/validations/element-validations/value.validation";
import {EnabledValidation} from "@utils/validations/element-validations/enabled.validation";
import {CssValidation} from "@utils/validations/element-validations/css.validation";
import {CheckedValidation} from "@utils/validations/element-validations/checked.validation";
import {BannerAlertValidation} from "@utils/validations/element-validations/bannerAlert.validation";
import {AttributeValidation} from "@utils/validations/element-validations/attribute.validation";
import {CountValidation} from "@utils/validations/element-validations/count.validation";
import {VisibilityValidation} from "@utils/validations/element-validations/visibility.validation";
import {FormLabelValueValidation} from "@utils/validations/element-validations/formLabelValue.validation";
import {OptionListValidation} from "@utils/validations/element-validations/optionList.validation";
import {MainHeaderValidation} from "@utils/validations/element-validations/pageHeader.validation";
import {ErrorMessageValidation} from "@utils/validations/element-validations/error-message.validation";

export class ValidationRegistry {
  private static validations: Map<string, IValidation> = new Map([
    ['text', new TextValidation()],
    ['value', new ValueValidation()],
    ['visibility', new VisibilityValidation()],
    ['enabled', new EnabledValidation()],
    ['checked', new CheckedValidation()],
    ['count', new CountValidation()],
    ['attribute', new AttributeValidation()],
    ['css', new CssValidation()],
    ['bannerAlert', new BannerAlertValidation()],
    ['formLabelValue', new FormLabelValueValidation()],
    ['optionList', new OptionListValidation()],
    ['mainHeader', new MainHeaderValidation()],
    ['errorMessage', new ErrorMessageValidation()]
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

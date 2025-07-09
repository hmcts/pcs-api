// validation.registry.ts
import { IValidation } from '../interfaces/validation.interface';
import { TextValidation } from '../validations/element-validations/text.validation';
import { ValueValidation } from '../validations/element-validations/value.validation';
import { VisibilityValidation } from '../validations/element-validations/visibility.validation';
import { EnabledValidation } from '../validations/element-validations/enabled.validation';
import { CheckedValidation } from '../validations/element-validations/checked.validation';
import { CountValidation } from '../validations/element-validations/count.validation';
import { AttributeValidation } from '../validations/element-validations/attribute.validation';
import { ContainsTextValidation } from '../validations/element-validations/contains-text.validation';
import { CssValidation } from '../validations/element-validations/css.validation';
import {BannerAlertValidation} from "@utils/validations/element-validations/bannerAlert.validation";

export class ValidationRegistry {
  private static validations: Map<string, IValidation> = new Map([
    ['text', new TextValidation()],
    ['value', new ValueValidation()],
    ['visibility', new VisibilityValidation()],
    ['enabled', new EnabledValidation()],
    ['checked', new CheckedValidation()],
    ['count', new CountValidation()],
    ['attribute', new AttributeValidation()],
    ['containsText', new ContainsTextValidation()],
    ['css', new CssValidation()],
    ['bannerAlert', new BannerAlertValidation()]
  ]);

  static getValidation(validationType: string): IValidation {
    const validation = this.validations.get(validationType);
    if (!validation) {
      throw new Error(`Validation '${validationType}' is not registered. Available validations: ${Array.from(this.validations.keys()).join(', ')}`);
    }
    return validation;
  }

  static registerValidation(validationType: string, validation: IValidation): void {
    this.validations.set(validationType, validation);
  }

  static getAvailableValidations(): string[] {
    return Array.from(this.validations.keys());
  }
}

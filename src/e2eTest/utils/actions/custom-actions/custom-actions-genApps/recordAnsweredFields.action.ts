import { Page } from 'playwright-core';

import { IAction, actionData, actionRecord } from '../../../interfaces';

const fieldsMap = new Map<string, string>();

export const FieldsStore = {
  set(key: string, value: string): void {
    fieldsMap.set(key, value);
  },

  has(key: string): boolean {
    return fieldsMap.has(key);
  },

  update(key: string, value: string): boolean {
    if (!fieldsMap.has(key)) {
      return false;
    }
    fieldsMap.set(key, value);
    return true;
  },

  get(key: string): string | undefined {
    return fieldsMap.get(key);
  },

  getAll(): ReadonlyMap<string, string> {
    return fieldsMap;
  },

  clear(): void {
    fieldsMap.clear();
  },

  delete(key: string): boolean {
    return fieldsMap.delete(key);
  },

  deleteKeys(keys: string[]): boolean {
    let deleted = false;

    for (const key of keys) {
      if (fieldsMap.delete(key)) {
        deleted = true;
      }
    }

    return deleted;
  },

  rename(oldKey: string, newKey: string): boolean {
    if (!fieldsMap.has(oldKey) || fieldsMap.has(newKey)) {
      console.log(`Keys ${oldKey} nor ${newKey} is present`);
      return false;
    }

    const value = fieldsMap.get(oldKey)!;
    fieldsMap.set(newKey, value);
    fieldsMap.delete(oldKey);
    return true;
  },
};

export class RecordAnswers implements IAction {
  async execute(page: Page, action: string, fieldName?: actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['recordUserEntry', () => this.recordUserEntry(fieldName as actionRecord)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async recordUserEntry(fields: actionRecord) {
    const recordIfPresent = (keyName: string, valueKey: string) => {
      const key = fields[keyName];
      const value = fields[valueKey];

      if (typeof key !== 'string' || value === undefined) {
        return;
      }

      FieldsStore.set(key, this.normalizeValueData(value));
    };

    recordIfPresent('question', 'option');
    recordIfPresent('label', 'input');

    for (let i = 1; ; i++) {
      const labelKey = `label${i}` as keyof typeof fields;
      const inputKey = `input${i}` as keyof typeof fields;
      const questionKey = `question${i}` as keyof typeof fields;
      const optionKey = `option${i}` as keyof typeof fields;

      const noLabel = !(labelKey in fields);
      const noInput = !(inputKey in fields);
      const noQuestion = !(questionKey in fields);
      const noOption = !(optionKey in fields);

      if (noLabel && noInput && noQuestion && noOption) {
        break;
      }

      if (!noLabel && !noInput) {
        recordIfPresent(labelKey as string, inputKey as string);
      }

      if (!noQuestion && !noOption) {
        recordIfPresent(questionKey as string, optionKey as string);
      }


    }
  }

  private normalizeValueData(value: actionData): string {
    if (Array.isArray(value)) {
      return value.map(val => String(val)).join(',');
    }

    if (typeof value === 'object' && value !== null) {
      return JSON.stringify(value);
    }

    return String(value);
  }
}

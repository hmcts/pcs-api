import fs from 'fs';
import path from 'path';

import { ActionRegistry } from './utils/registry/action.registry';
import { ValidationRegistry } from './utils/registry/validation.registry';

type RegistryType = {
  getItems: () => string[];
  sectionHeader: string;
  examplePrefix: string;
  columnHeader: string;
};

async function initializeRegistries() {
  return { ActionRegistry, ValidationRegistry };
}

const updateRegistrySection = (content: string, registry: RegistryType): string => {
  const { getItems, sectionHeader, examplePrefix, columnHeader } = registry;
  const startIdx = content.indexOf(sectionHeader);

  if (startIdx === -1) {
    return content;
  }

  const endIdx = content.indexOf('###', startIdx + 1);
  const sectionEnd = endIdx === -1 ? content.length : endIdx;
  const beforeSection = content.slice(0, startIdx);
  const afterSection = content.slice(sectionEnd);
  const sectionContent = content.slice(startIdx, sectionEnd);

  const registeredItems = new Set(getItems());

  const lines = sectionContent.split('\n');
  const newLines: string[] = [sectionHeader];

  const tableLines = [`| ${columnHeader}          | Example Usage |`, '|------------------|---------------|'];

  for (const line of lines) {
    const itemMatch = line.match(/^\| (.*?)\s*\|(.*)\|/);
    if (itemMatch && !line.includes('---')) {
      const itemName = itemMatch[1].trim();
      if (registeredItems.has(itemName)) {
        tableLines.push(line);
        registeredItems.delete(itemName);
      }
    }
  }

  registeredItems.forEach(item => {
    tableLines.push(`| ${item.padEnd(15)} | \`${examplePrefix}('${item}', ...)\` |`);
  });

  newLines.push(...tableLines, '');

  return beforeSection + newLines.join('\n') + afterSection;
};

export const updateTestReadme = async (): Promise<void> => {
  await initializeRegistries();
  const readmePath = path.join(__dirname, './test-README.md');

  try {
    let content = fs.readFileSync(readmePath, 'utf8');

    content = updateRegistrySection(content, {
      getItems: () => ActionRegistry.getAvailableActions(),
      sectionHeader: '### Actions',
      examplePrefix: 'performAction',
      columnHeader: 'Action',
    });

    content = updateRegistrySection(content, {
      getItems: () => ValidationRegistry.getAvailableValidations(),
      sectionHeader: '### Validations',
      examplePrefix: 'performValidation',
      columnHeader: 'Validation',
    });

    fs.writeFileSync(readmePath, content);
  } catch (error) {
    throw new Error('Error updating README:' + error);
  }
};

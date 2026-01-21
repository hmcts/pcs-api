import axios, { AxiosError } from 'axios';
import fs from 'fs';
import path from 'path';

const FIGMA_API_TOKEN = process.env.FIGMA_API_TOKEN;
const FIGMA_FILE_KEY = process.env.FIGMA_FILE_KEY;

// --- CUSTOMIZATION ---
const FIGMA_FOOTER_NAMES_TO_EXCLUDE = ['ExUI Footer', 'Footer Component', 'Global Footer', 'footer'];
const FIGMA_HEADER_NAMES_TO_EXCLUDE = ['ExUI Header', 'Header Component', 'Main Nav', 'Global Header'];
const H1_LAYER_NAMES = ['h1', 'main header', 'page title', 'title']; // Case-insensitive check
const EXCLUDED_TEXTS = [
  'Alpha',
  'This is a new service – your feedback will help us to improve it.',
];

type PotentialH1 = {
  text: string;
  fontSize: number;
  isNameMatch: boolean;
};

const FIGMA_API_BASE_URL = 'https://api.figma.com/v1';

/**
 * Checks if a Figma node should be excluded based on its name or visibility.
 * @param node The Figma node to check.
 * @returns True if the node should be excluded, false otherwise.
 */
function isExcluded(node: any): boolean {
  if (!node || node.visible === false) return true;
  const nodeNameLower = (node.name || '').toLowerCase();
  const allExclusions = [...FIGMA_HEADER_NAMES_TO_EXCLUDE, ...FIGMA_FOOTER_NAMES_TO_EXCLUDE];
  return allExclusions.some(name => nodeNameLower.includes(name.toLowerCase()));
}

/**
 * Sanitizes a string to be used as a JavaScript variable or filename.
 * @param name The string to sanitize.
 * @returns A camelCase string.
 */
function sanitizeForCode(name: string): string {
  if (!name) return 'unnamed';
  let sanitized = name
    .replace(/[^a-zA-Z0-9\s]/g, "") // Remove special characters
    .trim()
    .replace(/\s+/g, " ")
    .split(' ')
    .slice(0, 5) // Use max 5 words for a key
    .map((word, index) => {
      if (index === 0) return word.toLowerCase();
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
    })
    .join('');

  if (!sanitized) return 'unnamed';
  // Ensure it's a valid JS identifier
  return sanitized.match(/^[a-zA-Z_$][a-zA-Z0-9_$]*$/) ? sanitized : `key${sanitized}`;
}

/**
 * Generates a descriptive key for a text element based on its content and its layer name in Figma.
 * @param text The visible text content.
 * @param nodeName The name of the Figma layer containing the text.
 * @returns A descriptive, camelCased key string.
 */
function generateKey(text: string, nodeName: string): string {
  const textLower = text.toLowerCase();
  const nameLower = (nodeName || '').toLowerCase();

  // Rule 1: Handle specific options like "Yes" and "No" as radio options.
  if (textLower === 'yes') return 'yesRadioOption';
  if (textLower === 'no') return 'noRadioOption';

  // Rule 1.5: Handle specific button texts that should always be buttons.
  if (textLower === 'save and continue') return 'saveAndContinueButton';
  if (textLower === 'save for later') return 'saveForLaterButton';
  if (textLower === 'continue') return 'continueButton';

  // Rule 1.6: Handle specific link texts.
  if (textLower === 'cancel') {
    // If it's explicitly a button, let it fall through to button detection
    if (nameLower.includes('button') || nameLower.includes('btn')) {
      // Let it continue to button detection below
    } else {
      // Default to cancelLink (most Cancel actions are links, not labels)
      return 'cancelLink';
    }
  }
  if (textLower === 'sign out' || textLower === 'signout') {
    // Always treat sign out as a link
    return 'signOutLink';
  }

  // Rule 2: Determine the suffix based on the Figma layer name, using a priority order.
  let suffix = 'Label'; // Default suffix.

  // High priority/specific components
  if (nameLower.includes('iconbutton') || nameLower.includes('icon btn')) {
    suffix = 'IconButton';
  } else if (nameLower.includes('button') || nameLower.includes('btn')) {
    suffix = 'Button';
  } else if (nameLower.includes('textlink')) {
    suffix = 'TextLink';
  } else if (nameLower.includes('link')) {
    suffix = 'Link';
  } else if (nameLower.includes('breadcrumb')) {
    suffix = 'Breadcrumb';
  } else if (nameLower.includes('pagination')) {
    suffix = 'Pagination';
  } else if (nameLower.includes('tab')) {
    suffix = 'Tab';
  }
  // Headings
  else if (nameLower.includes('sectiontitle') || nameLower.includes('section header')) {
    suffix = 'SectionTitle';
  } else if (nameLower.includes('h2') || nameLower.includes('sub heading')) {
    suffix = 'H2';
  } else if (nameLower.includes('h3')) {
    suffix = 'H3';
  }
  // Form elements
  else if (nameLower.includes('textarea')) {
    suffix = 'TextArea';
  } else if (nameLower.includes('textfield')) {
    suffix = 'TextField';
  } else if (nameLower.includes('inputfield') || nameLower.includes('input box')) {
    suffix = 'InputField';
  } else if (nameLower.includes('dropdown') || nameLower.includes('select')) {
    suffix = 'Dropdown';
  } else if (nameLower.includes('slider')) {
    suffix = 'Slider';
  } else if (nameLower.includes('toggle') || nameLower.includes('switch')) {
    suffix = 'Toggle';
  } else if (nameLower.includes('radiobutton') || nameLower.includes('radio')) {
    suffix = 'RadioButton';
  } else if (nameLower.includes('checkbox')) {
    suffix = 'Checkbox';
  } else if (nameLower.includes('option')) { // General option fallback
    suffix = 'Option';
  } else if (nameLower.includes('form')) {
    suffix = 'Form';
  }
  // Informational elements
  else if (nameLower.includes('tooltip')) {
    suffix = 'Tooltip';
  } else if (nameLower.includes('alert') || nameLower.includes('notification')) {
    suffix = 'Alert';
  } else if (nameLower.includes('badge')) {
    suffix = 'Badge';
  } else if (nameLower.includes('caption')) {
    suffix = 'Caption';
  } else if (nameLower.includes('hint')) {
    suffix = 'HintText';
  } else if (nameLower.includes('para') || nameLower.includes('paragraph')) {
    suffix = 'Paragraph';
  }
  // Media / UI containers
  else if (nameLower.includes('avatar')) {
    suffix = 'Avatar';
  } else if (nameLower.includes('icon')) {
    suffix = 'Icon';
  } else if (nameLower.includes('image')) {
    suffix = 'Image';
  } else if (nameLower.includes('loader') || nameLower.includes('spinner')) {
    suffix = 'Loader';
  } else if (nameLower.includes('modal')) {
    suffix = 'Modal';
  } else if (nameLower.includes('table')) {
    suffix = 'Table';
  } else if (nameLower.includes('card')) {
    suffix = 'Card';
  } else if (nameLower.includes('menu')) {
    suffix = 'Menu';
  }


  // Rule 3: Combine the sanitized text and the determined suffix.
  const baseKey = sanitizeForCode(text);
  return baseKey + suffix;
}


/**
 * Finds the main page title (H1) by prioritizing layers named like 'h1'
 * and falling back to the largest font size on the page.
 * @param pageNode The Figma page or frame node to search within.
 * @returns The text content of the identified H1, or null.
 */
function findPageTitle(pageNode: any): string | null {
  function search(node: any): PotentialH1 | null {
    if (!node || node.visible === false || isExcluded(node)) {
      return null;
    }

    let bestCandidate: PotentialH1 | null = null;

    // Check the current node itself for H1 potential
    if (node.type === 'TEXT' && node.characters?.trim()) {
      const text = node.characters.trim();
      const nameLower = (node.name || '').toLowerCase();
      const isNameMatch = H1_LAYER_NAMES.some(h1Name => nameLower.includes(h1Name));
      const fontSize = node.style?.fontSize || 0;
      bestCandidate = { text, fontSize, isNameMatch };
    }

    // Recursively search children and compare with the current best candidate
    if (node.children) {
      for (const child of node.children) {
        const childCandidate = search(child);

        if (childCandidate) {
          if (!bestCandidate) {
            bestCandidate = childCandidate;
          } else {
            // A name match always wins over a non-name match.
            if (childCandidate.isNameMatch && !bestCandidate.isNameMatch) {
              bestCandidate = childCandidate;
            }
            // If both are name matches or both are not, the largest font size wins.
            else if (childCandidate.isNameMatch === bestCandidate.isNameMatch && childCandidate.fontSize > bestCandidate.fontSize) {
              bestCandidate = childCandidate;
            }
          }
        }
      }
    }

    return bestCandidate;
  }

  const result = search(pageNode);
  return result?.text || null;
}

/**
 * Recursively traverses the node tree to extract all visible text into a flat key-value object.
 * This function processes a SINGLE frame and handles collisions within it.
 * @param node The current Figma node.
 * @param pageTitle The page title, to avoid including it in the content body.
 * @returns A flat object of text content for a single frame, like { key: 'value', key2: 'value' }.
 */
function extractFlatText(node: any, pageTitle: string | null): Record<string, string> {
  if (isExcluded(node) || (node.type === 'TEXT' && node.characters?.trim() === pageTitle)) {
    return {};
  }

  let results: Record<string, string> = {};

  if (node.type === 'TEXT' && node.characters?.trim()) {
    const text = node.characters.trim();
    
    // Exclude specific texts that should not be included
    if (EXCLUDED_TEXTS.some(excludedText => text === excludedText || text.includes(excludedText))) {
      return {};
    }
    
    const key = generateKey(text, node.name);
    results[key] = text;
    return results;
  }

  if (node.children) {
    for (const child of node.children) {
      const childResults = extractFlatText(child, pageTitle);

      // This loop correctly handles collisions for items within a single frame (e.g., 3 options).
      for (let [key, value] of Object.entries(childResults)) {
        let uniqueKey = key;
        // If a key is already present, it means we have multiple elements with the same text/name combo.
        // We must create a unique key to preserve all of them.
        if (results.hasOwnProperty(uniqueKey)) {
          let i = 2;
          uniqueKey = `${key}${i}`;
          while (results.hasOwnProperty(uniqueKey)) {
            i++;
            uniqueKey = `${key}${i}`;
          }
        }
        results[uniqueKey] = value;
      }
    }
  }

  return results;
}


/**
 * Writes the final merged and flattened text content to a TypeScript file.
 * @param representativeNode The first Figma node for a page group, used for metadata.
 * @param outputDirPath The absolute path to the output directory.
 * @param filename The final filename to use.
 * @param pageTitle The determined page title for the group.
 * @param mergedData The final, flat content object for the group.
 */
function writeMergedFile(representativeNode: any, outputDirPath: string, filename: string, pageTitle: string | null, mergedData: Record<string, string>) {
  const variableName = path.basename(filename, '.ts');
  const outputFilePath = path.join(outputDirPath, filename);

  console.log(`  - Generating merged file: ${filename}`);

  const lines: string[] = [];
  if (pageTitle) {
    lines.push(`  mainHeader: \`${pageTitle.replace(/`/g, '\\`')}\``);
  }

  // Sort keys for a consistent, readable output file
  const sortedKeys = Object.keys(mergedData).sort();
  for (const key of sortedKeys) {
    const value = mergedData[key];
    lines.push(`  ${key}: \`${value.replace(/`/g, '\\`')}\``);
  }

  const contentString = lines.join(',\n');

  const fileContent = `// Auto-generated from Figma file: ${FIGMA_FILE_KEY}, based on page: "${representativeNode.name}" (${representativeNode.id}) and other states\n\n` +
    `export const ${variableName} = {\n` +
    contentString +
    `\n};\n`;

  fs.writeFileSync(outputFilePath, fileContent, 'utf-8');
}


/**
 * Recursively finds all frames that look like a page (i.e., have a title).
 * @param node The node to start searching from.
 * @returns An array of Figma nodes that represent pages.
 */
function collectPageFrames(node: any): any[] {
  let pageFrames: any[] = [];

  // Heuristic: If a frame has a clear H1, it's a page.
  if (node.type === 'FRAME' && findPageTitle(node)) {
    // Don't look for nested pages within this one. Treat it as a single unit.
    return [node];
  }

  // If it's a container (including a frame without a title), look inside its children.
  if (node.children) {
    for (const child of node.children) {
      // Concat arrays from deeper recursive calls.
      pageFrames = pageFrames.concat(collectPageFrames(child));
    }
  }

  return pageFrames;
}


/**
 * Handles errors from the Figma API, providing user-friendly messages.
 * @param error The error object.
 * @param context A string describing what the script was doing when the error occurred.
 */
function handleApiError(error: unknown, context: string) {
  console.error(`\n❌ An error occurred while ${context}.`);
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError;
    if (axiosError.response) {
      console.error(`  - Status: ${axiosError.response.status}`);
      console.error(`  - Message: ${JSON.stringify(axiosError.response.data)}`);
      if (axiosError.response.status === 403) {
        console.error("  - Authentication error. Please check your FIGMA_API_TOKEN.");
      } else if (axiosError.response.status === 404) {
        console.error("  - Not found. Please check your FIGMA_FILE_KEY.");
      }
    } else {
      console.error("  - The request was made, but no response was received.");
    }
  } else {
    console.error("  - An unexpected error occurred:", error);
  }
  process.exit(1);
}

/**
 * Main execution function.
 */
async function main() {
  const outputDirectory = process.argv[2];
  const nodeArg = process.argv[3];
  let figmaNodeId: string | null = null;


  if (!outputDirectory) {
    console.error('❌ Error: Please provide an output directory path.');
    console.log('\nUsage (all pages):');
    console.log('  npx ts-node scripts/pull-fig-text.ts <output-directory>');
    console.log('\nUsage (single page/frame):');
    console.log('  npx ts-node scripts/pull-fig-text.ts <output-directory> nodeid:<node-id>');
    process.exit(1);
  }

  if (nodeArg && nodeArg.startsWith('nodeid:')) {
    figmaNodeId = nodeArg.substring('nodeid:'.length);
  }


  const outputDirPath = path.resolve(outputDirectory);
  fs.mkdirSync(outputDirPath, { recursive: true });
  const headers = { 'X-Figma-Token': FIGMA_API_TOKEN };

  try {
    if (figmaNodeId) {
      // --- Fetch a single node ---
      const encodedNodeId = encodeURIComponent(figmaNodeId);
      const apiUrl = `${FIGMA_API_BASE_URL}/files/${FIGMA_FILE_KEY}/nodes?ids=${encodedNodeId}`;
      console.log(`\nFetching single node: ${figmaNodeId}...`);
      const response = await axios.get(apiUrl, { headers });
      const nodeData = response.data.nodes[figmaNodeId];

      if (!nodeData?.document) {
        throw new Error(`Could not find a valid node with ID "${figmaNodeId}".`);
      }
      const targetNode = nodeData.document;
      const pageTitle = findPageTitle(targetNode);
      const baseName = sanitizeForCode(pageTitle || targetNode.name);
      const content = extractFlatText(targetNode, pageTitle);
      console.log(`Processing node: "${targetNode.name}"`);
      writeMergedFile(targetNode, outputDirPath, `${baseName}.ts`, pageTitle, content);

    } else {
      // --- Fetch the entire file ---
      const apiUrl = `${FIGMA_API_BASE_URL}/files/${FIGMA_FILE_KEY}`;
      console.log(`\nFetching Figma file: ${FIGMA_FILE_KEY}...`);
      const response = await axios.get(apiUrl, { headers });
      const fileData = response.data;

      if (!fileData?.document?.children) {
        throw new Error("Invalid Figma file structure received.");
      }
      console.log(`Processing file: "${fileData.name}"`);

      const pageGroups = new Map<string, any[]>();

      for (const canvasPage of fileData.document.children) {
        if (canvasPage.type === 'CANVAS') {
          console.log(`\n--- Scanning Canvas: "${canvasPage.name}" ---`);
          const pageFrames = collectPageFrames(canvasPage);

          for(const frame of pageFrames) {
            const pageTitle = findPageTitle(frame);
            const effectiveTitle = pageTitle || frame.name;
            if(effectiveTitle) {
              const baseName = sanitizeForCode(effectiveTitle);
              if(!pageGroups.has(baseName)) {
                pageGroups.set(baseName, []);
              }
              pageGroups.get(baseName)!.push(frame);
            }
          }
        }
      }

      console.log(`\n--- Merging content and generating files ---`);
      for(const [baseName, frames] of pageGroups.entries()) {
        let mergedData: Record<string, string> = {};
        let finalPageTitle: string | null = null;
        // De-duplication is scoped per page group.
        const seenValues = new Set<string>();

        console.log(`  - Merging ${frames.length} frame(s) for page: "${baseName}"`);

        for(const frame of frames) {
          const pageTitle = findPageTitle(frame);
          if(!finalPageTitle) finalPageTitle = pageTitle;

          const flatData = extractFlatText(frame, pageTitle);

          for (const [key, value] of Object.entries(flatData)) {
            // De-duplicate based on the text content to avoid the same string appearing multiple times.
            if (!seenValues.has(value)) {
              // Ensure the key is unique to prevent accidental overwrites if different text sanitizes to the same key.
              let uniqueKey = key;
              if (mergedData.hasOwnProperty(uniqueKey)) {
                let i = 2;
                uniqueKey = `${key}${i++}`;
                while(mergedData.hasOwnProperty(uniqueKey)) {
                  uniqueKey = `${key}${i++}`;
                }
              }
              mergedData[uniqueKey] = value;
              seenValues.add(value); // Mark this text value as processed for this page group.
            }
          }
        }

        if (Object.keys(mergedData).length > 0) {
          const representativeFrame = frames[0];
          const filename = `${baseName}.ts`;
          writeMergedFile(representativeFrame, outputDirPath, filename, finalPageTitle, mergedData);
        } else {
          console.log(`  - No content found for "${baseName}", skipping file generation.`);
        }
      }
    }
    console.log(`\n✅ Figma processing finished. Files saved in: ${outputDirPath}`);
  } catch (error) {
    handleApiError(error, `communicating with the Figma API`);
  }
}

main();


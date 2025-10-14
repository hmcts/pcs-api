import fs from 'fs';
import fetch from 'node-fetch';
import dotenv from 'dotenv';


// Load environment variables from a .env file
dotenv.config();

const FIGMA_API_TOKEN = process.env.FIGMA_API_TOKEN;
// Replace with the file key from your Figma file's URL
const FIGMA_FILE_KEY = 'YOUR_FIGMA_FILE_KEY';
const FIGMA_API_BASE = 'https://api.figma.com/v1';

// The exact name of the frame in Figma you want to extract data from.
// Make sure your Figma layers are named logically!
const FRAME_NAME_TO_EXTRACT = 'Claim Form Step 1';

// --- Helper Function to Recursively Find Nodes ---
/**
 * Recursively searches the Figma tree for nodes of a specific type.
 * @param {object} node - The current node in the Figma tree.
 * @param {string} type - The node type to search for (e.g., 'TEXT', 'RECTANGLE').
 * @returns {Array} - An array of nodes that match the type.
 */

function findNodesByType(node, type) {
  let results = [];
  if (node.type === type) {
    results.push(node);
  }
  if (node.children) {
    for (const child of node.children) {
      results = results.concat(findNodesByType(child, type));
    }
  }
  return results;
}

// --- Main Function to Fetch and Process Data ---
async function fetchAndProcessFigmaData() {
  if (!FIGMA_API_TOKEN) {
    console.error('Error: FIGMA_API_TOKEN is not defined. Please create a .env file.');
    return;
  }
  if (FIGMA_FILE_KEY === 'YOUR_FIGMA_FILE_KEY') {
    console.error('Error: Please replace YOUR_FIGMA_FILE_KEY in the script with your actual Figma file key.');
    return;
  }

  console.log('Fetching data from Figma API...');
  try {
    const response = await fetch(`${FIGMA_API_BASE}/files/${FIGMA_FILE_KEY}`, {
      headers: {
        'X-Figma-Token': FIGMA_API_TOKEN,
      },
    });

    if (!response.ok) {
      throw new Error(`Figma API request failed with status: ${response.status}`);
    }

    const figmaJson = await response.json();
    console.log('Successfully fetched data. Processing now...');

    // Find the specific frame we want to process
    const canvas = figmaJson.document.children.find(c => c.type === 'CANVAS');
    const targetFrame = canvas.children.find(frame => frame.name === FRAME_NAME_TO_EXTRACT && frame.type === 'FRAME');

    if (!targetFrame) {
      console.error(`Error: Could not find a frame named "${FRAME_NAME_TO_EXTRACT}".`);
      return;
    }

    // Extract all text nodes from within that frame
    const textNodes = findNodesByType(targetFrame, 'TEXT');

    // Format the extracted data into a structured object based on layer names
    // **IMPORTANT**: This relies on your Figma layers being named logically.
    // For example, a text layer for the title should be named 'title' or 'page-title'.
    const formattedContent = {};
    for (const node of textNodes) {
      // A simple convention: use the layer name as the key.
      // You might want to camelCase or snake_case it.
      const key = node.name.replace(/\s+/g, '-').toLowerCase();
      const value = node.characters; // The actual text content

      // This is a basic example. You can build a more complex structure.
      // For instance, you could group fields by looking for a parent frame named 'form-group'.
      formattedContent[key] = value;
    }

    // Write the formatted JSON to a file
    const outputPath = './tests/figma-content.json';
    fs.writeFileSync(outputPath, JSON.stringify(formattedContent, null, 2));

    console.log(`✅ Successfully created formatted JSON at: ${outputPath}`);
    console.log('You can now import this file into your Playwright tests.');

  } catch (error) {
    console.error('An error occurred:', error.message);
  }
}

/**
 * Escapes special regex characters in a string so it can be used safely in RegExp.
 */
export function escapeForRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Returns a RegExp that matches the exact text with optional leading/trailing whitespace.
 */
export function exactTextWithOptionalWhitespaceRegex(text: string): RegExp {
  return new RegExp('^\\s*' + escapeForRegex(text) + '\\s*$');
}

export function generateRandomString(length: string | number): string {
  if (typeof length !== 'number' || !Number.isInteger(length) || length <= 0) {
    return '';
  }
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

export function stringToCamelCase(input: string): string {
  return input
    .toLowerCase()
    .replace(/[^a-z0-9\s]/gi, '')
    .split(/\s+/)
    .map((word, index) => (index === 0 ? word : word.charAt(0).toUpperCase() + word.slice(1)))
    .join('');

}
/**
 * Returns a formatted British standard time
 */
export function getCurrentBSTTime(): string {
  const now = new Date();
  const formatted = now.toLocaleString("en-GB", {
    timeZone: "Europe/London",
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    second: "2-digit",
    hour12: true,
  });

  return formatted.replace(/am|pm/, (match) => match.toUpperCase());
}

/* convert YYY-MM-DD to DD/MM/YYYY format */
export function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString("en-GB"); 
}

/* convert string for ex RENT_ARREARS to Rent arrears */
export function formatText(input: string): string {
  return input
    .toLowerCase() 
    .replace(/_/g, " ")
    .replace(/^\w/, c => c.toUpperCase());
}

/* covert string 100000 to £1000 */
export function formatCurrency(value: string): string {
  const numberValue = Number(value) / 100;
  return `£${numberValue}`;
}

/* convert EXAMPLE to Example */
export function formatWord(input: string): string {
  return input.charAt(0).toUpperCase() + input.slice(1).toLowerCase();
}


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

/* convert YYY-MM-DD to DD/MM/YYYY format or DD MONTH YYYY */
export function formatDate(dateStr: string, formatType: string): string {

  const date = new Date(dateStr);
  let finalDate: string = '';
  if (formatType === 'DD/MM/YYYY') {
    finalDate = date.toLocaleDateString("en-GB");
  } else if (formatType === 'DD/MONTH/YYYY') {

    const day = date.getDate();
    const month = date.toLocaleString("en-GB", { month: "long" });
    const year = date.getFullYear();

    finalDate = `${day} ${month} ${year}`;
  }
  return finalDate;

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

/* convert date and time 2025-12-11T14:20:59 to 11/12/2025, 2:20:59PM */
export function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr);

  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();

  let hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  const ampm = hours >= 12 ? 'PM' : 'AM';

  hours = hours % 12;
  hours = hours === 0 ? 12 : hours; // convert 0 → 12

  return `${day}/${month}/${year}, ${hours}:${minutes}:${seconds}${ampm}`;
}

/* covert 2026-06-03T16:31:23.063194 to 3 June 2026, 5:31:23PM */
export function formatDateTimeBST(dataTime: string): string {
  const date = new Date(`${dataTime}Z`);

  return new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Europe/London',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    second: '2-digit',
    hour12: true
  })
    .format(date)
    .replace(' at ', ', ')
    .replace(' am', 'AM')
    .replace(' pm', 'PM');
}

/* Formats a numeric case number by inserting a hyphen after every 4 digits.
Example: "1781518470935861" -> "1781-5184-7093-5861"
*/
export function formatTheCaseNumber(caseNumber: string): string {
  return caseNumber.replace(/(\d{4})(?=\d)/g, '$1-');
}

/* convert string for ex RENT_ARREARS to Rent Arrears */
export function formatCaseStateText(input: string): string {
  return input
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, c => c.toUpperCase());
}

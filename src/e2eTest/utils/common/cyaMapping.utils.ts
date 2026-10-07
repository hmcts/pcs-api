export const actionMapQuestions: Record<string, string> = {
  "upload a document to the system": "Upload general application",
  "Uploaded file": "Document"
};

export const skipNormalization = new Set([
  'File name',
  'Add an issue date to the file name',
  'Email address (Optional)',
  'Email address',
  '.pdf',
  'Do you know the defendant’s name?',
  'Defendant’s first name',
  'Defendant’s last name',
  'Do you want to use this email address for notifications?',
  'Enter email address'

]);

export const ignoreAnswerInQuestions = [
  'document',
  'type of document',
  'defendant’s first name',
  'defendant’s last name',
  'do you know the defendant’s name? '
];
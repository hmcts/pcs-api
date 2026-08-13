export const actionMapQuestions: Record<string, string> = {
  "upload a document to the system": "Upload general application",
  "Email address (Optional)": "Email address",
  "Phone number (Optional)": "Phone number"
};

export const skipNormalization = new Set([
  'File name',
  'Add an issue date to the file name',
  //'Email address'
]);
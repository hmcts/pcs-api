import * as fs from 'fs';
import * as path from 'path';

const testCasesPath = path.join(__dirname, '../../../e2e-output/data/test-cases');

const files = fs.readdirSync(testCasesPath).filter(f => f.endsWith('.json'));
let removedCount = 0;

for (const file of files) {
  const filePath = path.join(testCasesPath, file);
  try {
    const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    let modified = false;

    function cleanObject(obj: any): any {
      if (!obj || typeof obj !== 'object') return obj;

      if (Array.isArray(obj)) {
        return obj.map(cleanObject);
      }

      const cleaned = { ...obj };
      if (cleaned.attachments && Array.isArray(cleaned.attachments)) {
        const before = cleaned.attachments.length;
        cleaned.attachments = cleaned.attachments.filter((a: any) => {
          const isSmallVideo = a.type === 'video/webm' && a.size < 10000;
          if (isSmallVideo) removedCount++;
          return !isSmallVideo;
        });
        if (cleaned.attachments.length !== before) modified = true;
      }
      for (const key in cleaned) {
        if (key !== 'attachments' && cleaned[key] && typeof cleaned[key] === 'object') {
          cleaned[key] = cleanObject(cleaned[key]);
        }
      }
      return cleaned;
    }
    const cleanedData = cleanObject(data);
    if (modified) {
      fs.writeFileSync(filePath, JSON.stringify(cleanedData, null, 2));
    }
  } catch (error) {
    // Ignore errors
  }
}

import { FormattingRule } from './cyaPage.validation';

export const cyaFormattingRules: FormattingRule[] = [
    {
        originalQuestion: 'Total rent arrears',
        answerFormatter: (answer) => {
            if (typeof answer === 'string') {
                const num = parseFloat(answer);
                if (!isNaN(num)) {
                    return `£${num.toLocaleString('en-GB', { minimumFractionDigits: 2 })}`;
                }
            }
            return answer;
        },
        answerPatterns: [
            '£1,000.00'
        ]
    }
];

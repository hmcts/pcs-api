import {performValidation} from "@utils/controller-enforcement";

export const validateHiddenElements = async (
  hiddenElements: {
    elementType: 'paragraph' | 'listItem'| 'p';
    text: string;
  }[] = []

) => {
  for (const element of hiddenElements) {
    await performValidation('text', {
      elementType: element.elementType,
      text: element.text
    });}

};

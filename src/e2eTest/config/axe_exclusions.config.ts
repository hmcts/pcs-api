export const axe_Exclusions = [
  '.govuk-notification-banner__link', // temporary exclusion for link-in-text-block POFCC-165
  '#target-size',// all these issues are found on Case tabs . These failures are not consistent as they are XUI behaviour
  '#target-offset',
  '.sort-button-icon',
  '.toggle-button-icon',
  '.overlay-toggle',
  'button.overlay-toggle',
  'ccd-case-file-view-folder-toggle',
  'ccd-case-file-view-folder-sort',
  'ccd-case-file-view-overlay-menu',
  '#label',
  '#presentational-role',
  '#non-empty-placeholder',
  '#non-empty-title',
  '#aria-labelledby',
  '#aria-label',
  '#explicit-label',
  '#implicit-label',
  '#aria-required-parent',
  'button[role="treeitem"]',
];

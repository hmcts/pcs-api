// Conventions enforced against the schema that src/main/resources/db/migration produces.
// Run with bin/db-schema-lint.sh, which migrates a throwaway Postgres and points this at it.
// Rule reference: https://github.com/kristiandupont/schemalint/tree/master/src/rules

// ignored.json holds the violations that existed when this lint was introduced, so a rule can be
// enforced on new migrations without first clearing its backlog. Regenerate with
// `bin/db-schema-lint.sh --update-ignored` — it should only ever shrink.
const ignored =
  process.env.SCHEMALINT_INCLUDE_IGNORED === "true"
    ? []
    : require("./ignored.json");

/** @type {import("schemalint").Config } */
module.exports = {
  connection: {
    host: process.env.PGHOST || "localhost",
    port: Number(process.env.PGPORT || 55432),
    user: process.env.PGUSER || "postgres",
    password: process.env.PGPASSWORD || "postgres",
    database: process.env.PGDATABASE || "pcs",
    charset: "utf8",
  },

  schemas: [{ name: "public" }, { name: "draft" }],

  rules: {
    // Clean as of V030, so these fail on the first violation anywhere in the schema.
    "name-casing": ["error", "snake"],
    "prefer-jsonb-to-json": ["error"],
    "prefer-identity-to-serial": ["error"],
    // All 86 foreign keys are already ON UPDATE NO ACTION. ON DELETE is deliberately left
    // unconstrained (70 NO ACTION, 15 CASCADE, 1 SET NULL) — it is a modelling decision per
    // relationship, not a convention.
    "reference-actions": ["error", { onUpdate: "NO ACTION" }],

    // Enforced on new objects only — the violations that already existed are in ignored.json.
    "require-primary-key": ["error"],
    "prefer-text-to-varchar": ["error"],
    "prefer-timestamptz-to-timestamp": ["error"],
    "index-referencing-column": ["error"],
    // New tables must be singular; the existing plural ones are permanent exceptions below.
    "name-inflection": ["error", "singular"],

    // Off: PCS models closed value sets as Postgres enum types (V001 creates 20+ of them),
    // so the rule's preference for text + CHECK contradicts a deliberate choice.
    "prefer-text-with-check-to-enum": ["off"],

    // Off: there is no audit-column convention to enforce — 7 of 57 tables have created_at,
    // 5 have created and 2 have created_date.
    "mandatory-columns": ["off"],

    // Off: PCS has no row-level security; access control sits in CCD and the API layer.
    "row-level-security": ["off"],
  },

  ignores: [
    // Owned by Flyway, not by us.
    { identifierPattern: "public\\.flyway_schema_history.*", rulePattern: ".*" },
    // Created by db-scheduler-spring-boot-starter.
    { identifierPattern: "public\\.scheduled_tasks.*", rulePattern: ".*" },

    // Permanent name-inflection exceptions rather than ignored.json entries: renaming a live table
    // is a breaking change that has to be staged across releases, and several of these are not
    // really plurals (flag_ref_data, help_with_fees, rent_arrears) — the inflection library just
    // sees a trailing s.
    ...[
      "draft.draft_case_data",
      "public.claim_party_contact_details",
      "public.contact_preferences",
      "public.enf_selected_defendants",
      "public.flag_ref_data",
      "public.help_with_fees",
      "public.household_circumstances",
      "public.possession_alternatives",
      "public.reasonable_adjustments",
      "public.regular_expenses",
      "public.rent_arrears",
    ].map((identifier) => ({ identifier, rule: "name-inflection" })),

    ...ignored,
  ],
};

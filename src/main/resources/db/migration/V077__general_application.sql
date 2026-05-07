CREATE TABLE help_with_fees (
  id            UUID PRIMARY KEY,
  hwf_reference VARCHAR(16) NOT NULL
);

CREATE TABLE general_application (
  id                    UUID PRIMARY KEY,
  case_id               UUID NOT NULL REFERENCES public.pcs_case (id),
  sot_id                UUID REFERENCES public.statement_of_truth (id),
  hwf_id                UUID REFERENCES public.help_with_fees (id),
  type                  VARCHAR(50) NOT NULL,
  state                 VARCHAR(30),
  party_id              UUID NOT NULL REFERENCES public.party (id),
  within_14_days        YES_NO,
  need_hwf              YES_NO,
  applied_for_hwf       YES_NO,
  other_parties_agreed  YES_NO,
  without_notice        YES_NO,
  without_notice_reason VARCHAR(6800),
  what_order_wanted     VARCHAR(6800),
  documents_uploaded    YES_NO,
  language_used         VARCHAR(30)
);

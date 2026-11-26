-- The eviction a bailiff has scheduled for a warrant of possession
CREATE TABLE public.scheduled_eviction (
    id                                              integer NOT NULL GENERATED ALWAYS AS IDENTITY,
    court_given_permission_to_dispense_with_service public.yes_no NOT NULL,
    scheduled_eviction_date                         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at                                      TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_at                                TIMESTAMP WITH TIME ZONE NOT NULL,
    enf_warrant_id                                  INTEGER NOT NULL,

    CONSTRAINT scheduled_eviction_pk PRIMARY KEY (id),
    CONSTRAINT scheduled_eviction_enf_warrant_fk
        FOREIGN KEY (enf_warrant_id) REFERENCES public.enf_warrant (id)
);

CREATE INDEX idx_scheduled_eviction_enf_warrant_id ON public.scheduled_eviction (enf_warrant_id);

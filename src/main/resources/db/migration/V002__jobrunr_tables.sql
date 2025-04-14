-- V002__jobrunr_tables.sql
-- Creates JobRunr tables and indexes (PostgreSQL)

CREATE TABLE public.jobrunr_backgroundjobservers (
  id CHARACTER(36) NOT NULL,
  workerpoolsize INTEGER NOT NULL,
  pollintervalinseconds INTEGER NOT NULL,
  firstheartbeat TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
  lastheartbeat TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
  running INTEGER NOT NULL,
  systemtotalmemory BIGINT NOT NULL,
  systemfreememory BIGINT NOT NULL,
  systemcpuload NUMERIC(3,2) NOT NULL,
  processmaxmemory BIGINT NOT NULL,
  processfreememory BIGINT NOT NULL,
  processallocatedmemory BIGINT NOT NULL,
  processcpuload NUMERIC(3,2) NOT NULL,
  deletesucceededjobsafter VARCHAR(32),
  permanentlydeletejobsafter VARCHAR(32),
  name VARCHAR(128),
  CONSTRAINT jobrunr_backgroundjobservers_pkey PRIMARY KEY (id)
);

CREATE TABLE public.jobrunr_jobs (
  id CHARACTER(36) NOT NULL,
  version INTEGER NOT NULL,
  jobasjson TEXT NOT NULL,
  jobsignature VARCHAR(512) NOT NULL,
  state VARCHAR(36) NOT NULL,
  createdat TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  updatedat TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  scheduledat TIMESTAMP WITHOUT TIME ZONE,
  recurringjobid VARCHAR(128),
  CONSTRAINT jobrunr_jobs_pkey PRIMARY KEY (id)
);

CREATE TABLE public.jobrunr_metadata (
  id VARCHAR(156) NOT NULL,
  name VARCHAR(92) NOT NULL,
  owner VARCHAR(64) NOT NULL,
  value TEXT NOT NULL,
  createdat TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  updatedat TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  CONSTRAINT jobrunr_metadata_pkey PRIMARY KEY (id)
);

CREATE TABLE public.jobrunr_recurring_jobs (
  id CHARACTER(128) NOT NULL,
  version INTEGER NOT NULL,
  jobasjson TEXT NOT NULL,
  createdat BIGINT DEFAULT 0 NOT NULL,
  CONSTRAINT jobrunr_recurring_jobs_pkey PRIMARY KEY (id)
);

CREATE TABLE public.jobrunr_migrations (
  id CHARACTER(36) NOT NULL,
  script VARCHAR(64) NOT NULL,
  installedon VARCHAR(29) NOT NULL,
  CONSTRAINT jobrunr_migrations_pkey PRIMARY KEY (id)
);

-- Indexes for jobrunr_backgroundjobservers
CREATE INDEX jobrunr_bgjobsrvrs_fsthb_idx ON public.jobrunr_backgroundjobservers (firstheartbeat);
CREATE INDEX jobrunr_bgjobsrvrs_lsthb_idx ON public.jobrunr_backgroundjobservers (lastheartbeat);

-- Indexes for jobrunr_jobs
CREATE INDEX jobrunr_job_created_at_idx ON public.jobrunr_jobs (createdat);
CREATE INDEX jobrunr_job_rci_idx ON public.jobrunr_jobs (recurringjobid);
CREATE INDEX jobrunr_job_scheduled_at_idx ON public.jobrunr_jobs (scheduledat);
CREATE INDEX jobrunr_job_signature_idx ON public.jobrunr_jobs (jobsignature);
CREATE INDEX jobrunr_jobs_state_updated_idx ON public.jobrunr_jobs (state, updatedat);
CREATE INDEX jobrunr_state_idx ON public.jobrunr_jobs (state);

-- Indexes for jobrunr_recurring_jobs
CREATE INDEX jobrunr_recurring_job_created_at_idx ON public.jobrunr_recurring_jobs (createdat);

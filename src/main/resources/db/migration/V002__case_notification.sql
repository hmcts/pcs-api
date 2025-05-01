CREATE TABLE case_notification
(
  notification_id UUID NOT NULL,
  case_id         UUID NOT NULL,
  provider_notification_id UUID NOT NULL,
  submitted_at    TIMESTAMP WITHOUT TIME ZONE,
  scheduled_at    TIMESTAMP WITHOUT TIME ZONE,
  last_updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  status          VARCHAR(255) NOT NULL,
  type            VARCHAR(255) NOT NULL,
  recipient       VARCHAR(255) NOT NULL,
  CONSTRAINT pk_case_notification PRIMARY KEY (notification_id)
);

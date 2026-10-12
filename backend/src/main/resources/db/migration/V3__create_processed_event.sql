CREATE TABLE processed_event
(
    consumer_group varchar(100) NOT NULL,
    event_id uuid NOT NULL,
    processed_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_group, event_id)
);
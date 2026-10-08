CREATE TABLE "customers"
(
    "customer_id" uuid PRIMARY KEY,
    "full_name"   varchar(100) NOT NULL,
    "email"       varchar(100) NOT NULL,
    "status"      varchar(10)  NOT NULL
);

CREATE TABLE "customer_interactions"
(
    "interaction_id" uuid PRIMARY KEY,
    "customer_id"    uuid         NOT NULL REFERENCES "customers" ("customer_id") DEFERRABLE INITIALLY IMMEDIATE,
    "channel"        varchar(10)  NOT NULL,
    "summary"        VARCHAR(500) NOT NULL,
    "actor"          varchar(30)  NOT NULL,
    "occurred_at"    timestamp with time zone NOT NULL,
    "correlation_id" varchar(60)  NOT NULL
);

CREATE INDEX ON "customer_interactions" ("customer_id", "occurred_at");

CREATE INDEX ON "customer_interactions" ("correlation_id");

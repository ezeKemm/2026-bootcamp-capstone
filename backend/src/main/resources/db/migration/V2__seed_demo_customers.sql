-- Demo fixtures used across the capstone (Lab 48: Amina Khan ACTIVE, Ravi Singh PROSPECT).
-- Fixed UUIDs so the frontend, tests and docs can refer to the same customers.
INSERT INTO "customers" ("customer_id", "full_name", "email", "status") VALUES
                                                                            ('5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11', 'Amina Khan', 'amina.khan@example.com', 'ACTIVE'),
                                                                            ('7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1', 'Ravi Singh', 'ravi.singh@example.com', 'PROSPECT');
CREATE TABLE app_users(username varchar(80) PRIMARY KEY,password_hash varchar(100) NOT NULL,role varchar(40) NOT NULL CHECK(role IN ('ADMIN','WAREHOUSE_MANAGER','VIEWER')));
CREATE TABLE login_audit(id uuid PRIMARY KEY,username varchar(80) NOT NULL,success boolean NOT NULL,timestamp timestamptz NOT NULL DEFAULT now(),correlation_id varchar(64) NOT NULL);
CREATE INDEX login_audit_recent ON login_audit(timestamp DESC);

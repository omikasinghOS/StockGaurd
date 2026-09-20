CREATE TABLE outbox(event_id uuid PRIMARY KEY,topic varchar(100) NOT NULL,partition_key varchar(100) NOT NULL,body text NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),published_at timestamptz);
CREATE INDEX outbox_pending ON outbox(created_at) WHERE published_at IS NULL;
CREATE TABLE processed_events(event_id uuid PRIMARY KEY,event_type varchar(80) NOT NULL,processed_at timestamptz NOT NULL DEFAULT now());

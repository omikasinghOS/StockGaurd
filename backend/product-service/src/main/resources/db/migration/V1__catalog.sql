CREATE TABLE products (
 id uuid PRIMARY KEY, sku varchar(40) NOT NULL UNIQUE, name varchar(120) NOT NULL,
 category varchar(80) NOT NULL, price numeric(10,2) NOT NULL CHECK(price>0), supplier varchar(120) NOT NULL,
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL
);
INSERT INTO products SELECT ('00000000-0000-0000-0000-' || lpad(n::text,12,'0'))::uuid,
 'SKU-'||n, (ARRAY['Industrial Sensor','Safety Relay','Control Module','Power Supply','Network Switch','Actuator'])[1+mod(n,6)]||' '||n,
 (ARRAY['Sensors','Electrical','Automation'])[1+mod(n,3)], 500+mod(n*173,15000),
 (ARRAY['Acme Components','Pune Industrial','Northstar Supply'])[1+mod(n,3)], now(),now()
FROM (SELECT generate_series(101,122) n UNION ALL SELECT 215 UNION ALL SELECT 216) s;

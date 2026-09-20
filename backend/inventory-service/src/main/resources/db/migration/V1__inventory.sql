CREATE TABLE warehouses (id uuid PRIMARY KEY,code varchar(20) NOT NULL UNIQUE,name varchar(100) NOT NULL,city varchar(80) NOT NULL);
INSERT INTO warehouses VALUES
 ('10000000-0000-0000-0000-000000000001','WH-MUM','Mumbai Central','Mumbai'),
 ('10000000-0000-0000-0000-000000000002','WH-PUN','Pune Industrial','Pune'),
 ('10000000-0000-0000-0000-000000000003','WH-JAI','Jaipur North','Jaipur');
CREATE TABLE inventory (
 id uuid PRIMARY KEY,product_id uuid NOT NULL,warehouse_id uuid NOT NULL REFERENCES warehouses,
 available_quantity integer NOT NULL CHECK(available_quantity>=0), reserved_quantity integer NOT NULL CHECK(reserved_quantity>=0),
 reorder_point integer NOT NULL CHECK(reorder_point>=0),safety_stock integer NOT NULL CHECK(safety_stock>=0),
 version bigint NOT NULL DEFAULT 0,updated_at timestamptz NOT NULL,
 UNIQUE(product_id,warehouse_id)
);
INSERT INTO inventory SELECT gen_random_uuid(),('00000000-0000-0000-0000-'||lpad(n::text,12,'0'))::uuid,w.id,
 CASE WHEN n=215 THEN 8 WHEN n=101 THEN 1 WHEN n=108 THEN 0 ELSE 80+mod(n*13,350) END,0,40,20,0,now()
FROM (SELECT generate_series(101,122) n UNION ALL SELECT 215 UNION ALL SELECT 216) p CROSS JOIN warehouses w;

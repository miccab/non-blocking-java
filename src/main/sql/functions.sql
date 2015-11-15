CREATE TABLE product
(
  id integer NOT NULL,
  name character varying(64) NOT NULL,
  CONSTRAINT "PK_PRODUCT" PRIMARY KEY (id)
);

CREATE TABLE product_description
(
  id integer NOT NULL,
  description text NOT NULL,
  CONSTRAINT "PK_PRODUCT_DESCRIPTION" PRIMARY KEY (id)
);

CREATE TABLE product_group
(
  id integer NOT NULL,
  name character varying(64) NOT NULL,
  CONSTRAINT "PK_PRODUCT_GROUP" PRIMARY KEY (id)
)
;

CREATE TABLE product_to_group
(
  product_id integer NOT NULL,
  product_group_id integer NOT NULL,
  CONSTRAINT "PK_PRODUCT_TO_GROUP" PRIMARY KEY (product_id, product_group_id)
)
;

CREATE OR REPLACE FUNCTION find_product_name(integer)
  RETURNS character varying AS
$BODY$
DECLARE
    product_id ALIAS FOR $1;
    product_rec product%ROWTYPE;
BEGIN
 if product_id & 1 = 1 then
	PERFORM pg_sleep(1);
 end if;
 select into product_rec * from product where id  = product_id;
 IF NOT FOUND THEN
	RAISE EXCEPTION 'product % not found', product_id;
 else
	return product_rec.name;
 END IF;
END;
$BODY$
  LANGUAGE plpgsql;

ALTER FUNCTION find_product_name(integer)
  OWNER TO dropwizard;

CREATE OR REPLACE FUNCTION find_product_description(integer)
  RETURNS character varying AS
$BODY$
DECLARE
    product_id ALIAS FOR $1;
    product_rec product_description%ROWTYPE;
BEGIN
 if product_id & 1 = 1 then
	PERFORM pg_sleep(1);
 end if;
 select into product_rec * from product_description where id  = product_id;
 IF NOT FOUND THEN
	RAISE EXCEPTION 'product % not found', product_id;
 else
	return product_rec.description;
 END IF;
END;
$BODY$
  LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION gen_data() RETURNS VOID
LANGUAGE 'plpgsql' AS $$
    BEGIN
        FOR i IN 1 .. 1000000 LOOP
            insert into product (id,name) values (i, 'product_' || i);
        END LOOP;
    END;
$$;

select gen_data();


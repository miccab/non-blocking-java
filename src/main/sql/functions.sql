CREATE TABLE product
(
  id integer NOT NULL,
  name character varying(64) NOT NULL,
  CONSTRAINT "PK_PRODUCT" PRIMARY KEY (id)
);

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



CREATE OR REPLACE FUNCTION gen_data() RETURNS VOID
LANGUAGE 'plpgsql' AS $$
    BEGIN
        FOR i IN 1 .. 1000000 LOOP
            insert into product (id,name) values (i, 'product_' || i);
        END LOOP;
    END;
$$;

select gen_data();


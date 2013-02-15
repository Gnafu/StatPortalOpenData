

-- ----------------------------
-- Table structure for "public"."md_data"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_data";
CREATE TABLE "public"."md_data" (
"name" varchar(100),
"description" varchar(250),
"db_name" varchar(50),
"table_name" varchar(100),
"num_rows" int4,
"available" bool,
"last_update" timestamp(6),
"id_lu_data_type" int4,
"generic_grants" bool,
"id" int4 NOT NULL,
"id_metadata" int4,
"id_owner_user" int4,
"uid" varchar(36) NOT NULL,
"content_desc" text
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Table structure for "public"."md_data_dim"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_data_dim";
CREATE TABLE "public"."md_data_dim" (
"dimcode_field" varchar(50),
"alias" varchar(100),
"description" varchar(250),
"id" int4 NOT NULL,
"id_hier_node" int4,
"id_data" int4,
"uid" varchar(36) NOT NULL
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Table structure for "public"."md_generic_column"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_generic_column";
CREATE TABLE "public"."md_generic_column" (
"id" int4 NOT NULL,
"id_data" int4,
"cardinality" int4,
"column_field" varchar,
"alias" varchar,
"descriptivefield" bool
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Table structure for "public"."md_hier_node"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_hier_node";
CREATE TABLE "public"."md_hier_node" (
"name" varchar(100),
"table_name" varchar(100),
"pk_field" varchar(50),
"desc_field" varchar(50),
"description" varchar(250),
"num_rows" int4,
"row_size" int4,
"last_update" timestamp(6),
"generic_grants" bool,
"sort_field" varchar(50),
"id" int4 NOT NULL,
"id_hierarchy" int4,
"id_user_owner" int4
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Records of md_hier_node
-- ----------------------------

INSERT INTO "public"."md_hier_node" VALUES ('Paesi', 'nod_paesi', 'id', 'nome', null, null, null, null, null, null, '1515', '19512', null);
INSERT INTO "public"."md_hier_node" VALUES ('Località', 'nod_localita', 'id', 'nome', null, null, null, null, null, null, '1516', '19512', null);
INSERT INTO "public"."md_hier_node" VALUES ('Sezioni censuarie - Toscana', 'nod_sezioni_toscana', 'id', 'descr', null, null, null, null, null, null, '1517', '19512', null);
INSERT INTO "public"."md_hier_node" VALUES ('Fasce età', 'nod_fasce_eta', 'id', 'descr', null, '22', null, null, null, null, '1818', '181818', null);
INSERT INTO "public"."md_hier_node" VALUES ('Comuni', 'NOD_COMUNI', 'ID', 'NOME', null, '8113', '110', null, 't', null, '277147', '19512', null);
INSERT INTO "public"."md_hier_node" VALUES ('Provincia', 'NOD_PROVINCE', 'ID', 'NOME', null, '104', '82', null, 'f', null, '53532', '19512', null);
INSERT INTO "public"."md_hier_node" VALUES ('Province 2013', 'nod_province_nuove', 'id', 'descr', null, '71', null, null, 't', null, '1919', '19512', null);
INSERT INTO "public"."md_hier_node" VALUES ('Regioni', 'nod_regioni2', 'id', 'nome', null, '20', null, null, null, null, '838164', '19512', null);
INSERT INTO "public"."md_hier_node" VALUES ('Anno', 'NOD_ANNI', 'ID', 'NOME', 'Livello di Dettaglio Annuo', '20', '44', null, 't', null, '79894', '851123', null);

-- ----------------------------
-- Table structure for "public"."md_hierarchy"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_hierarchy";
CREATE TABLE "public"."md_hierarchy" (
"name" varchar(100),
"description" varchar(250),
"id_lu_hier_type" int4,
"id" int4 NOT NULL
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Records of md_hierarchy
-- ----------------------------
INSERT INTO "public"."md_hierarchy" VALUES ('Gerarchia Territoriale', 'International Benchmarking Database 2006 - Territoriale', '1', '19512');
INSERT INTO "public"."md_hierarchy" VALUES ('Fasce eta', '', '3', '181818');
INSERT INTO "public"."md_hierarchy" VALUES ('ASR_PROVINCE', null, '3', '734660');
INSERT INTO "public"."md_hierarchy" VALUES ('Temporale', 'Gerarchia Temporale ', '2', '851123');

-- ----------------------------
-- Table structure for "public"."md_layer"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_layer";
CREATE TABLE "public"."md_layer" (
"id" int4 NOT NULL,
"name" varchar(50) NOT NULL,
"path" varchar(250) NOT NULL
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Records of md_layer
-- ----------------------------
INSERT INTO "public"."md_layer" VALUES ('1', 'regioni', 'regioni');
INSERT INTO "public"."md_layer" VALUES ('2', 'comuni-geo2', 'comuni-geo2');
INSERT INTO "public"."md_layer" VALUES ('3', 'province-geo', 'province-geo');
INSERT INTO "public"."md_layer" VALUES ('4', 'paesi-geo', 'paesi-geo');
INSERT INTO "public"."md_layer" VALUES ('5', 'sezioni-prova', 'sezioni-prova');
INSERT INTO "public"."md_layer" VALUES ('6', 'regioni2', 'regioni2');


-- ----------------------------
-- Records of geometry_columns
-- ----------------------------
INSERT INTO "public"."geometry_columns" VALUES ('', 'public', 'regioni', 'the_geom', '2', '4326', 'MULTIPOLYGON');
INSERT INTO "public"."geometry_columns" VALUES ('', 'public', 'comuni-geo2', 'the_geom', '2', '4326', 'MULTIPOLYGON');
INSERT INTO "public"."geometry_columns" VALUES ('', 'public', 'province-geo', 'the_geom', '2', '4326', 'MULTIPOLYGON');
INSERT INTO "public"."geometry_columns" VALUES ('', 'public', 'paesi-geo', 'the_geom', '2', '4326', 'MULTIPOLYGON');
INSERT INTO "public"."geometry_columns" VALUES ('', 'public', 'sezioni-prova', 'the_geom', '2', '4326', 'MULTIPOLYGON');
INSERT INTO "public"."geometry_columns" VALUES ('', 'public', 'regioni2', 'the_geom', '2', '4326', 'MULTIPOLYGON');


-- ----------------------------
-- Table structure for "public"."md_lu_hier_type"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_lu_hier_type";
CREATE TABLE "public"."md_lu_hier_type" (
"id" int4 NOT NULL,
"name" varchar(100)
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Records of md_lu_hier_type
-- ----------------------------
INSERT INTO "public"."md_lu_hier_type" VALUES ('1', 'TERRITORIALE');
INSERT INTO "public"."md_lu_hier_type" VALUES ('2', 'TEMPORALE');
INSERT INTO "public"."md_lu_hier_type" VALUES ('3', 'GENERICA');

-- ----------------------------
-- Table structure for "public"."md_measure_fields"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_measure_fields";
CREATE TABLE "public"."md_measure_fields" (
"measure_field" varchar(50),
"alias" varchar(100),
"description" varchar(250),
"measure_units" varchar(50),
"pos" int4,
"id" int4 NOT NULL,
"id_data" int4,
"decimal_places" int2,
"uid" varchar(36) NOT NULL
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Table structure for "public"."md_rel_hier_node"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_rel_hier_node";
CREATE TABLE "public"."md_rel_hier_node" (
"fk_field" varchar(50),
"id_parent_node" int4 NOT NULL,
"id_child_node" int4 NOT NULL
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Table structure for "public"."md_rel_layer_node"
-- ----------------------------
DROP TABLE IF EXISTS "public"."md_rel_layer_node";
CREATE TABLE "public"."md_rel_layer_node" (
"id_hier_node" int4 NOT NULL,
"id_layer" int4 NOT NULL,
"layer_field" varchar(50) NOT NULL,
"node_field" varchar(50)
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Records of md_rel_layer_node
-- ----------------------------
INSERT INTO "public"."md_rel_layer_node" VALUES ('1515', '4', 'idjoin', 'id');
INSERT INTO "public"."md_rel_layer_node" VALUES ('1517', '5', 'idjoin', 'id');
INSERT INTO "public"."md_rel_layer_node" VALUES ('53532', '3', 'idjoin', 'id');
INSERT INTO "public"."md_rel_layer_node" VALUES ('277147', '2', 'idjoin', 'id');
INSERT INTO "public"."md_rel_layer_node" VALUES ('838164', '6', 'idjoin', 'id');


-- ----------------------------
-- Alter Sequences Owned By 
-- ----------------------------

-- ----------------------------
-- Primary Key structure for table "public"."md_data"
-- ----------------------------
ALTER TABLE "public"."md_data" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_data_dim"
-- ----------------------------
ALTER TABLE "public"."md_data_dim" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_generic_column"
-- ----------------------------
ALTER TABLE "public"."md_generic_column" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_hier_node"
-- ----------------------------
ALTER TABLE "public"."md_hier_node" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_hierarchy"
-- ----------------------------
ALTER TABLE "public"."md_hierarchy" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_layer"
-- ----------------------------
ALTER TABLE "public"."md_layer" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_lu_hier_type"
-- ----------------------------
ALTER TABLE "public"."md_lu_hier_type" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_measure_fields"
-- ----------------------------
ALTER TABLE "public"."md_measure_fields" ADD PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table "public"."md_rel_hier_node"
-- ----------------------------
ALTER TABLE "public"."md_rel_hier_node" ADD PRIMARY KEY ("id_child_node", "id_parent_node");

-- ----------------------------
-- Primary Key structure for table "public"."md_rel_layer_node"
-- ----------------------------
ALTER TABLE "public"."md_rel_layer_node" ADD PRIMARY KEY ("id_hier_node");

-- ----------------------------
-- Foreign Key structure for table "public"."md_data_dim"
-- ----------------------------
ALTER TABLE "public"."md_data_dim" ADD FOREIGN KEY ("id_hier_node") REFERENCES "public"."md_hier_node" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."md_data_dim" ADD FOREIGN KEY ("id_data") REFERENCES "public"."md_data" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Key structure for table "public"."md_generic_column"
-- ----------------------------
ALTER TABLE "public"."md_generic_column" ADD FOREIGN KEY ("id_data") REFERENCES "public"."md_data" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Key structure for table "public"."md_hier_node"
-- ----------------------------
ALTER TABLE "public"."md_hier_node" ADD FOREIGN KEY ("id_hierarchy") REFERENCES "public"."md_hierarchy" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Key structure for table "public"."md_hierarchy"
-- ----------------------------
ALTER TABLE "public"."md_hierarchy" ADD FOREIGN KEY ("id_lu_hier_type") REFERENCES "public"."md_lu_hier_type" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Key structure for table "public"."md_measure_fields"
-- ----------------------------
ALTER TABLE "public"."md_measure_fields" ADD FOREIGN KEY ("id_data") REFERENCES "public"."md_data" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Key structure for table "public"."md_rel_hier_node"
-- ----------------------------
ALTER TABLE "public"."md_rel_hier_node" ADD FOREIGN KEY ("id_parent_node") REFERENCES "public"."md_hier_node" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."md_rel_hier_node" ADD FOREIGN KEY ("id_child_node") REFERENCES "public"."md_hier_node" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Key structure for table "public"."md_rel_layer_node"
-- ----------------------------
ALTER TABLE "public"."md_rel_layer_node" ADD FOREIGN KEY ("id_hier_node") REFERENCES "public"."md_hier_node" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."md_rel_layer_node" ADD FOREIGN KEY ("id_layer") REFERENCES "public"."md_layer" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

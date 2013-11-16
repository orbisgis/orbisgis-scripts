/* SCRIPT SQL : LAYER FROM BD Topo
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :
Create Layers from BD Topo datas

NEED TO SELECT A STUDY AREA RENAME TO SELECTION
NEED LAYERS FROM BD_TOPO :  
BUILDINGS :
- BATI_INDIFFERENCIE
- BATI_INDUSTRIEL
- BATI_REMARQUABLE

ROADS :
- ROUTE

VEGETATION :
- ZONE_VEGETATION

HYDROLOGY :
- SURFACE_EAU

OUTPUT :
- study_area
- bati
- routes
- route_surface
- veg
- hydrologie

@author Anne Bernabe
Creation :  09//06/2013
Last modification : 09/06/2013
Details : drop table

 ---------------------------------------------------------------------------------------------------
 */

-- 1- Study area
create table study_area as select st_union(the_geom)as the_geom from SELECTION;


-- 2- Create Layers 
-- (a) buildings as  table bati
create table bati_indifferencie2  as select a.* from BATI_INDIFFERENCIE a,study_area b  where ST_Intersects(b.the_geom, a.the_geom); 
create table bati_industriel2 as select a.* from BATI_INDUSTRIEL a, study_area b where ST_Intersects(b.the_geom, a.the_geom); 
create table bati_remarquable2 as select a.* from BATI_REMARQUABLE a, study_area b where ST_Intersects(b.the_geom, a.the_geom); 

--unify buildings area in bati
create table bati1 as select the_geom,ID AS id,HAUTEUR AS hauteur,Z_MIN AS z_min,Z_MAX AS z_max  from bati_indifferencie2  union select the_geom,ID AS id,HAUTEUR AS hauteur,Z_MIN AS z_min,Z_MAX AS z_max from bati_industriel2 ;
create table bati as select the_geom,ID AS id,HAUTEUR AS hauteur,Z_MIN AS z_min,Z_MAX AS z_max  from bati_remarquable2  union select the_geom,id,hauteur,z_min,z_max from bati1 ;

-- (b) vegetation as  table veg
create table veg  as select a.the_geom, a.ID AS id from ZONE_VEGETATION a,study_area b  where st_intersects(b.the_geom, a.the_geom); 

-- (c) roads as  table routes 
create table routes as select a.the_geom, a.ID AS id, ST_length(a.the_geom) AS l_route, a.LARGEUR AS largeur from ROUTE a, study_area b  where st_intersects(b.the_geom, a.the_geom); 
create table route_surface as select st_buffer(the_geom, largeur) as the_geom from routes;

-- (d) hydrologic network as table hydro
create table hydro as select a.the_geom, a.ID AS id from SURFACE_EAU a, study_area b where st_isvalid(b.the_geom) and st_isvalid(b.the_geom)and ST_Intersects(a.the_geom,b.the_geom) ;

-- filter to obtain surface (polygone)
create table hydro_lim as select * from st_explode(hydro);
create table hydro_lim2 as select * from hydro_lim where st_dimension(the_geom) = 2;
create table hydrologie as select st_union(the_geom)as the_geom from hydro_lim2;

-- 3- Drop table
drop table SELECTION;
drop table bati_indifferencie2 purge;
drop table bati_industriel2 purge;
drop table bati_remarquable2 purge;
drop table BATI_INDIFFERENCIE ;
drop table BATI_INDUSTRIEL ;
drop table BATI_REMARQUABLE ;
drop table bati1 purge;
drop table ROUTE ;
drop table ZONE_VEGETATION;
drop table hydro_lim purge;
drop table hydro_lim2 purge;
drop table SURFACE_EAU ;

/* SCRIPT SQL : PREPROCESS INDICATOR CALCULATION
LICENCE : 

Orbisgis is distributed under gpl 3 license. it is produced by the "atelier sig"
team of the irstv institute <http://www.irstv.fr/> cnrs fr 2488.

copyright (c) 2007-2012 irstv (fr cnrs 2488)

This file is part of orbisgis.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

1-  Create study area 
2-  Cut layers according to the study area
3-  Cut study area into urban islands 
4-  Calculate intersection of layers and urban islands


REQUIREMENT :

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
- route_surface
- veg
- hydrologie
- ilots
- intersection_bati
- intersection_route
- intersection_veg



@author Anne Bernabe
creation : 24/04/2013
last modification :24/04/2013
details : update intersection calculation 

 ---------------------------------------------------------------------------------------------------
 */
 
 -- 1- STUDY AREA

-- UNIFY SELECTION
create table study_area as select st_union(the_geom)as the_geom from SELECTION;

-- 2- CUT LAYERS 

-- (a) buildings as  table bati
create table bati_indifferencie2  as select a.* from BATI_INDIFFERENCIE a,study_area b  where st_contains(b.the_geom, a.the_geom); 
create table bati_industriel2 as select a.* from BATI_INDUSTRIEL a, study_area b where st_contains(b.the_geom, a.the_geom); 
create table bati_remarquable2 as select a.* from BATI_REMARQUABLE a, study_area b where st_contains(b.the_geom, a.the_geom); 

--unify buildings area in bati
create table bati1 as select the_geom,ID AS id,HAUTEUR AS hauteur,Z_MIN AS z_min,Z_MAX AS z_max  from bati_indifferencie2  union select the_geom,ID AS id,HAUTEUR AS hauteur,Z_MIN AS z_min,Z_MAX AS z_max from bati_industriel2 ;
create table bati as select the_geom,ID AS id,HAUTEUR AS hauteur,Z_MIN AS z_min,Z_MAX AS z_max  from bati_remarquable2  union select the_geom,id,hauteur,z_min,z_max from bati1 ;

-- (b) vegetation as  table veg
create table veg  as select a.the_geom, a.ID AS id from ZONE_VEGETATION a,study_area b  where st_intersects(b.the_geom, a.the_geom); 


-- (c) roads as  table routes 
create table routes as select a.the_geom, a.ID AS id, ST_length(a.the_geom) AS l_route, a.LARGEUR AS largeur from ROUTE a, study_area b  where st_intersects(b.the_geom, a.the_geom); 

-- buffer to obtain surface (polygone) no union
create table route_surface as select st_buffer(the_geom, largeur) as the_geom from routes;

-- buffer to identify urban island
create table route_lim as select st_union(st_buffer(the_geom,0.5)) as the_geom from routes;

-- (d) hydrologic network as table hydro
create table hydro as select a.the_geom, a.ID AS id from SURFACE_EAU a, study_area b where st_isvalid(b.the_geom) and st_isvalid(b.the_geom)and st_contains(a.the_geom,b.the_geom) ;

-- filter to obtain surface (polygone)
create table hydro_lim as select * from st_explode(hydro);
create table hydro_lim2 as select * from hydro_lim where st_dimension(the_geom) = 2;
create table hydrologie as select st_union(the_geom)as the_geom from hydro_lim2;

-- 3- CUT STUDY AREA  INTO URBAN ISLAND

-- create limite 
-- union of route_lim and hydro_lim
create table lim as select the_geom from route_lim  union select the_geom from hydrologie where st_isvalid(the_geom);

-- unifiy
create table limite as select st_union(the_geom) as the_geom from lim where st_isvalid(the_geom);

-- create urban island 
create table ilot_join as select st_symdifference(a.the_geom, b.the_geom) as the_geom from limite a, study_area b;

-- explose urban island 
create table ilots as select * from st_explode(ilot_join);
alter table ilots rename column explod_id to id ;
alter table ilots add column  s_maille double;
alter table ilots add column  p_maille double;
update ilots set p_maille= st_length(the_geom), s_maille= st_area(the_geom);

-- 4- PREPARATION OF GEOMETRY (INTERSECTION CALCULATION)

-- (a) intersection_bati (the_geom bati | id | hauteur | p_bati | s_bati | v_bati | compa_bati [m2/m3] | ic_bati [m2/m2] )

-- intersection
create table intersection_bati_join as select st_intersection(b.the_geom,a.the_geom) 
as the_geom, a.id, b.hauteur from ilots a, bati b where st_isvalid(b.the_geom)  and st_intersects(a.the_geom,b.the_geom);

-- explode
create table intersection_bati as select * from st_explode(intersection_bati_join);


-- calculations preparation scale of buildings
alter table intersection_bati add column  p_bati double;
alter table intersection_bati add column  s_bati double;
alter table intersection_bati add column  v_bati double;
alter table intersection_bati add column  compa_bati double;
alter table intersection_bati add column  ic_bati double;
update intersection_bati set p_bati= st_length(the_geom), 
s_bati= st_area(the_geom), 
v_bati= st_area(the_geom)*hauteur, 
compa_bati= st_length(the_geom)*hauteur/(st_area(the_geom)*hauteur), 
ic_bati=st_length(the_geom)*hauteur/(st_area(the_geom)*hauteur)^(2/3) 
where (st_area(the_geom)*hauteur)!=0;

-- (b) intersection_route (the_geom route | id | s_route  )

-- intersection
create table intersection_l_route_join as select st_intersection(a.the_geom,b.the_geom) 
as the_geom, a.id, b.l_route from ilots a, routes b where st_isvalid(b.the_geom) and st_intersects(a.the_geom,b.the_geom) ;


create table intersection_route_join as select st_intersection(a.the_geom,b.the_geom) 
as the_geom, a.id from ilots a, route_surface b where st_isvalid(b.the_geom) and st_intersects(a.the_geom,b.the_geom) ;


create table intersection_route_simple as select st_simplify(the_geom, 0.015) as the_geom, id from intersection_route_join;
create table intersection_route_join_2 as select st_union(the_geom) as the_geom, id from intersection_route_simple group by id;

-- explode
create table intersection_l_route as select * from st_explode(intersection_l_route_join);
create table intersection_route as select * from st_explode(intersection_route_join_2);

-- calculations preparation
alter table intersection_route add column  s_route double;
alter table intersection_route add column  p_route double;
update  intersection_route set s_route= st_area(the_geom), p_route= st_length(the_geom);



-- (c) intersection_veg (the_geom veg | id | s_veg)

-- intersection
create table intersection_veg_join as select st_intersection(a.the_geom,b.the_geom) 
as the_geom, a.id
from ilots a, veg b where st_isvalid(b.the_geom) and st_intersects(a.the_geom,b.the_geom) ;

-- explode
create table intersection_veg as select * from st_explode(intersection_veg_join);

-- calculations preparation
alter table intersection_veg add column  s_veg double;
alter table intersection_veg add column  p_veg double;
update  intersection_veg set s_veg= st_area(the_geom), p_veg= st_length(the_geom);


-- 5-REMOVE TABLE

-- remove table 
drop table SELECTION;
drop table bati_indifferencie2 purge;
drop table bati_industriel2 purge;
drop table bati_remarquable2 purge;
drop table BATI_INDIFFERENCIE ;
drop table BATI_INDUSTRIEL ;
drop table BATI_REMARQUABLE ;
drop table bati1 purge;
drop table ROUTE ;
drop table routes purge;
drop table ZONE_VEGETATION;
drop table lim purge;
drop table hydro_lim purge;
drop table hydro_lim2 purge;
drop table hydro purge;
drop table route_lim purge;
drop table SURFACE_EAU ;
drop table limite purge;
drop table ilot_join purge;
drop table intersection_bati_join purge;
drop table intersection_route_join_2 purge;
drop table intersection_l_route_join purge;
drop table intersection_route_simple purge;
drop table intersection_route_join purge;
drop table intersection_veg_join purge;


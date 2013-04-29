/* SCRIPT SQL : CREATE MESH WITH ROAD AXES

Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

Create mesh from road axes in study area



INPUT 
-study area
-routes
-hydrologie

OUTPUT :
- grille

@author Anne Bernabe
Creation :  09//06/2013
Last modification : 09/06/2013

 ---------------------------------------------------------------------------------------------------
 */


-- buffer to identify urban island
create table route_lim as select st_union(st_buffer(the_geom,0.1)) as the_geom from routes;


-- union of route_lim and hydro_lim
create table lim as select the_geom from route_lim  union select the_geom from hydrologie where st_isvalid(the_geom);

-- unifiy
create table limite as select st_union(the_geom) as the_geom from lim where st_isvalid(the_geom);

-- create urban island 
create table grille_join as select st_symdifference(a.the_geom, b.the_geom) as the_geom from limite a, study_area b;

-- explose urban island 
create table grille as select * from st_explode(grille_join);
alter table grille rename column explod_id to id ;

-- drop table
drop table route_lim purge;
drop table lim purge;
drop table limite purge;
drop table grille_join purge;

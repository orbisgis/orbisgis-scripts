/* SCRIPT SQL : CUT LAYERS WITH A MESH

Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :
Cut layer with the mesh limits


INPUT 
-study area
-route_surface
-hydro
-veg

OUTPUT :
- intersection_bati
- intersection_route
- intersection_veg
- intersection_hydro




@author Anne Bernabe
Creation :  09/06/2013
Last modification : 09/06/2013

 ---------------------------------------------------------------------------------------------------
 */

-- (a) Buildings
-- intersection
create table intersection_bati_join as select st_intersection(b.the_geom,a.the_geom) 
as the_geom, a.id, b.hauteur from grille a, bati b where st_isvalid(b.the_geom)  and st_intersects(a.the_geom,b.the_geom);

-- explode
create table intersection_bati as select * from st_explode(intersection_bati_join);

--(b) Roads
-- intersection
create table intersection_route_join as select st_intersection(a.the_geom,b.the_geom) 
as the_geom, a.id from grille a, route_surface b where st_isvalid(b.the_geom) and st_intersects(a.the_geom,b.the_geom) ;
create table intersection_route_simple as select st_simplify(the_geom, 0.015) as the_geom, id from intersection_route_join;
create table intersection_route_join_2 as select st_union(the_geom) as the_geom, id from intersection_route_simple group by id;

-- explode
create table intersection_route as select * from st_explode(intersection_route_join_2);

--(c) Vegetation
-- intersection
create table intersection_veg_join as select st_intersection(b.the_geom,a.the_geom) 
as the_geom, a.id from grille a, veg b where st_isvalid(b.the_geom)  and st_intersects(a.the_geom,b.the_geom);

-- explode
create table intersection_veg as select * from st_explode(intersection_veg_join);

--(d) Water
-- intersection surfaces
create table intersection_hydro_join as select st_intersection(b.the_geom,a.the_geom) 
as the_geom, a.id from grille a, hydro b where st_isvalid(b.the_geom)  and st_intersects(a.the_geom,b.the_geom);

-- explode
create table intersection_hydro as select * from st_explode(intersection_hydro_join);


-- drop table
drop table intersection_bati_join purge;
drop table intersection_route_join purge;
drop table intersection_route_join_2 purge;
drop table intersection_route_simple purge;
drop table intersection_veg_join purge;
drop table intersection_hydro_join purge;


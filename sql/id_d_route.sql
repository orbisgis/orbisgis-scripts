/* SCRIPT SQL : ROADS DENSITY
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the ratio of the roads area and the surface of the mesh
d_route = s_route/s_maille


INPUT :
- intersection_route
- grille

OUTPUT :
- d_route


@author Anne Bernabe
Creation : 24/04/2013
Last modification :08/06/2013
Details : Comments
 ---------------------------------------------------------------------------------------------------
 */
 
create table s_route as select id as id, sum(st_area(the_geom)) as s_route from intersection_route group by id;
create table d_route as select a.the_geom, a.id as id, st_area(a.the_geom)as s_maille, b.s_route, b.s_route/st_area(a.the_geom)as d_route,
cast(b.s_route/st_area(a.the_geom)*5 as integer) +1 as id_d_route from grille a left join  s_route b on a.id= b.id;

drop table s_route purge;


/* NOTICE : VIEW SCALE : 

Density of roads 
1 :    0    < d_route < 0,2
2 :  0,2  < d_route  < 0,4
3 :  0,4  < d_route  < 0,6
4 :  0,6  <d_route   < 0,8
5 :  0,8  <d_route   < 1
*/

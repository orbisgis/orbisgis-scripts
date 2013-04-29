/* SCRIPT SQL : VEGETATION DENSITY
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the ratio of the vegetation and the surface of the mesh
d_veg = sum(s_veg)/s_maille

INPUT :
- intersection_veg
- grille

OUTPUT :
- d_veg


@author Anne Bernabe
Creation : 24/04/2013
Last modification : 09/06/2013
Details : Scale

 ---------------------------------------------------------------------------------------------------
 */
 
create table s_veg as select id as id, sum(st_area(the_geom)) as s_veg from intersection_veg group by id;
create table d_veg as select a.the_geom, a.id as id, st_area(a.the_geom)as s_maille, b.s_veg, b.s_veg/st_area(a.the_geom)as d_veg,
cast(b.s_veg/st_area(a.the_geom)/0.2 as integer)+1 as id_d_veg from grille a left join  s_veg b on a.id= b.id;

drop table s_veg purge;

/* NOTICE : VIEW SCALE : 

Density of buildings 
1 :  0     < d_veg < 0,2
2 :  0,2  < d_veg < 0,4
3 :  0.4  < d_veg < 0,6
5 :  0,6 < d_veg  < 0.8
*/

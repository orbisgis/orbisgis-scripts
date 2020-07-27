/* SCRIPT SQL : BUILDING DENSITY
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the ratio of the built-up area and the surface of the mesh
d_bati = s_bati/s_maille


INPUT :
- intersection_bati
- grille




OUTPUT :
- d_bati


@author Anne Bernabe
Creation : 24/04/2013
Last modification :24/04/2013
Details : Comments

 ---------------------------------------------------------------------------------------------------
 */
 
create table s_bati as select id as id, sum(st_area(the_geom)) as s_bati from intersection_bati group by id;
create table d_bati as select a.the_geom, a.id as id, st_area(a.the_geom)as s_maille, b.s_bati, b.s_bati/st_area(a.the_geom)as d_bati,
cast(b.s_bati/st_area(a.the_geom)*5 as integer)+1 as id_bati from grille a left join  s_bati b on a.id= b.id;

drop table s_bati purge;

/* NOTICE : VIEW SCALE : 

Density of buildings 
1 :    0   < d_bati <  0,2
2 :  0,2  < d_bati <  0,4
3 :  0,4  < d_bati <  0,6
4 :  0,6  < d_bati <  0,8
5 :  0,8  < d_bati <  1
*/

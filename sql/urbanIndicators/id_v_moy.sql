/* SCRIPT SQL : AVERAGE VOLUME OF BUILDINGS
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the average volume of buildings 
v_moy= sum(v_bati)/nb_bati


INPUT :
- intersection_route
- grille

OUTPUT :
- v_moy


@author Anne Bernabe
Creation : 24/04/2013
Last modification : 09/06/2013
Details : Scale and comments
 ---------------------------------------------------------------------------------------------------
 */
create table v_moy1 as select id, sum(st_area(the_geom)* hauteur)/count(the_geom) as v_moy, 
cast( sum(st_area(the_geom)* hauteur)/count(the_geom)/1500 as integer)+1 as id_v_moy from intersection_bati group by id;
create table v_moy as select a.the_geom, a.id, b.v_moy, b.id_v_moy from grille a left join  v_moy1 b on a.id= b.id;


drop table v_moy1 purge;

/* NOTICE : VIEW SCALE : 

average volume of buildings [m3]
1 :    0 < v_moy < 1500
2 : 1500 < v_moy < 3000
3 : 3000 < v_moy < 4500
4 : 4500 < v_moy < 6000
5 : 6000 < v_moy < 7500
*/

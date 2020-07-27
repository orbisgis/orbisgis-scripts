/* SCRIPT SQL : AVERAGE PERIMETER OF BUILDINGS
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the average perimeter of buildings
p_moy= sum(p_bati)/nb_bati


INPUT :
- intersection_route
- ilots

OUTPUT :
- p_moy


@author Anne Bernabe
Creation : 24/04/2013
Last modification : 09/06/2013
Details : Comments 

 ---------------------------------------------------------------------------------------------------
 */
create table  p_moy1 as select id, sum(st_length(the_geom))/count(the_geom) as p_moy, 
cast( sum (st_length(the_geom)) /count(the_geom)/50 as integer)+1 as id_p_moy from intersection_bati group by id;
create table p_moy as select a.the_geom, a.id, b.p_moy, b.id_p_moy from grille a left join  p_moy1 b on a.id= b.id;


drop table p_moy1 purge;

/* NOTICE : VIEW SCALE : 

average perimeter of buildings [m]
1 :    0   < p_moy <50
2 :  50   < p_moy < 100
3 : 100  < p_moy < 150
4 : 150  < p_moy < 200
5 :  200 < p_moy < 250
*/

/* SCRIPT SQL : COMPACITY OF BUILDINGS 
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the compacity of buildings which caracterize exchanges with outdoor
compa_bati = Sum(s_ext)/Sum(v_bati)
s_ext = p_bati *hauteur


INPUT :
- intersection_route
- grille

OUTPUT :
-  compa_bati


@author Anne Bernabe
Creation : 24/04/2013
Last modification :24/04/2013
Details : Update Intersection calculation 

 ---------------------------------------------------------------------------------------------------
 */
 
create table  compa_bati1 as select id, sum(st_length(the_geom)* hauteur)/sum(st_area(the_geom)* hauteur) as compa_bati, 
cast(sum(st_length(the_geom)* hauteur)/sum(st_area(the_geom)* hauteur)/0.15 as integer)+1 as id_compa_bati from intersection_bati  where st_area(the_geom)!=0 and hauteur !=0 group by id ;
create table compa_bati as select a.the_geom, a.id, b.compa_bati, b.id_compa_bati from grille a left join  compa_bati1 b on a.id= b.id;

drop table compa_bati1 purge;

/* NOTICE : VIEW SCALE : 


1 :    0   < compa_bati < 0.15
2 :   0.15 < compa_bati < 0.3
3 :   0.3 < compa_bati < 0.45
4 :  0.45 < compa_bati < 0.6
5 :  0.6 < compa_bati < 0.75
*/
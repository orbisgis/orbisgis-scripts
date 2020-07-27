/* SCRIPT SQL : MEAN HEIGHT OF BUILDINGS
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the average height of buildings
h_moy = Sum (hauteur)/ nb_bati


INPUT :
- intersection_bati
- grille

OUTPUT :
- h_moy


@author Anne Bernabé
Creation : 24/04/2013
Last modification : 07/06/2013
Details : Comments and test
 ---------------------------------------------------------------------------------------------------
 */

create table  h_moy1 as select id, sum(hauteur)/count(the_geom) as h_moy, 
cast(sum(hauteur)/count(the_geom)/6 as integer)+1 as id_h_moy from intersection_bati group by id;
create table h_moy as select a.the_geom, a.id, b.h_moy, b.id_h_moy from grille a left join  h_moy1 b on a.id= b.id;


drop table h_moy1 purge;

/* NOTICE : VIEW SCALE : 

Height in meter
1 :   0 < h_moy < 6
2 :  6 < h_moy < 12
3 :  12  < h_moy < 18
4 : 18 < h_moy < 24
5 :  24<h_moy > 30
other h_moy>30
*/

/* SCRIPT SQL : NUMBER OF BUILDING 
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the ratio of the built-up area and the surface of the mesh
nb_bati_moy = nb_bati/s_maille


INPUT :
- intersection_bati
- grille

OUTPUT :
- nb_bati_moy


@author Anne Bernabé
Creation : 24/04/2013
Last modification :08/06/2013
Details : Scale

 ---------------------------------------------------------------------------------------------------
 */


create table nb_bati as select id as id, count(the_geom) as nb_bati from intersection_bati group by id;
create table nb_bati_moy as select a.the_geom, a.id as id, st_area(a.the_geom)as s_maille, b.nb_bati, b.nb_bati/st_area(a.the_geom)as nb_bati_moy,
cast(b.nb_bati/st_area(a.the_geom)*10000/3 as integer)+1 as id_nb_bati from grille a left join  nb_bati b on a.id= b.id;
drop table nb_bati purge;

/* NOTICE : VIEW SCALE : 

NB of buildings/ha
1 :  0 < nb_bati < 3
2 :  3 < nb_bati < 6
3 :  6  < nb_bati < 9
4 :  9 < nb_bati < 12
5 : 12 < nb_bati <15
*/
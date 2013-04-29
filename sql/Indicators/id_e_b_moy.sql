/* SCRIPT SQL : AVERAGE DISTANCE BETWEEN BUILDINGS
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the average distance between two buildings
e_b_moy =  ( p_vide/2 - sqrt( p_vide²/4 - 4*s_vide ) )/2;
p_vide = p_maille + p_bati  - intersection (maille/bati)
s_vide = s_maille - s_bati



INPUT :
- intersection_bati
- grille

OUTPUT :
-  e_b_moy


@author Anne Bernabe
Creation : 24/04/2013
Last modification : 09/06/2013
Details : view scale

 ---------------------------------------------------------------------------------------------------
 */
 -- Building parameters
 create table  bati1 as select id, ST_Union(the_geom) AS the_geom, sum(st_length(the_geom)) as p_bati,  sum(st_area(the_geom)) as s_bati from intersection_bati group by id;


-- Empty space parameters 
create table e_b_moy as select a.the_geom, a.id, st_length(a.the_geom)as p_maille, st_area(a.the_geom) as s_maille, b.p_bati, b.s_bati, 
st_length(a.the_geom)+b.p_bati- 2*st_length(st_intersection(st_tomultiline(a.the_geom),st_tomultiline(b.the_geom))) as p_vide,
st_area(a.the_geom)-s_bati as s_vide
from grille a left join  bati1 b on a.id= b.id;


-- Average distance between buildings	
alter table e_b_moy add column e_b_moy double;
alter table e_b_moy add column id_e_b_moy integer;
update e_b_moy set e_b_moy = (p_vide/2-((p_vide^2/4-4*s_vide)^(1/2)))/2, 
id_e_b_moy= cast((p_vide/2-((p_vide^2/4-4*s_vide)^(1/2)))/2/25 as integer)+1;

drop table bati1 purge;


/* NOTICE : VIEW SCALE : 

average distance between buildings [m]
1 :  0     < e_b_moy < 25
2 :  25   < e_b_moy < 50
3 :  50   < e_b_moy < 75 
4 :  75   < e_b_moy < 100
5 :  100 < e_b_moy < 125
*/
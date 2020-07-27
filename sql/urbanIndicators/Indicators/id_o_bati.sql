/* SCRIPT SQL : BUILDING MEAN ORIENTIAON
WARNING :  ST_AZIMUT calculate orientation compare to east-west axe 

Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

calculates the average orientation of buildings



INPUT :
- intersection_bati
- grille

OUTPUT :
- o_bati


@author Anne Bernabé
Creation : 24/04/2013
Last modification : 07/06/2013
Details : Comments and test
 ---------------------------------------------------------------------------------------------------
 */
--minimum rectangle
create table polygons as select ST_MinimumRectangle(the_geom) as the_geom, id, ST_area(the_geom) as s_bati from intersection_bati;

-- minimun diameter and building o_bati
create table lines as select ST_MinimumDiameter(the_geom) as the_geom, id, s_bati from polygons;
alter table lines add column o_bati double;
alter table lines add column o_bati_ponderee double;

update lines set o_bati=ToDegrees(ST_AZIMUT(ST_STARTPOINT(the_geom), ST_ENDPOINT(the_geom))); 
update lines set o_bati_ponderee=o_bati*s_bati; 

--WARNING :  ST_AZIMUT calculate orientation compare to east-west axe 
--update lines set o_bati=ToDegrees(ST_AZIMUT(ST_STARTPOINT(the_geom) +90 , ST_ENDPOINT(the_geom))); 

-- at scale of the mesh
create table o_bati1 as select id, abs(sum(o_bati_ponderee)/sum(s_bati)) as o_bati, cast(abs(sum(o_bati_ponderee)/sum(s_bati))/22.5 as integer) +1 as id_o_bati
from lines  group by id;
create table o_bati as select a.the_geom, a.id as id, b.o_bati, b.id_o_bati from grille a left join  o_bati1 b on a.id= b.id;


-- Drop table

drop table polygons purge;
drop table lines purge;
drop table o_bati1 purge;

/* NOTICE : VIEW SCALE : 

Height in meter
1 :  0    < o_bati < 22.5
2 :  22.5 < o_bati < 45
3 :  45   < o_bati < 67.5
4 :  67.5 < o_bati <
4 :  90   < o_bati < 112.5
5 :  112.5< o_bati < 135
6:   135  < o_bati < 152.5
7 :  152.5< o_bati < 180
*/

/* SCRIPT SQL : CREATE GRID 50 x 50 FROM STUDY AREA
 Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :

Create grid envelop from study area



INPUT 
-study area


OUTPUT :
- grille

@author Anne Bernabe
Creation :  09//06/2013
Last modification : 09/06/2013
Details : drop table

 ---------------------------------------------------------------------------------------------------
 */


create table env as select ST_envelope(the_geom) from study_area;
create table grille as select * from ST_CreateGrid(env,50,50);
drop table env purge;
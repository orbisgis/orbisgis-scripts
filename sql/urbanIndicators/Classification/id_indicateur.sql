/* SCRIPT SQL : CREATE INDICATORS FOR MORPHOLOGICAL CLASSIFICATION
Licence : 

OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.

Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)

This file is part of OrbisGIS.
---------------------------------------------------------------------------------------------------
DESCRIPTION :
Global morphological analysis



INPUT 
- intersection_bati
- intersection_route
- intersection_veg
- intersection_hydro
- grille

OUTPUT :
- resultats
- indicateur
+ table of results cvs store in '/tmp'




@author Anne Bernabe
Creation :  10/06/2013
Last modification : 12/06/2013

 ---------------------------------------------------------------------------------------------------
 */

-- 1- Preparation of indicators
-- (a) On grid
alter table grille add column  s_maille double;
alter table grille add column  p_maille double;
update grille set p_maille= st_length(the_geom), s_maille= st_area(the_geom);


-- (b) On building
-- calculations preparation scale of buildings
alter table intersection_bati add column  p_bati double;
alter table intersection_bati add column  s_bati double;
alter table intersection_bati add column  v_bati double;
alter table intersection_bati add column  compa_bati double;
alter table intersection_bati add column  ic_bati double;
alter table intersection_bati add column orientation double;
update intersection_bati set p_bati= st_length(the_geom), 
s_bati= st_area(the_geom), 
v_bati= st_area(the_geom)*hauteur, 
compa_bati= st_length(the_geom)*hauteur/(st_area(the_geom)*hauteur), 
ic_bati=st_length(the_geom)*hauteur/(st_area(the_geom)*hauteur)^(2/3) 
where (st_area(the_geom)*hauteur)!=0;


-- orientation 
-- minimum rectangle
create table polygons as select ST_MinimumRectangle(the_geom) as the_geom, id, s_bati from intersection_bati;
-- minimun diameter and building orientation
create table  lines as select ST_MinimumDiameter(the_geom) as the_geom, id, s_bati from polygons;
alter table lines add column orientation double;
alter table lines add column orientation_ponderee double;
update lines set orientation=ToDegrees(ST_AZIMUT(ST_STARTPOINT(the_geom), ST_ENDPOINT(the_geom))); 
update lines set orientation_ponderee=orientation*s_bati; 

-- at scale of district
create table orientation as select id, abs(sum(orientation_ponderee)/sum(s_bati)) as orientation
from lines  group by id;

-- (c) On roads
-- calculations preparation
alter table intersection_route add column  s_route double;
alter table intersection_route add column  p_route double;
update  intersection_route set s_route= st_area(the_geom), p_route= st_length(the_geom);

-- (d) On vegetation
-- calculations preparation
alter table intersection_veg add column  s_veg double;
alter table intersection_veg add column  p_veg double;
update  intersection_veg set s_veg= st_area(the_geom), p_veg= st_length(the_geom);

-- (e) On water surface
-- calculations preparation
alter table intersection_hydro add column  s_eau double;
alter table intersection_hydro add column  p_eau double;
update  intersection_hydro set s_eau= st_area(the_geom), p_eau= st_length(the_geom);


-- 2- Creation of table of results
-- (a) Indicators relate to buildings at scale of urban island
create table indicateur_bati as select id,
st_union(the_geom) as the_geom,
count(the_geom) as nb_bati,
sum(p_bati) as p_bati,
sum(s_bati) as s_bati,
sum(v_bati) as v_bati,
sum(hauteur*s_bati)/sum(s_bati) as h_moy, --id6
sum(p_bati)/count(the_geom) as p_moy, --id7
sum(v_bati)/count(the_geom) as v_moy, --id9
sum(p_bati*hauteur)/sum(v_bati) as compa_bati, --id4
StandardDeviation(hauteur) as s_hati
from intersection_bati where s_bati!=0 and v_bati!=0 group by id; 

--  (b) indicators relate to roads at scale of urban island
create table indicateur_route1 as select id,
st_union(the_geom) as the_geom,
sum(p_route) as p_route,
sum(s_route) as s_route
from intersection_route group by id; 

create table indicateur_route2 as select a.id,
a.the_geom as the_geom, b.p_route, b.s_route
from grille a LEFT JOIN indicateur_route1 b on a.id = b.id;


create table indicateur_route as select a.id,
a.the_geom, a.p_route, a.s_route 
from indicateur_route2 a LEFT JOIN indicateur_route2 b on a.id = b.id; 

--  (c) indicators relate vegetation at scale of urban island 
create table indicateur_veg as select id,
st_union(the_geom) as the_geom,
sum(p_veg) as p_veg,
sum(s_veg) as s_veg
from intersection_veg group by id;

--  (d) indicators relate water surface at scale of urban island
create table indicateur_hydro as select id,
st_union(the_geom) as the_geom,
sum(p_eau) as p_eau,
sum(s_eau) as s_eau
from intersection_hydro group by id;


-- (e) create table of result resultats
create table resu_1 as select a.*, 
a.p_maille +b.p_bati -2*st_length(st_intersection(st_tomultiline(a.the_geom),st_tomultiline(b.the_geom))) as p_vide,
a.s_maille-b.p_bati as s_vide,
b.nb_bati, b.p_bati, b.s_bati, b.v_bati, b.h_moy, b.p_moy, b.v_moy, b.compa_bati, s_hati     
from grille a left join indicateur_bati b on a.id=b.id;
create table resu_2 as select a.*,b.orientation as o_bati from resu_1 a left join orientation b on a.id=b.id;
create table resu_3 as select a.*,b.p_route, b.s_route from resu_2 a left join indicateur_route b on a.id=b.id;
create table resu_4 as select a.*, b.p_eau, b.s_eau from resu_3 a left join indicateur_hydro b on a.id=b.id;
create table resultats as select a.*, b.p_veg, b.s_veg from resu_4 a left join indicateur_veg b on a.id=b.id;


-- 3- Indicator calculations
-- add colums 
alter table resultats add column nb_bati_moy double;
alter table resultats add column d_bati double;
alter table resultats add column d_veg double;
alter table resultats add column d_route double;
alter table resultats add column d_eau double;
alter table resultats add column e_moy double;
alter table resultats add column ces double;
alter table resultats add column cos double;
alter table resultats add column fsi double;
alter table resultats add column gsi double;
alter table resultats add column osr double;
alter table resultats add column l integer;
alter table resultats add column n double;
alter table resultats add column m double;
alter table resultats add column h_w double;

-- update
update resultats set 
d_bati = s_bati/s_maille,
d_veg = s_veg/s_maille,
d_route = s_route/s_maille,
d_eau = s_eau/s_maille,
e_moy= (p_vide/2-((p_vide^2/4 - 4*s_vide)^(1/2)))/2,
ces=  s_bati/s_maille,
nb_bati_moy=nb_bati/s_maille,
fsi= v_bati/s_maille,
gsi= s_bati/s_maille,
osr= s_vide/s_maille,
l= cast(h_moy/3 as integer),
m= 2*log(nb_bati)/log(s_maille/p_maille*p_bati/s_bati) where p_vide^2/4 - 4*s_vide >0;

-- Replace null value for csv format
UPDATE resultats SET 
p_vide=0, s_vide=0 WHERE s_vide IS NULL;

update resultats set
cos = (l*s_bati)/s_maille,
h_w= h_moy/e_moy;

UPDATE resultats SET s_veg=0, p_veg=0, d_veg=0 where d_veg IS NULL;
UPDATE resultats SET p_route=0, s_route=0, d_route=0  WHERE d_route IS NULL;
UPDATE resultats SET p_eau=0, s_eau=0, d_eau=0  WHERE d_eau IS NULL;


UPDATE resultats SET 
nb_bati=0, p_bati=0, v_bati=0, s_bati =0, compa_bati=0,
s_hati=0, o_bati=0, nb_bati_moy=0, d_bati=0,p_moy=0,h_moy=0, v_moy=0, 
e_moy=0, l=0, cos=0,osr=1, gsi=0, fsi=0, h_w=0, ces=0, m=0 WHERE nb_bati_moy IS NULL;
UPDATE resultats SET m=0 WHERE m IS NULL;


--4 -Indicators of vizualisation
create table indicateur as select id, 
the_geom, 
cast(h_moy/6 as integer) +1 as h_moy, 
cast(p_moy/50 as integer) +1 as p_moy ,
cast(v_moy/1500 as integer) +1  as v_moy ,
cast(compa_bati/0.15 as integer) +1 as compa_bati ,  
cast(d_bati/0.15 as integer) +1 as d_bati , 
cast(d_veg/0.2 as integer) +1 as d_veg ,
cast(d_eau/0.1 as integer) +1 as d_eau , 
cast(d_route/0.15 as integer) +1 as d_route ,
cast (nb_bati_moy*10000/3 as integer) +1  as nb_bati_moy ,
cast (e_moy/25 as integer) +1 as e_moy,
cast(o_bati/22.5 as integer)+1 as o_bati, 
cast(s_hati/3 as integer)+1 as s_hati,
cast(m as integer)+1 as m,
cast(l as integer)+1 as l, 
cast(h_w/0.25 as integer) +1 as h_w,
cast(osr/0.2 as integer) +1 as osr,
cast(fsi as integer) +1 as fsi from resultats ; 

-- 5 - Post process
--Cas0
create table cas0 as select id, 
h_moy, p_moy ,
v_moy ,
compa_bati ,  
d_bati , 
d_veg , 
d_eau,
d_route ,
nb_bati_moy ,
e_moy,
o_bati, 
s_hati,
m,
l, 
h_w,
osr,
fsi
from resultats where d_bati!=0;
execute export( cas0, '/tmp/cas0.csv');

--cas1
create table cas1 as select id, 
h_moy, p_moy ,
v_moy ,
compa_bati ,  
d_bati , 
d_veg , 
d_eau,
d_route ,
nb_bati_moy ,
e_moy
from resultats where d_bati!=0;
execute export( cas1, '/tmp/cas1.csv');

--Cas 2
create table cas2 as select id, 
h_moy ,d_bati 
from resultats where d_bati!=0;
execute export( cas2, '/tmp/cas2.csv');
-- Cas 3
create table cas3 as select id, 
h_moy ,d_bati, cos
from resultats where d_bati!=0;
execute export(cas3, '/tmp/cas3.csv');

--Cas4
create table cas4 as select id, 
h_moy ,d_bati, d_veg
from resultats where d_bati!=0;
execute export(cas4, '/tmp/cas4.csv');

--Cas5
create table cas5 as select id, 
h_moy ,d_bati, s_hati
from resultats where d_bati!=0;
execute export(cas5, '/tmp/cas5.csv');

--Cas6
create table cas6 as select id, 
h_moy ,d_bati, e_moy
from resultats where d_bati!=0;
execute export(cas6, '/tmp/cas6.csv');

--Cas7
create table cas7 as select id, 
h_w ,d_bati
from resultats where d_bati!=0;
execute export(cas7, '/tmp/cas7.csv');

--Cas8
create table cas8 as select id, 
h_moy ,d_bati, m
from resultats where d_bati!=0;
execute export(cas8, '/tmp/cas8.csv');


--Cas9
create table cas9 as select id, 
h_moy ,d_bati, o_bati
from resultats where d_bati!=0;
execute export(cas9, '/tmp/cas9.csv');


--Cas10
create table cas10 as select id, 
fsi,osr,gsi,l
from resultats where d_bati!=0;
execute export( cas10, '/tmp/cas10.csv');


--Cas11
create table cas11 as select id, 
ces,cos
from resultats where d_bati!=0;
execute export( cas11, '/tmp/cas11.csv');


--Cas12
create table cas12 as select id, 
d_bati,h_moy,nb_bati_moy 
from resultats where d_bati!=0;
execute export( cas12, '/tmp/cas12.csv');


-- 6 -Drop table 
drop table indicateur_bati purge ;
drop table indicateur_route purge ;
drop table indicateur_route1 purge ;
drop table indicateur_route2 purge ;
drop table indicateur_veg purge ;
drop table indicateur_hydro purge;
drop table orientation purge;
drop table polygons purge;
drop table lines purge;
drop table resu_1 purge;
drop table resu_2 purge;
drop table resu_3 purge;
drop table resu_4 purge;
drop table cas0 purge;
drop table cas1 purge;
drop table cas2 purge;
drop table cas3 purge;
drop table cas4 purge;
drop table cas5 purge;
drop table cas6 purge;
drop table cas7 purge;
drop table cas8 purge;
drop table cas9 purge;
drop table cas10 purge;
drop table cas11 purge;
drop table cas12 purge;
drop table intersection_bati purge;
drop table intersection_route purge;
drop table intersection_veg purge;
drop table intersection_hydro purge;


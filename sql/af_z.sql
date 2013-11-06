/** SCRIPT BeanShell : Frontal density
LICENCE : 

Orbisgis is distributed under gpl 3 license. it is produced by the "atelier sig"
team of the irstv institute <http://www.irstv.fr/> cnrs fr 2488.

copyright (c) 2007-2012 irstv (fr cnrs 2488)

This file is part of orbisgis.
---------------------------------------------------------------------------------------------------

Input : 
- building : bati 
- grid : grille
- Maximum heigth : h_max
- Wind direction : alpha

Output :
-  frontal_density(alpha,z)


@author Anne Bernabe
creation : 03/06/2013
last modification : 03/06/2013
details : 

 ---------------------------------------------------------------------------------------------------


*/

/** 1- Preprocess 
-- Cut building layer with grid boundary */

sql("create table intersection_bati_join as select  st_intersection(b.the_geom,a.the_geom) as the_geom, a.id, b.hauteur from grille a, bati b where st_isvalid(b.the_geom)  and st_intersects(a.the_geom,b.the_geom);");


/** Explode */
sql("create table intersection_bati as select *, ST_area(the_geom) as s_bati from st_explode(intersection_bati_join);");


/** Define  maximum heigth */
h_max=3;

/** Define direction alpha in degree  */
alpha= 45;
	
/** Direction alpha in radiant*/
b=Math.toRadians(alpha);
	
bati_alpha= "bati_"+alpha;
		
sql("CREATE TABLE "+ bati_alpha  +" as select ST_ROTATE(the_geom, " + b +") as the_geom, id, hauteur, s_bati,  ST_Xmax(ST_ROTATE(the_geom, " + b +"))-ST_XMin(ST_ROTATE(the_geom, " + b +")) as lf from intersection_bati;");
	
/** 2- Loop in vertical direction by step of 1 m*/
/**Initialisation of af */
sql("CREATE TABLE af0 as select ST_area(the_geom) as s_maille, id from grille;");
	
/**Loop in vertical direction*/
for (i=1; i<h_max+1; i++){
	n=i-1;
	lfi = "lf"+i;
	afn = "af"+n;
	afi = "af"+i;

	/** Frontal length at scale of grid mesh heigth > i */
	sql("CREATE TABLE " + lfi + " as select id, sum(lf) as lf, sum(s_bati) as s_bati from " + bati_alpha  + " where hauteur>" + i + " group by id;");
		
	/**Frontal density at scale of grid mesh */
	sql("CREATE TABLE " + afi + " as select a.*, b.lf/(a.s_maille-s_bati) as "+ afi +" from " + afn + " a left join " + lfi + " b on a.id=b.id;");
	sql("update " + afi + " set " + afi + " =0 where "+ afi +" IS NULL;");
	};
/** Frontal density table */
densite_alpha= "densite_frontale_"+alpha;
afmax= "af"+h_max;
sql("CREATE TABLE "+ densite_alpha +" as select a.the_geom, b.* from grille a left join " + afmax + " b on a.id=b.id;");

/**Cleaning loop*/
for (i=1; i<h_max+1; i++){
	n=i-1;
	afi= "af"+i;
	lfi = "lf"+i;
	sql("DROP TABLE " + afi + " purge;");
	sql("DROP TABLE " + lfi + " purge;");
};

sql("DROP TABLE " + bati_alpha  + " purge;");
sql("DROP TABLE af0 purge;");
sql("DROP TABLE intersection_bati_join purge;");
sql("DROP TABLE intersection_bati purge;");

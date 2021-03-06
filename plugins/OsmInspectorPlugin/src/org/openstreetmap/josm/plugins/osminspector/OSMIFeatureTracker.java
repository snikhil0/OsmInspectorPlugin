package org.openstreetmap.josm.plugins.osminspector;

import java.util.HashMap;
import java.util.Iterator;

import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class OSMIFeatureTracker 
{

	private HashMap< Long, SimpleFeature > hashFeatures;
	private MemoryFeatureCollection features;

	public OSMIFeatureTracker( FeatureCollection<SimpleFeatureType, SimpleFeature> featuresIn ) 
	{
		hashFeatures 	= new HashMap();
		features		= new MemoryFeatureCollection( featuresIn.getSchema() );
		
		for( Iterator it = features.iterator(); it.hasNext(); )
		{
			SimpleFeature element = (SimpleFeature) it.next();
			Long ID = ( Long.parseLong( (String) element.getAttribute( "problem_id" ) ) );
			hashFeatures.put( ID, element );
		}
		
		features.addAll( featuresIn );
	}

	public boolean mergeFeatures( FeatureCollection<SimpleFeatureType, SimpleFeature> newFeatures )
	{
		for( Iterator it = newFeatures.iterator(); it.hasNext(); )
		{
			SimpleFeature element = (SimpleFeature) it.next();
			Long ID = ( Long.parseLong( (String) element.getAttribute( "problem_id" ) ) );
			
			if( ! hashFeatures.containsKey(ID ) )
			{
				hashFeatures.put( ID, element );
				features.add( element );
			}
		}
		
		return true;
	}
	
	public HashMap< Long, SimpleFeature > getFeatureHash()
	{
		return hashFeatures;
	}

	public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatures()
	{
		return features;
	}
}

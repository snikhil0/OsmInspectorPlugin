package org.openstreetmap.josm.plugins.osminspector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

public class GeoFabrikWFSClient {

	private final Bounds bbox;
	private DataStore data;
	
	public GeoFabrikWFSClient(Bounds bounds) {
		bbox = bounds;
	}

	public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatures()
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		String getCapabilities = "http://tools.geofabrik.de/osmi/view/routing_non_eu/wxs?SERVICE=WFS&VERSION=1.0.0&REQUEST=GetCapabilities";
		Map connectionParameters = new HashMap();
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL",
				getCapabilities);
		connectionParameters.put("WFSDataStoreFactory:WFS_STRATEGY",
				"mapserver");
		connectionParameters.put("WFSDataStoreFactory:LENIENT", true);
		connectionParameters.put("WFSDataStoreFactory:TIMEOUT", 20000);
		connectionParameters.put("WFSDataStoreFactory:BUFFER_SIZE", 10000);
		// Step 2 - connection
		data = DataStoreFinder.getDataStore(connectionParameters);
		
		// Step 3 - discovery; enhance to iterate over all types with bounds
		String typeNames[] = data.getTypeNames();
		String typeName = typeNames[1];
		SimpleFeatureType schema = data.getSchema(typeName);
		// Step 4 - target
		FeatureSource<SimpleFeatureType, SimpleFeature> source = data
				.getFeatureSource(typeName);
		System.out.println("Source Metadata Bounds:" + source.getBounds());
		System.out.println("Source schema: " + source.getSchema());
		
		// Step 5 - query
		List<AttributeDescriptor> listAttrs = schema.getAttributeDescriptors();
		String geomName = listAttrs.get(0).getLocalName();
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4236");
				
		LatLon minLL = bbox.getMin();
		LatLon maxLL = bbox.getMax();
		double minLat = Math.min(minLL.getY(), maxLL.getY());
		double maxLat = Math.max(minLL.getY(), maxLL.getY());
		double minLon = Math.min(minLL.getX(), maxLL.getX());
		double maxLon = Math.max(minLL.getX(), maxLL.getX());
		
		
		ReferencedEnvelope bboxRef = new ReferencedEnvelope(minLon, maxLon, minLat, maxLat, targetCRS);
		System.out.println("Reference Bounds:" + bboxRef);
		
		//
		// Ask WFS service for typeName data constrained by bboxRef
		//
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools

				.getDefaultHints());
		Filter filterBB = ff.bbox(ff.property(geomName), bboxRef);
		FeatureCollection<SimpleFeatureType, SimpleFeature> features = source
				.getFeatures(filterBB);

		return features;
		//
		// As features are iterated, construct an error icon much like what OSMI
		// currently uses, and styled similarly
		// Bonus points if JOSM auto triggers a data load based on the BBox.
		// Might be best left to user...
		//
		// See
		// https://github.com/iandees/josm-shapefile/blob/master/src/main/java/com/yellowbkpk/geo/shapefile/ShapefileLayer.java
		// for rendering ideas
		//
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			//CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
			GeoFabrikWFSClient theTest = new GeoFabrikWFSClient(
					new Bounds(-124.0, -120.0, 32.0, 36.0));
			FeatureCollection<SimpleFeatureType, SimpleFeature> features = theTest
					.getFeatures();
			OsmInspectorLayer inspector = new OsmInspectorLayer(
					theTest.getData());
			inspector.setVisible(true);
			
			ReferencedEnvelope bounds = new ReferencedEnvelope();
			Iterator<SimpleFeature> iterator = features.iterator();
			try {
				while (iterator.hasNext()) {
					Feature feature = (Feature) iterator.next();
					bounds.include(feature.getBounds());
				}
				System.out.println("Calculated Bounds:" + bounds);
			} finally {
				features.close(iterator);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public DataStore getData() {
		return data;
	}

	public void setData(DataStore data) {
		this.data = data;
	}

}

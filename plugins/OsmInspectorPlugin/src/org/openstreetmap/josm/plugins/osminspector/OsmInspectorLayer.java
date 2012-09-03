package org.openstreetmap.josm.plugins.osminspector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.lang.time.StopWatch;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.osminspector.gui.OsmInspectorDialog;
import org.openstreetmap.josm.tools.ImageProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

@SuppressWarnings("deprecation")
public class OsmInspectorLayer extends Layer {

	private StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
	private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
	private StreamingRenderer renderer;
	private CoordinateReferenceSystem crs;
	private CoordinateReferenceSystem crsOSMI;
	private GeomType geometryType;
	private String geometryAttributeName;

	private SimpleFeatureSource featureSource;
	private MapContext context;
	private boolean bIsChanged;

	/**
	 * dialog showing the bug info
	 */
	private OsmInspectorDialog dialog;

	/**
	 * supported actions
	 */


	// Container for bugs from Osmi
	private ArrayList<OSMIFeatureTracker> arrFeatures;
	private ArrayList<SimpleFeature> bugsInScene;
	private LinkedList<BugInfo> osmiBugInfo;

	public SimpleFeatureSource getFeatureSource() {
		return featureSource;
	}

	public void setFeatureSource(SimpleFeatureSource featureSource) {
		this.featureSource = featureSource;
	}

	public boolean isbIsChanged() {
		return bIsChanged;
	}

	public void setbIsChanged(boolean bIsChanged) {
		this.bIsChanged = bIsChanged;
	}

	public ArrayList<OSMIFeatureTracker> getArrFeatures() {
		return arrFeatures;
	}

	public void setArrFeatures(ArrayList<OSMIFeatureTracker> arrFeatures) {
		this.arrFeatures = arrFeatures;
	}

	public ArrayList<SimpleFeature> getBugsInScene() {
		return bugsInScene;
	}

	public void setBugsInScene(ArrayList<SimpleFeature> bugsInScene) {
		this.bugsInScene = bugsInScene;
	}

	public LinkedList<BugInfo> getOsmiBugInfo() {
		return osmiBugInfo;
	}

	public void setOsmiBugInfo(LinkedList<BugInfo> osmiBugInfo) {
		this.osmiBugInfo = osmiBugInfo;
	}

	public BugIndex getOsmiIndex() {
		return osmiIndex;
	}

	public void setOsmiIndex(BugIndex osmiIndex) {
		this.osmiIndex = osmiIndex;
	}

	// Pointer to prev and next osmi bugs
	private BugIndex osmiIndex;

	/**
	 * Helper class that stores the bug next and prev pointers and can navigate
	 * the entire bug list
	 * 
	 * @author snikhil
	 * 
	 */
	public class BugIndex {
		private int nextIndex;
		private int previousIndex;
		private LinkedList<BugInfo> osmBugs;

		public BugIndex(LinkedList<BugInfo> bugs) {
			osmBugs = bugs;
			nextIndex = 0;
			previousIndex = bugs.size() - 1;
		}

		public void next() {
			previousIndex = nextIndex;
			nextIndex = ++nextIndex % osmBugs.size();
		}

		public void prev() {
			nextIndex = previousIndex;
			previousIndex = --previousIndex < 0 ? osmBugs.size() - 1
					: previousIndex;
		}

		public BugInfo getItemPointedByNext() {
			return osmBugs.get(nextIndex);
		}

		public BugInfo getItemPointedByPrev() {
			return osmBugs.get(previousIndex);
		}

		public BugInfo getNext() {
			next();
			return osmBugs.get(nextIndex);
		}

		public BugInfo getPrev() {
			prev();
			return osmBugs.get(nextIndex);
		}

	}

	private enum GeomType {
		POINT, LINE, POLYGON
	};

	private static final Color LINE_COLOUR = Color.BLUE;
	private static final Color FILL_COLOUR = Color.CYAN;
	private static final float OPACITY = 1.0f;
	private static final float LINE_WIDTH = 1.0f;
	private static final float POINT_SIZE = 10.0f;

	/**
	 * 
	 * @param wfsClient
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 * @throws IOException
	 * @throws IndexOutOfBoundsException
	 * @throws ParseException
	 */
	public OsmInspectorLayer(GeoFabrikWFSClient wfsClient)
			throws NoSuchAuthorityCodeException, FactoryException, IOException,
			IndexOutOfBoundsException, ParseException {
		super("OsmInspector");
		
		arrFeatures = new ArrayList<OSMIFeatureTracker>();
		osmiBugInfo = new LinkedList<OsmInspectorLayer.BugInfo>();
		bugsInScene = new ArrayList<SimpleFeature>();
		
		// Step 3 - discovery; enhance to iterate over all types with bounds

		String typeNames[] = wfsClient.getTypeNames();
		renderer = new StreamingRenderer();
		crs = CRS.decode(Main.getProjection().toCode());
		crsOSMI = CRS.decode("EPSG:4326");
		context = new DefaultMapContext(crsOSMI);

		for (int idx = 1; idx < typeNames.length; ++idx) {
			String typeName = typeNames[idx];

			FeatureCollection<SimpleFeatureType, SimpleFeature> features = wfsClient
					.getFeatures(typeName);
			setGeometry(typeName);

			System.out.println("Osm Inspector Features size: "
					+ features.size());
			Style style = createDefaultStyle();

			OSMIFeatureTracker tracker = new OSMIFeatureTracker(features);
			arrFeatures.add(tracker);
			FeatureIterator<SimpleFeature> it = tracker.getFeatures()
					.features();
			while (it.hasNext()) {
				bugsInScene.add(it.next());
			}
			context.addLayer(tracker.getFeatures(), style);
		}

		Iterator<SimpleFeature> sfi = bugsInScene.iterator();
		while (sfi.hasNext()) {
			osmiBugInfo.add(new BugInfo(sfi.next()));
		}
		osmiIndex = new BugIndex(osmiBugInfo);

		context.setTitle("Osm Inspector Errors");
		renderer.setContext(context);
		bIsChanged = true;
		
		// finally initialize the dialog
		dialog = new OsmInspectorDialog(this);
		
	}

	/**
	 * 
	 * @param wfsClient
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 * @throws IOException
	 */
	public void loadFeatures(GeoFabrikWFSClient wfsClient)
			throws NoSuchAuthorityCodeException, FactoryException, IOException {
		String typeNames[] = wfsClient.getTypeNames();
		int layerOffset = 1;

		context.clearLayerList();

		for (int idx = 1; idx < typeNames.length; ++idx) {
			String typeName = typeNames[idx];

			FeatureCollection<SimpleFeatureType, SimpleFeature> features = wfsClient
					.getFeatures(typeName);
			setGeometry(typeName);

			System.out.println("Osm Inspector Features size: "
					+ features.size());

			OSMIFeatureTracker tracker = arrFeatures.get(idx - layerOffset);
			tracker.mergeFeatures(features);

			FeatureIterator<SimpleFeature> it = tracker.getFeatures()
					.features();
			while (it.hasNext()) {
				bugsInScene.add(it.next());
			}

			Style style = createDefaultStyle();
			context.addLayer(tracker.getFeatures(), style);
		}

		bIsChanged = true;
	}

	private Style createDefaultStyle() {
		Rule rule = createRule(LINE_COLOUR, FILL_COLOUR);

		FeatureTypeStyle fts = sf.createFeatureTypeStyle();
		fts.rules().add(rule);

		Style style = sf.createStyle();
		style.featureTypeStyles().add(fts);
		return style;
	}

	private Rule createRule(Color outlineColor, Color fillColor) {
		Symbolizer symbolizer = null;
		Fill fill = null;
		Stroke stroke = sf.createStroke(ff.literal(outlineColor),
				ff.literal(LINE_WIDTH));

		switch (geometryType) {
		case POLYGON:
			fill = sf.createFill(ff.literal(fillColor), ff.literal(OPACITY));
			symbolizer = sf.createPolygonSymbolizer(stroke, fill,
					geometryAttributeName);
			break;

		case LINE:
			symbolizer = sf.createLineSymbolizer(stroke, geometryAttributeName);
			break;

		case POINT:
			fill = sf.createFill(ff.literal(fillColor), ff.literal(OPACITY));

			Mark mark = sf.getTriangleMark();
			mark.setFill(fill);
			mark.setStroke(stroke);

			Graphic graphic = sf.createDefaultGraphic();
			graphic.graphicalSymbols().clear();
			graphic.graphicalSymbols().add(mark);
			graphic.setSize(ff.literal(POINT_SIZE));

			symbolizer = sf.createPointSymbolizer(graphic,
					geometryAttributeName);
		}

		Rule rule = sf.createRule();
		rule.symbolizers().add(symbolizer);
		return rule;
	}

	private void setGeometry(String typename) {
		System.out.println("Passed type is" + typename);
		if (typename.compareTo("duplicate_ways") == 0) {
			geometryType = GeomType.LINE;
		} else
			geometryType = GeomType.POINT;
	}

	@Override
	public Icon getIcon() {
		return ImageProvider.get("layer/osmdata_small");
	}

	@Override
	public Object getInfoComponent() {
		return getToolTipText();
	}

	@Override
	public Action[] getMenuEntries() {
		return new Action[] {};
	}

	@Override
	public String getToolTipText() {
		return org.openstreetmap.josm.tools.I18n.tr("OsmInspector");
	}

	@Override
	public boolean isMergable(Layer other) {
		return false;
	}

	@Override
	public void mergeFrom(Layer from) {
		return;
	}

	@Override
	public void paint(Graphics2D g, MapView mv, Bounds box) {
		System.out.println("Rendering time...");
		StopWatch sw = new StopWatch();
		sw.start();

		LatLon min = box.getMin();
		LatLon max = box.getMax();

		Coordinate northWest = new Coordinate(max.getX(), min.getY());
		Coordinate southEast = new Coordinate(min.getX(), max.getY());

		EastNorth center = Main.map.mapView.getCenter();
		EastNorth leftop = Main.map.mapView.getEastNorth(0, 0);
		EastNorth rightbot = Main.map.mapView.getEastNorth(
				mv.getBounds().width, mv.getBounds().height);

		Envelope envelope = new Envelope(Math.min(leftop.east(),
				rightbot.east()), Math.max(leftop.east(), rightbot.east()),
				Math.min(rightbot.north(), leftop.north()), Math.max(
						rightbot.north(), leftop.north()));

		Envelope envelope2 = new Envelope(Math.min(min.lat(), max.lat()),
				Math.max(min.lat(), max.lat()), Math.min(min.lon(), max.lon()),
				Math.max(min.lon(), max.lon()));

		// ReferencedEnvelope mapArea = new ReferencedEnvelope(envelope, crs);
		ReferencedEnvelope mapArea = new ReferencedEnvelope(envelope2, crsOSMI);

		// System.out.println( "rendering bounds:" + envelope + " box " + box +
		// " maparea " + mapArea );

		renderer.setInteractive(false);
		renderer.paint(g, mv.getBounds(), mapArea);
		bIsChanged = false;
		sw.stop();
		System.out.println(sw.getTime());
	}

	@Override
	public void visitBoundingBox(BoundingXYVisitor v) {
	}

	public boolean isChanged() {
		return bIsChanged;
	}

	/**
	 * 
	 * The Bug attribute class: hold geom, id and description for that bug
	 * @author snikhil
	 *
	 */
	public class BugInfo {
		private final Geometry geom;
		private final String desc;
		private final String id;

		public BugInfo(SimpleFeature next) throws IndexOutOfBoundsException,
				ParseException {
			this.geom = (Geometry) next.getAttribute(0);
			this.desc = (String) next.getAttribute(7);
			this.id = next.getID();
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return String.valueOf(id).concat(" : ").concat(geom.toString())
					.concat(" : ").concat(desc);
		}
	}

}
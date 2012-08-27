package org.openstreetmap.josm.plugins.osminspector;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.tools.Shortcut;

public class ImportOsmInspectorBugsAction extends JosmAction {
	OsmInspectorPlugin plugin;
	/**
	 * 
	 */
	private static final long serialVersionUID = -6484182416189079287L;

	public ImportOsmInspectorBugsAction( OsmInspectorPlugin thePlugin ) {
		super(tr("Import Osm Inspector Bugs..."), "importosmibugs",
				tr("Import Osm Inspector Bugs..."), Shortcut.registerShortcut(
						"importosmibugs",
						tr("Edit: {10}", tr("Import Osm Inspector Bugs...")),
						KeyEvent.VK_O, Shortcut.ALT_CTRL), true);
		putValue("help", ht("/Action/ImportOsmInspectorBugs"));
		plugin = thePlugin;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if(isEnabled()) {
			System.out.println("enabled event...");
			try {
				CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
				Bounds bounds = Main.map.mapView.getLatLonBounds(Main.map.mapView.getBounds());
				
				System.out.println( "OSMI View bounds" + bounds );
				
				OsmInspectorLayer inspector = plugin.getLayer();
				
				if( inspector == null )
				{
					GeoFabrikWFSClient wfs = new GeoFabrikWFSClient(bounds);
					wfs.initializeDataStore();
					inspector = new OsmInspectorLayer( wfs );
					Main.main.addLayer( inspector );
					plugin.setLayer( inspector );
				}
				else
				{
					GeoFabrikWFSClient wfs = new GeoFabrikWFSClient(bounds);
					wfs.initializeDataStore();
					inspector.loadFeatures( wfs );
					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!isEnabled()) {
			System.out.println("Osm Inspector Action not enanbled");
			
		}
	}
}

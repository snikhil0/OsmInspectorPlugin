package org.openstreetmap.josm.plugins.osminspector.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.widgets.ListPopupMenu;
import org.openstreetmap.josm.plugins.osminspector.OsmInspectorLayer;
import org.openstreetmap.josm.plugins.osminspector.OsmInspectorLayer.BugInfo;
import org.openstreetmap.josm.tools.Shortcut;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class OsmInspectorDialog extends ToggleDialog implements
		ListSelectionListener, LayerChangeListener, MouseListener {

	private OsmInspectorLayer layer;
	private JList bugsList;
	private OsmInspectorNextAction actNext;
	private OsmInspectorPrevAction actPrev;
	private SelectionPopup popup;
	private DefaultListModel model;
	/**
	 * 
	 */
	private static final long serialVersionUID = 5465011236663660394L;

	/**
	 * The popup menu for the selection list
	 */
	class SelectionPopup extends ListPopupMenu {
		public SelectionPopup(JList list) {
			super(list);
		}
	}

	/**
	 * Builds the content panel for this dialog
	 */
	protected void buildContentPanel() {
		Main.map.addToggleDialog(this, true);
		
		model = new DefaultListModel();
		refreshModel();
		bugsList = new JList(model);
		bugsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bugsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		bugsList.setVisibleRowCount(-1);
		// refreshBugList();
		// the next action
		final SideButton nextButton = new SideButton(
				actNext = new OsmInspectorNextAction(layer));
		bugsList.getSelectionModel().addListSelectionListener(actNext);
		nextButton.createArrow(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = bugsList.getSelectedIndex();
				Geometry geom = layer.getOsmiBugInfo().get(index).getGeom();
				Point centroid = geom.getCentroid();
				LatLon center = new LatLon(centroid.getY(), centroid.getX());
				Main.map.mapView.zoomTo(center);
			}
		});

		// the search button
		final SideButton prevButton = new SideButton(
				actPrev = new OsmInspectorPrevAction(layer));
		prevButton.createArrow(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(e);
			}
		});

		Shortcut sprev = Shortcut.registerShortcut("prev", "Prev OSMI bug",
				KeyEvent.VK_J, Shortcut.CTRL_SHIFT);
		Main.registerActionShortcut(actPrev, sprev);

		Shortcut snext = Shortcut.registerShortcut("next", "Next OSMI bug",
				KeyEvent.VK_K, Shortcut.CTRL_SHIFT);
		Main.registerActionShortcut(actNext, snext);

		createLayout(bugsList, true,
				Arrays.asList(new SideButton[] { nextButton, prevButton }));
		this.add(bugsList);
	}

	private void refreshModel() {
		model.clear();
		for (BugInfo b : layer.getOsmiBugInfo()) {
			model.addElement(b.getDesc());
		}
		
	}

	public OsmInspectorDialog(OsmInspectorLayer layer) {

		super(tr("Osmi"), "selectionlist",
				tr("Open a OSM Inspector selection list window."), Shortcut
						.registerShortcut("subwindow:selection",
								tr("Toggle: {0}", tr("Current Selection")),
								KeyEvent.VK_W, Shortcut.ALT_SHIFT), 150, // default
																			// height
				true // default is "show dialog"
		);
		this.layer = layer;
		buildContentPanel();
	}

	@Override
	public void showNotify() {
		super.showNotify();
	}

	@Override
	public void hideNotify() {
		if (dialogsPanel != null) {
			// TODO Auto-generated method stub
			super.hideNotify();
		}
	}

	public class OsmInspectorNextAction extends AbstractAction implements
			ListSelectionListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = 123266015594117296L;
		private OsmInspectorLayer layer;

		public OsmInspectorNextAction(Layer inspector) {
			super("next");
			layer = (OsmInspectorLayer) inspector;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			layer.getOsmiIndex().next();
			BugInfo next = layer.getOsmiIndex().getItemPointedByNext();
			System.out.println(next);
			Geometry geom = next.getGeom();
			Point centroid = geom.getCentroid();
			LatLon center = new LatLon(centroid.getY(), centroid.getX());
			Main.map.mapView.zoomTo(center);

		}

		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			// TODO Auto-generated method stub

		}
	}

	private class OsmInspectorPrevAction extends AbstractAction implements
			ListSelectionListener {

		private OsmInspectorLayer layer;

		public OsmInspectorPrevAction(Layer inspector) {
			super("prev");
			layer = (OsmInspectorLayer) inspector;

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			layer.getOsmiIndex().prev();
			BugInfo prev = layer.getOsmiIndex().getItemPointedByPrev();
			System.out.println(prev);
			Geometry geom = prev.getGeom();
			Point centroid = geom.getCentroid();
			LatLon center = new LatLon(centroid.getY(), centroid.getX());
			Main.map.mapView.zoomTo(center);
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			// TODO Auto-generated method stub

		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		if(newLayer instanceof OsmInspectorLayer) {
			this.layer = (OsmInspectorLayer) newLayer;
			refreshModel();
			refreshBugList();
		}

	}

	private void refreshBugList() {
		bugsList.clearSelection();
		bugsList = new JList(model);
		
	}

	@Override
	public void layerAdded(Layer layer) {
		if(layer instanceof OsmInspectorLayer) {
			refreshModel();
			refreshBugList();
		}
	}

	@Override
	public void layerRemoved(Layer arg0) {
		if(layer instanceof OsmInspectorLayer) {
			bugsList.clearSelection();
			model.clear();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		System.out.println(e.getFirstIndex());
	}

}

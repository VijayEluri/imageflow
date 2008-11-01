package gui;

import graph.Node;
import graph.NodeText;

import ij.io.SaveDialog;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import models.unit.UnitDelegate;
import models.unit.UnitElement;
import visualap.Delegate;
import visualap.ErrorPrinter;
import visualap.GPanel;

public class InsertUnitMenu extends JMenu {

	private GPanel activePanel;
	private final ArrayList<Delegate> availableUnits;
	private static Point savedPoint = new Point(75, 75);
	
	public InsertUnitMenu(final GPanel gpanel, final ArrayList<Delegate> availableUnits) {
		this("Insert", gpanel, availableUnits, savedPoint);
	}
	
	public InsertUnitMenu(final String name, 
			final GPanel gpanel, 
			final ArrayList<Delegate> availableUnits, 
			final Point savedPoint) {
		this.setName(name);
		this.setText(name);
		this.activePanel = gpanel;
		this.availableUnits = availableUnits;
		ActionListener newAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JMenuItem source = (JMenuItem)(e.getSource());
				String action = source.getText();
				if (action.equals("Add Text")) {	
					// corrected fault: NodeText instead of NodeBean
					Node n = new NodeText(new Point(savedPoint), "text"); savedPoint.translate(4, 4);
					activePanel.getNodeL().add(n, "text$0");
					activePanel.getSelection().clear();
					activePanel.getSelection().add(n);
					activePanel.repaint();
					return;
				}

				// add selected node
				for (int i = 0; i < availableUnits.size(); i++) {
					UnitDelegate delegate = (UnitDelegate)availableUnits.get(i); 
					if (delegate.getName().equals(action)) {
						try {
							//							Object myBean = delegate.get(i).clazz.newInstance();
							UnitElement n = delegate.createUnit(savedPoint);
							n.setContext(activePanel.getGlobalVars());
							activePanel.getNodeL().add(n, activePanel.shortName(action));
							activePanel.getSelection().clear();
							activePanel.getSelection().add(n);
							activePanel.repaint();
						} catch (Exception ex) {
							ErrorPrinter.printInfo("instantiation of a new bean failed"+ ex);
						}
						return;
					}
				}
			}}; 
		JMenuItem mi = new JMenuItem("Add Text");
		mi.setToolTipText("Insert text");
		add(mi).addActionListener(newAction);		

		//list over all available units

		for (int i = 0; i < availableUnits.size(); i++) {
			UnitDelegate delegate = (UnitDelegate) availableUnits.get(i);

			mi = new JMenuItem(delegate.getName());
			mi.setToolTipText(delegate.getToolTipText());
			add(mi).addActionListener(newAction);		
		}
	}
	
}

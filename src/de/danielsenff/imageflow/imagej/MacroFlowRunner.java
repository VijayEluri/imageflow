/**
 * Copyright (C) 2008-2010 Daniel Senff
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.danielsenff.imageflow.imagej;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JOptionPane;

import visualap.Node;
import de.danielsenff.imageflow.ImageFlow;
import de.danielsenff.imageflow.controller.FlowRunner;
import de.danielsenff.imageflow.controller.GraphController;
import de.danielsenff.imageflow.imagej.MacroGenerator.ImageJResult;
import de.danielsenff.imageflow.models.connection.Connection;
import de.danielsenff.imageflow.models.connection.ConnectionList;
import de.danielsenff.imageflow.models.connection.Input;
import de.danielsenff.imageflow.models.unit.CommentNode;
import de.danielsenff.imageflow.models.unit.ForGroupUnitElement;
import de.danielsenff.imageflow.models.unit.GroupUnitElement;
import de.danielsenff.imageflow.models.unit.SourceUnitElement;
import de.danielsenff.imageflow.models.unit.UnitElement;
import de.danielsenff.imageflow.models.unit.UnitList;
import de.danielsenff.imageflow.models.unit.UnitElement.Type;

/**
 * MacroRunner takes the current workflow and processes it, so that a clean Macro can be 
 * created. Since for example unconnected units shall be discarded by the {@link MacroGenerator}
 * but not deleted, this creates a temporary {@link UnitList} with only the 
 * units and connections valid and useful without destroying the original workflow.
 * @author Daniel Senff
 *
 */
public class MacroFlowRunner implements FlowRunner {

	private UnitList macroUnitList;
	private MacroGenerator macroGenerator;
	
	/**
	 * @param units
	 */
	public MacroFlowRunner(final UnitList units) {
		this.macroUnitList = sortList(units.clone());
		this.macroGenerator = new MacroGenerator(macroUnitList);
	}

	
	public String generateMacro(boolean extendedMacro, boolean silent) {
		/*
		 *  analysis and verification of the connection network 
		 */
		if (!checkNetwork()) {
			System.out.println("Error in node network.");
			return null;
		}
		
		return getMacroGenerator().generateMacro(extendedMacro, silent);
	}
	
	public MacroGenerator getMacroGenerator() {
		return this.macroGenerator;
	}

	/**
	 * Is true if a {@link UnitElement} exists in the {@link UnitList} that is cleaned for
	 * running as a macro. If the unit exists, it means, that the subgraph up until this 
	 * unit is valid for running too.  
	 * @param unit 
	 * @return 
	 */
	public boolean contains(UnitElement unit) {
		return this.macroUnitList.contains(unit);
	}


	/**
	 * Returns a subset of a workflow. It only includes all elements to 
	 * calculate the final {@link UnitElement} specified.
	 * @param endUnit 
	 * @return
	 */
	public UnitList getSubUnitList(UnitElement endUnit) {
		UnitList subgraph = new UnitList();
		subgraph.add(endUnit);

		traverseGraph(endUnit, subgraph);

		return subgraph;
	}

	/**
	 * @param endUnit
	 * @return
	 */
	public MacroFlowRunner getSubMacroFlowRunner(final UnitElement endUnit) {
		return new MacroFlowRunner(getSubUnitList(endUnit));
	}

	private void traverseGraph(UnitElement unit, UnitList subgraph) {
		for (Input input : unit.getInputs()) {
			UnitElement parent = input.getParent();
			if(!subgraph.contains(parent)) {
				subgraph.add(parent);
				traverseGraph(parent, subgraph);
			}
		}
	}


	/**
	 * check if all connections have in and output
	 * @param connectionMap
	 * @return
	 */
	public boolean checkNetwork() {

		if(macroUnitList.isEmpty()) {
			System.err.println("The workflow is empty, running it doesn't do anything.");
			JOptionPane.showMessageDialog(ImageFlow.getApplication().getMainFrame(), 
					"The workflow has no displayable units, running it doesn't do anything."
					+'\n' + "The operation will not proceed.",
					"Invalid workflow", 
					JOptionPane.WARNING_MESSAGE);
			return false;
		}

		for (Node node : macroUnitList) {
			UnitElement unit = (UnitElement)node;
			for (Input input : unit.getInputs()) {
				if(input.isRequired() && !input.isConnected()) {
					JOptionPane.showMessageDialog(ImageFlow.getApplication().getMainFrame(), 
							"Some elements don't have all required input connections."
							+'\n' + "The operation will not proceed.",
							"Missing connections", 
							JOptionPane.WARNING_MESSAGE);
					return false;
				}
			}

			// sources -> file exists
			if(node instanceof SourceUnitElement) {
				SourceUnitElement sUnit = (SourceUnitElement) node;
				if(!sUnit.existsFile()) {
					JOptionPane.showMessageDialog(ImageFlow.getApplication().getMainFrame(), 
							"The file "+sUnit.getFilePath()+" doesn't exist."
							+'\n' + "The operation will not proceed.",
							"Invalid workflow", 
							JOptionPane.WARNING_MESSAGE);
					return false;
				}
			}
		}
		
		ConnectionList connections = macroUnitList.getConnections();
		if(!connections.isEmpty()) {
			// wrong number if units are discarded, number as you can see in the workflow, but not howmany are used internally
			System.out.println("Number of connections: "+ connections.size()); 
			for (Connection connection : connections) {

				if (!connection.isCompatible()) {
					JOptionPane.showMessageDialog(ImageFlow.getApplication().getMainFrame(), 
							"The graph has conflicting imagetypes."
							+'\n' + "The operation will not proceed.",
							"Connection error", 
							JOptionPane.WARNING_MESSAGE);

					System.err.println("Faulty connection, image type not compatible");
					return false;
				}

				// TODO actually this is bullshit, a connection should always be valid and existing
				switch(connection.checkConnection()) {
				case MISSING_BOTH:
				case MISSING_FROM_UNIT:
				case MISSING_TO_UNIT:
					System.err.println("Faulty connection, no input or output unit found.");
					System.err.println(connection.toString() 
							+ " with status " + connection.checkConnection());
					return false;				
				}
			}
		} else if (macroUnitList.hasSourcesAsDisplay()) {
			// ok, we got no connections, but we have Source-units, 
			// which are set to display.

			//do nothing
		} 

		// check if units got all the inputs they need
		if (!macroUnitList.areAllInputsConnected()) {
			JOptionPane.showMessageDialog(ImageFlow.getApplication().getMainFrame(), 
					"Some elements don't have all required input connections."
					+'\n' + "The operation will not proceed.",
					"Missing connections", 
					JOptionPane.WARNING_MESSAGE);
			System.err.println("not all required inputs are connected");
			return false;
		}


		//TODO check parameters
		return true;
	}

	
	
	/**
	 * @param unitElements
	 * @return
	 */
	public static UnitList sortList(final UnitList unitElements) {

		// temporary list, discarded after this method call
		UnitList orderedList = new UnitList();

		// the unitElements can contain grouped units
		// those need to be destroyed and reintegrated into 
		// the regular workflow
		
		
		// as long as we got groups in the unitList
		// we identify the group, ungroup it and see if all groups all 
		Collection<GroupUnitElement> foundGroups = new Vector<GroupUnitElement>();
		for (Node node : unitElements) {
			if(node instanceof GroupUnitElement && !(node instanceof ForGroupUnitElement)) {
				addFoundGroup(foundGroups, node);
			}
		}

		for (GroupUnitElement group : foundGroups) {
			GraphController.ungroup(group, unitElements);	
		}



		// reset all marks
		unitElements.unmarkUnits();

		int mark = 0;	// nth element, that has been sorted
		int i = 0; 		// nth lap in the loop
		int index = 0; 	// index 0 < i < unitElements.size()

		try {
			//loop over all units, selection sort, levelorder
			// TODO I don't like this condition, daniel
			while(!unitElements.isEmpty()) {
				index = i % unitElements.getSize();
				Node node =  unitElements.get(index);

				// find out what kind of node is stored
				if(node instanceof CommentNode) {
					//if comment then remove and ignore, we don't need it
					unitElements.remove(node);
				} else if (node instanceof UnitElement) {
					UnitElement unit = (UnitElement) node;
					// check if all inputs of this node are marked
					// if so, this unit is moved from the old list to the new one

					if(unit.hasMarkedOutput()) throw new Exception("Unit has Output marked, "
							+ "the output should be marked if this unit is already processed in this workflow, "
							+ "by which point it shouldn't be here anymore");

					if(!unit.hasDisplayBranch()) {
						// unit itself is not a display and
						// if it doesn't have any unit in its outputs that has
						// then it can be removed without consequences
						unitElements.remove(index);
					}
					else if(unit.hasAllInputsMarked()) {
						// success
						// increment mark & mark outputs
						mark++;	
						unit.setMark(mark);

						// remove from the old list and
						// move this to the new ordered list
						Node remove = unitElements.remove(index);
						orderedList.add(remove);
					} else if (!unit.hasRequiredInputsConnected() 
							&& unit.getUnitType() != Type.SOURCE) {
						// if unit has no connections actually, it can be discarded right away
						unitElements.remove(index);
						// if there is a branch with two units connected, the first one will be discarded, 
						// the second will still exist, but as the input is now missing, it will 
						// be deleted in the next lap
					} else if (!unit.hasOutputsConnected() 
							&& unit.getUnitType() == Type.SOURCE 
							&& !unit.isDisplayAny()) {
						// if source has no connected outputs and is not visible
						unitElements.remove(index);
					} 
					//else throw new Exception("can't be handled");
				} else throw new Exception("this is no node object");
				// Selection Sort
				// each time an element whose previous nodes have already been registered
				// is found the next loop over the element list is one element shorter.
				// thereby having O(n^2) maybe this can be done better later
				i++;

			}

		} catch(Exception ex) {
			// restore list, without damaging it
			ex.printStackTrace();
		}

		return orderedList;
	}


	private static void addFoundGroup(Collection<GroupUnitElement> foundGroups,
			Node node) {
		if(node instanceof GroupUnitElement) {
			GroupUnitElement group = (GroupUnitElement) node;
			foundGroups.add(group);
			for (Node embeddedNode : group.getNodes()) {
				if(node instanceof GroupUnitElement)
					addFoundGroup(foundGroups, embeddedNode);
			}
		}
		
	}


	public ArrayList<ImageJResult> getOpenedImages() {
		if (getMacroGenerator() != null) {
			return getMacroGenerator().getOpenedImages();
		} else {
			return new ArrayList<ImageJResult>();
		}
	}

}

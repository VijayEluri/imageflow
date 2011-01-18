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
package de.danielsenff.imageflow.controller;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.DataFormatException;

import javax.swing.JOptionPane;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import visualap.Node;
import de.danielsenff.imageflow.ImageFlow;
import de.danielsenff.imageflow.models.MacroElement;
import de.danielsenff.imageflow.models.connection.Connection;
import de.danielsenff.imageflow.models.connection.ConnectionList;
import de.danielsenff.imageflow.models.connection.Input;
import de.danielsenff.imageflow.models.connection.Output;
import de.danielsenff.imageflow.models.datatype.DataType;
import de.danielsenff.imageflow.models.datatype.DataTypeFactory;
import de.danielsenff.imageflow.models.delegates.UnitDescription;
import de.danielsenff.imageflow.models.parameter.BooleanParameter;
import de.danielsenff.imageflow.models.parameter.ChoiceParameter;
import de.danielsenff.imageflow.models.parameter.IntegerParameter;
import de.danielsenff.imageflow.models.parameter.Parameter;
import de.danielsenff.imageflow.models.unit.CommentNode;
import de.danielsenff.imageflow.models.unit.GroupUnitElement;
import de.danielsenff.imageflow.models.unit.UnitElement;
import de.danielsenff.imageflow.models.unit.UnitFactory;
import de.danielsenff.imageflow.models.unit.UnitList;

/**
 * Read a workflow from ifw-file.
 * @author Daniel Senff
 *
 */
public class WorkflowXMLBuilder {

	private UnitList unitList;
	private Collection<ConnectionDelegate> connectionDelegates;
	private Collection<GroupUnitElement> groupUnits;
	private HashMap<Integer, UnitElement> newNodes;

	/**
	 * Create a builder object based on a {@link UnitList}
	 * @param units
	 */
	public WorkflowXMLBuilder(final UnitList units) {
		this.unitList = units;
		this.connectionDelegates = new Vector<ConnectionDelegate>();
		this.newNodes = new HashMap<Integer, UnitElement>();
		this.groupUnits = new Vector<GroupUnitElement>();
	}


	/**
	 * Reads the contents of a flow-XML input stream.
	 * @param url 
	 */
	public void read(URL url) {
		try {
			SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(url);

			Element root = doc.getRootElement();

			System.out.println("Read nodes ...");

			// read units
			Element unitsElement = root.getChild("Units");

			// TODO newNodes can be removed
			readUnits(newNodes, url, unitsElement);
			
			// at this point we have all units in the workflow, however no connections
			// groups are missing their inputs and outputs

			System.out.println("Process Connection Delegates ...");

			// process ConnectionDelegates
			for (ConnectionDelegate conndel : connectionDelegates) {
				conndel.list.add(readConnection(newNodes, conndel));
			}

			System.out.println("Process Groups and add inputs and outputs ...");

			// process groups
			for (GroupUnitElement unit : groupUnits) {
				unit.dealWithConnections(getConnectionList());
			}

			System.out.println("Read global connections ...");

			// read connections
			Element connectionsElement = root.getChild("Connections");
			readConnections(newNodes, connectionsElement);
		}
		catch (Exception e) {
			System.err.println("Invalid XML-File!");
			e.printStackTrace();

			JOptionPane.showMessageDialog(((ImageFlow)ImageFlow.getInstance()).getMainFrame(), 
					"The selected workflow could not be loaded.",
					"Workflow not loaded", 
					JOptionPane.INFORMATION_MESSAGE);
		}	
	}

	private void readUnits(HashMap<Integer, UnitElement> newNodes,
			URL url, Element unitsElement) {
		if (unitsElement != null) {  
			List<Element> unitsList = unitsElement.getChildren();
			Iterator<Element> unitsIterator = unitsList.iterator();

			// loop over all Units
			Element actualUnitElement;
			Node node;
			while (unitsIterator.hasNext()) { 
				actualUnitElement = unitsIterator.next();
				node = readNode(actualUnitElement, url);
				getUnitList().add(node);
			}
		}
	}

	/**
	 * @param actualUnitElement
	 * @param url
	 * @return
	 */
	protected Node readNode(final Element actualUnitElement, final URL url) {
		int xPos = Integer.parseInt(actualUnitElement.getChild("XPos").getValue());
		int yPos = Integer.parseInt(actualUnitElement.getChild("YPos").getValue());
		Point position = new Point(xPos, yPos);
		String label = "";

		if(actualUnitElement.getChild("Label") != null)
			label = actualUnitElement.getChild("Label").getValue();

//		System.out.println("Read unit: " +label);

		Element unitDescriptionElement = actualUnitElement.getChild("UnitDescription");
		Node node;
		if(actualUnitElement.getChild("UnitID") != null 
				&& unitDescriptionElement != null) {
			node = createUnitElement(actualUnitElement, url, position,	label, unitDescriptionElement);
		} else {
			node = createCommentNode(position, label);
		}
		return node;
	}


	/**
	 * Create the unit element described in the workflow xml file.
	 * the Unit definition is the same as the XML Unit Definition.
	 * @param actualUnitElement
	 * @param url
	 * @param position
	 * @param label
	 * @param unitDescriptionElement
	 * @return
	 */
	protected Node createUnitElement(final Element actualUnitElement,
			final URL url, 
			final Point position, 
			final String label,
			final Element unitDescriptionElement) {
		int unitID 	= Integer.parseInt(actualUnitElement.getChild("UnitID").getValue());
		UnitDescription unitDescription = new UnitDescription(url);
		try {
			unitDescription.readXML(unitDescriptionElement);
		} catch (DataFormatException e) {
			e.printStackTrace();
		}


		Element unitsElement = unitDescriptionElement.getChild("Units");
		UnitElement unitElement;
		Element internalConnectionsElement = unitDescriptionElement.getChild("InternalConnections");
		Element externalConnectionsElement = unitDescriptionElement.getChild("ExternalConnections");
		Element originalConnectionsElement = unitDescriptionElement.getChild("OriginalConnections");
		if(unitsElement != null 
				&& internalConnectionsElement != null
				&& externalConnectionsElement != null
				&& originalConnectionsElement != null) {
			//create GroupUnit
			unitElement = readGroup(url, position, label, unitID,
					unitDescription, unitsElement,
					internalConnectionsElement, 
					externalConnectionsElement,
					originalConnectionsElement);
		} else {
			// create UnitElement
			unitElement = createProcessingUnit(position, label, unitID, unitDescription);
		}

		return unitElement;
	}


	private Node createCommentNode(final Point position, String label) {
		return new CommentNode(position, label);
	}

	/**
	 * Create a regular processing {@link UnitElement}.
	 * @param position
	 * @param label
	 * @param unitID
	 * @param unitDescription
	 * @return
	 */
	protected UnitElement createProcessingUnit(final Point position, 
			final String label,
			final int unitID, 
			final UnitDescription unitDescription) {
		UnitElement unitElement = UnitFactory.createProcessingUnit(unitDescription, position);
		unitElement.setDisplay(unitDescription.getIsDisplayUnit());
		unitElement.setHelpString(unitDescription.helpString);
		unitElement.setLabel(label);
		newNodes.put(unitID, unitElement);
		return unitElement;
	}

	/**
	 * Read {@link GroupUnitElement} from XML.
	 * @param url
	 * @param position
	 * @param label
	 * @param unitID
	 * @param unitDescription
	 * @param unitsElement
	 * @param internalConnectionsElement
	 * @param externalConnectionsElement
	 * @param originalConnectionsElement
	 * @return
	 */
	protected UnitElement readGroup(final URL url, 
			final Point position,
			final String label, 
			final int unitID, 
			final UnitDescription unitDescription,
			final Element unitsElement, 
			final Element internalConnectionsElement,
			final Element externalConnectionsElement,
			final Element originalConnectionsElement) {
		System.out.println("Process group..");

		UnitElement unitElement = new GroupUnitElement(position, label);
		HashMap<Integer, UnitElement> embeddedNodes = new HashMap<Integer, UnitElement>();

		unitElement.setHelpString(unitDescription.helpString);
		newNodes.put(unitID, unitElement);
		groupUnits.add((GroupUnitElement) unitElement);

		/*for (int i = 0; i < unitDescription.numInputs; i++) {
			unitElement.addInput(UnitFactory.createInput(unitDescription.input[i+1], unitElement, i+1));
		}

		for (int i = 0; i < unitDescription.numOutputs; i++) {
			unitElement.addOutput(UnitFactory.createOutput(unitDescription.output[i+1], unitElement, i+1));
		}*/


		if(unitsElement != null) {

			List<Element> unitsList = unitsElement.getChildren();
			Iterator<Element> unitsIterator = unitsList.iterator();

			// loop over all Units
			while (unitsIterator.hasNext()) { 
				Element actualEmbeddedUnitElement = unitsIterator.next();
				Node embeddedNode = readNode(actualEmbeddedUnitElement, url);
				((GroupUnitElement)unitElement).getNodes().add(embeddedNode);
				if(embeddedNode instanceof UnitElement) {
					System.out.println("put in embeddedNodes: " + embeddedNode 
							+ " unitid: "+ ((UnitElement)embeddedNode).getUnitID());
					newNodes.put(((UnitElement)embeddedNode).getUnitID(), (UnitElement) embeddedNode);
					embeddedNodes.put(((UnitElement)embeddedNode).getUnitID(), (UnitElement) embeddedNode);
				}
			}
		}

		if(internalConnectionsElement != null) {
			List<Element> connectionsList = internalConnectionsElement.getChildren();
			Collection<Connection> groupInternalConnections = 
				((GroupUnitElement)unitElement).getInternalConnections();
			readGroupConnections(connectionsList, groupInternalConnections);
		}

		if(originalConnectionsElement != null) {
			// FIXME WTF, why internal connection here? is this a bug?
			List<Element> connectionsList = internalConnectionsElement.getChildren();
			Collection<Connection> groupOriginalConnections = 
				((GroupUnitElement)unitElement).getOriginalConnections();
			readGroupConnections(connectionsList, groupOriginalConnections);
		}

		if(externalConnectionsElement != null) {
			List<Element> connectionsList = externalConnectionsElement.getChildren();
			Collection<Connection> groupExternalConnections = 
				((GroupUnitElement)unitElement).getExternalConnections();
			readGroupConnections(connectionsList, groupExternalConnections);
		}

		return unitElement;
	}


	private void readGroupConnections(List<Element> connectionsList,
			Collection<Connection> groupExternalConnections) {
		Iterator<Element> connectionsIterator = connectionsList.iterator();

		// external connections require the hashmap with all embedded and existing units	
		// we need all units of the workflow :/
		ConnectionDelegate conDelegate;
		while (connectionsIterator.hasNext()) { 
			conDelegate = new ConnectionDelegate(connectionsIterator.next(), groupExternalConnections);
			connectionDelegates.add(conDelegate);
		}
	}


	private class ConnectionDelegate {
		public int fromUnitID, fromUnitOutput;
		public int toUnitID, toUnitInput;
		// to add this list
		public Collection<Connection> list;

		public ConnectionDelegate(int fromUnitID, int fromUnitOutput,
				int toUnitID, int toUnitInput, Collection<Connection> list) {
			this.fromUnitID = fromUnitID;
			this.fromUnitOutput = fromUnitOutput;
			this.toUnitID = toUnitID;
			this.toUnitInput = toUnitInput;
			this.list = list;
		}

		public ConnectionDelegate(Element connectionElement, Collection<Connection> list) {
			this(Integer.parseInt(connectionElement.getChild("FromUnitID").getValue()), 
					Integer.parseInt(connectionElement.getChild("FromOutputNumber").getValue()),
					Integer.parseInt(connectionElement.getChild("ToUnitID").getValue()),
					Integer.parseInt(connectionElement.getChild("ToInputNumber").getValue()),
					list
			);
		}
	} 


	private void readConnections(HashMap<Integer, UnitElement> newNodes, Element connectionsElement) {
		//unitsElement != null &&
		if ( connectionsElement != null) {  
			List<Element> connectionsList = connectionsElement.getChildren();
			Iterator<Element> connectionsIterator = connectionsList.iterator();
			while (connectionsIterator.hasNext()) { 
				getUnitList().addConnection(readConnection(newNodes, connectionsIterator.next()));
			}
		}
	}


	private static Connection readConnection(HashMap<Integer, UnitElement> newNodes,
			Element connectionElement) {
		int fromUnitID = 
			Integer.parseInt(connectionElement.getChild("FromUnitID").getValue());
		int fromOutputNumber = 
			Integer.parseInt(connectionElement.getChild("FromOutputNumber").getValue());
		int toUnitID = 
			Integer.parseInt(connectionElement.getChild("ToUnitID").getValue());
		int toInputNumber = 
			Integer.parseInt(connectionElement.getChild("ToInputNumber").getValue());

		Connection con = null;
		try {
			con = new Connection(newNodes.get(fromUnitID), fromOutputNumber, 
					newNodes.get(toUnitID), toInputNumber);
		} catch (ArrayIndexOutOfBoundsException ex) {
			System.err.println("fromUnitID " + fromUnitID
					+ " fromOutputNumber" + fromOutputNumber
					+ " toUnitId " + toUnitID
					+ " toInputNumber " + toInputNumber);
		}
		return con;
	}

	private static Connection readConnection(HashMap<Integer, UnitElement> newNodes,
			ConnectionDelegate connDel) {
		int fromUnitID = connDel.fromUnitID;
		int fromOutputNumber = connDel.fromUnitOutput;
		int toUnitID = connDel.toUnitID;
		int toInputNumber = connDel.toUnitInput;

		Connection con = null;
		try {
			con = new Connection(newNodes.get(fromUnitID), fromOutputNumber, 
					newNodes.get(toUnitID), toInputNumber);
		} catch (ArrayIndexOutOfBoundsException ex) {
			System.err.println("fromUnitID " + fromUnitID
					+ " fromOutputNumber" + fromOutputNumber
					+ " toUnitId " + toUnitID
					+ " toInputNumber " + toInputNumber);
		}
		return con;
	}


	/**
	 * @param file
	 * @throws IOException
	 */
	public void write(final File file) throws IOException {
		Element root = new Element("FlowDescription");
		Document flowGraph = new Document(root);

		Element units = new Element("Units");
		Element unitElement;
		root.addContent(units);
		for (Node node : getUnitList()) {
			unitElement = new Element("Unit");
			units.addContent(unitElement);

			writeXMLNode(node, unitElement);
		} // end node

		// now for connections
		Element connectionsElement = new Element("Connections");
		root.addContent(connectionsElement);

		for (Connection connection : getConnectionList()) {
			writeXMLConnection(connectionsElement, connection);
		}

		// output
		FileOutputStream fos = new FileOutputStream(file);
		new XMLOutputter(Format.getPrettyFormat()).output(flowGraph, fos);
	}


	private void writeXMLNode(Node node, Element unitElement) {
		// unit details 

		Element xPos = new Element("XPos");
		xPos.addContent(node.getOrigin().x+"");
		unitElement.addContent(xPos);

		Element yPos = new Element("YPos");
		yPos.addContent(node.getOrigin().y+"");
		unitElement.addContent(yPos);

		Element label = new Element("Label");
		label.addContent(node.getLabel());
		unitElement.addContent(label);

		if(node instanceof UnitElement) {

			UnitElement unit = (UnitElement) node;
			Element unitDescription = writeXMLUnitElement(unit, unitElement);

			if(node instanceof GroupUnitElement) {
				GroupUnitElement group = (GroupUnitElement)node;

				// groupUnits have additional fields for
				// unitlist included in group
				Element embeddedUnitsElement = new Element("Units");
				unitDescription.addContent(embeddedUnitsElement);
				Element embeddedUnitElement;
				for (Node embeddedNode : group.getNodes()) {
					embeddedUnitElement = new Element("Unit");
					embeddedUnitsElement.addContent(embeddedUnitElement);
					writeXMLNode(embeddedNode, embeddedUnitElement);
				}

				// list of internal connections
				Element internalConnectionsElement = new Element("InternalConnections");
				unitDescription.addContent(internalConnectionsElement);
				for (Connection connection : group.getInternalConnections()) {
					writeXMLConnection(internalConnectionsElement, connection);	
				}

				// list of external connections
				Element externalConnectionsElement = new Element("ExternalConnections");
				unitDescription.addContent(externalConnectionsElement);
				for (Connection connection : group.getExternalConnections()) {
					writeXMLConnection(externalConnectionsElement, connection);	
				}				

				// list of original connections
				Element originalConnectionsElement = new Element("OriginalConnections");
				unitDescription.addContent(originalConnectionsElement);
				for (Connection connection : group.getOriginalConnections()) {
					writeXMLConnection(originalConnectionsElement, connection);	
				}
			}
		} // end unitelement
	}


	private Element writeXMLUnitElement(UnitElement unit, Element unitElement) {
		Element unitID = new Element("UnitID");
		unitID.addContent(unit.getUnitID()+"");
		unitElement.addContent(unitID);

		// unit description analog to the Unit-XML
		Element unitDescription = new Element("UnitDescription");
		unitElement.addContent(unitDescription);

		Element general = new Element("General");
		unitDescription.addContent(general);

		Element unitName = new Element("UnitName");
		unitName.addContent(unit.getUnitName());
		general.addContent(unitName);

		Element pathToIcon = new Element("PathToIcon");
		pathToIcon.addContent(unit.getIconPath());
		general.addContent(pathToIcon);

		Element imageJSyntax = new Element("ImageJSyntax");
		imageJSyntax.addContent(((MacroElement)unit.getObject()).getImageJSyntax());
		general.addContent(imageJSyntax);

		Element color = new Element("Color");
		String colorHex = Integer.toHexString( unit.getColor().getRGB() );
		color.addContent("0x"+colorHex.substring(2));
		general.addContent(color);

		Element iconSize = new Element("IconSize");
		iconSize.addContent(unit.getCompontentSize().toString());
		general.addContent(iconSize);

		Element helpStringU = new Element("HelpString");
		helpStringU.addContent(unit.getHelpString());
		general.addContent(helpStringU);

		Element doDisplayU = new Element("DoDisplay");
		doDisplayU.addContent(unit.isDisplay() ? "true" : "false");
		general.addContent(doDisplayU);


		// deal with all parameters
		Element parameters = new Element("Parameters");
		unitDescription.addContent(parameters);
		for (Parameter parameter : unit.getParameters()) {
			Element parameterElement = new Element("Parameter");
			parameters.addContent(parameterElement);	

			Element name = new Element("Name");
			name.addContent(parameter.getDisplayName());
			parameterElement.addContent(name);

			Element dataType = new Element("DataType");
			dataType.addContent(parameter.getParaType());
			parameterElement.addContent(dataType);
			if(parameter.getOptions().get("as") != null) {
				String dataTypeAs = (String) parameter.getOptions().get("as");
				dataType.setAttribute("as", dataTypeAs);
			}
			

			Element value = new Element("Value");
			value.addContent(parameter.getValue()+"");

			Element helpStringP = new Element("HelpString");
			helpStringP.addContent(parameter.getHelpString());
			parameterElement.addContent(helpStringP);

			//special parameters

			if(parameter instanceof ChoiceParameter) {

				value = new Element("Value");
				ChoiceParameter choiceParameter = (ChoiceParameter)parameter;
				String choiceString =  choiceParameter.getChoicesString();
				value.addContent(choiceString);

				Element choiceNumber = new Element("ChoiceNumber");
				choiceNumber.addContent(choiceParameter.getChoiceIndex()+"");
				parameterElement.addContent(choiceNumber);	
			}
			parameterElement.addContent(value);

			if(parameter instanceof IntegerParameter) {
				if(parameter.getOptions().get("min") != null) {
					int min = (Integer) parameter.getOptions().get("min");
					value.setAttribute("min", Integer.toString(min));
				}
				if(parameter.getOptions().get("max") != null) {
					int max = (Integer) parameter.getOptions().get("max");
					value.setAttribute("max", Integer.toString(max));
				}
				
			}
			
			if(parameter instanceof BooleanParameter) {
				Element trueString = new Element("TrueString");
				trueString.addContent(((BooleanParameter)parameter).getTrueString());
				parameterElement.addContent(trueString);	
			}
		}

		// deal with inputs
		Element inputs = new Element("Inputs");
		unitDescription.addContent(inputs);
		for (Input input : unit.getInputs()) {
			writeXMLInput(inputs, input);
		}

		// deal with outputs
		Element outputs = new Element("Outputs");
		unitDescription.addContent(outputs);
		for (Output output : unit.getOutputs()) {
			writeXMLOutput(outputs, output);
		}
		return unitDescription;
	}


	private void writeXMLInput(Element inputs, Input input) {
		Element inputElement = new Element("Input");
		inputs.addContent(inputElement);	

		Element name = new Element("Name");
		name.addContent(input.getDisplayName());
		inputElement.addContent(name);

		Element shortName = new Element("ShortName");
		shortName.addContent(input.getShortDisplayName());
		inputElement.addContent(shortName);

		Element required = new Element("Required");
		required.addContent(input.isRequired() ? "true" : "false");
		inputElement.addContent(required);

		Element dataTypeElement = new Element("DataType");
		DataType dataType = input.getDataType();
		dataTypeElement.addContent(dataType.getClass().getSimpleName());
		inputElement.addContent(dataTypeElement);

		if(dataType instanceof DataTypeFactory.Image) {
			Element imageType = new Element("ImageType");
			imageType.addContent(""+((DataTypeFactory.Image)dataType).getImageBitDepth());
			inputElement.addContent(imageType);	
		}


		Element needToCopyInput = new Element("NeedToCopyInput");
		String boolNeedCopy = input.isNeedToCopyInput() ? "true" : "false"; 
		needToCopyInput.addContent(boolNeedCopy);
		inputElement.addContent(needToCopyInput);
	}


	private void writeXMLOutput(Element outputs, Output output) {
		Element outputElement = new Element("Output");
		outputs.addContent(outputElement);	

		Element name = new Element("Name");
		name.addContent(output.getDisplayName());
		outputElement.addContent(name);

		Element shortName = new Element("ShortName");
		shortName.addContent(output.getShortDisplayName());
		outputElement.addContent(shortName);

		Element dataType = new Element("DataType");
		dataType.addContent(output.getDataType().getClass().getSimpleName());
		outputElement.addContent(dataType);

		if(output.getDataType() instanceof DataTypeFactory.Image) {
			Element imageType = new Element("ImageType");
			imageType.addContent(""+((DataTypeFactory.Image)output.getDataType()).getImageBitDepth());
			outputElement.addContent(imageType);	
		}

		// In case of plugins with multiple outputs
		// I want to leave the option to display only selected outputs
		Element doDisplay = new Element("DoDisplay");
		String boolIsDisplay = output.isDoDisplay() ? "true" : "false"; 
		doDisplay.addContent(boolIsDisplay);
		outputElement.addContent(doDisplay);
	}


	private void writeXMLConnection(Element connectionsElement, Connection edge) {
		Connection conn = (Connection) edge;

		Element connectionElement = new Element("Connection");
		connectionsElement.addContent(connectionElement);

		Element fromUnitID = new Element("FromUnitID");
		fromUnitID.addContent(""+ ((UnitElement)conn.getFromUnit()).getUnitID());
		connectionElement.addContent(fromUnitID);

		Element fromOutputNumber = new Element("FromOutputNumber");
		fromOutputNumber.addContent(""+conn.getOutput().getIndex());
		connectionElement.addContent(fromOutputNumber);

		Element toUnitID = new Element("ToUnitID");
		toUnitID.addContent(""+((UnitElement)conn.getToUnit()).getUnitID());
		connectionElement.addContent(toUnitID);

		Element toInputNumber = new Element("ToInputNumber");
		toInputNumber.addContent(""+conn.getInput().getIndex());
		connectionElement.addContent(toInputNumber);
	}


	/**
	 * Returns the UnitList processed by this Builder.
	 * @return the unitList
	 */
	public UnitList getUnitList() {
		return unitList;
	}

	/**
	 * Returns the Connections in the UnitList processed by this Builder.
	 * @return
	 */
	public ConnectionList getConnectionList() {
		return getUnitList().getConnections();
	}

}

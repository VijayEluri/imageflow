package backend;
import graph.Edge;
import graph.Edges;
import ij.IJ;
import ij.ImageJ;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import macro.MacroGenerator;
import models.Connection;
import models.ConnectionList;
import models.Input;
import models.unit.UnitElement;
import models.unit.UnitFactory;
import models.unit.UnitList;



/**
 * @author danielsenff
 *
 */
public class GraphController {

	
	
	private final UnitList unitElements;
	private final ConnectionList connectionMap;

	/**
	 * 
	 */
	public GraphController() {

		////////////////////////////////////////////////////////
		// setup of units
		////////////////////////////////////////////////////////
		
		unitElements = new UnitList();
//		unitElements.add(null);
		
		final UnitElement sourceUnit = UnitFactory.createSourceUnit();
		unitElements.add(sourceUnit);

		final UnitElement blurUnit = UnitFactory.createGaussianBlurUnit(new Point(150, 150));
		unitElements.add(blurUnit);
		
		final UnitElement mergeUnit = UnitFactory.createImageCalculatorUnit(new Point(320, 30));
		unitElements.add(mergeUnit);
		
		

		
		////////////////////////////////////////////////////////
		// setup the connections
		////////////////////////////////////////////////////////
		
		connectionMap = new ConnectionList();
		
		// add six connections
		// the conn is established on adding
		// fromUnit, fromOutputNumber, toUnit, toInputNumber
		Connection con;
		/*con = new Connection(sourceUnit,1,blurUnit,1);
		connectionMap.add(con);
		con = new Connection(blurUnit,1,mergeUnit,1);
		connectionMap.add(con);
		con = new Connection(sourceUnit,1,mergeUnit,2);
		connectionMap.add(con);*/

		
		// remove one connection
		//connectionMap.remove( Connection.getID(2,1,5,1) );
		
		
		
	}

	/**
	 * verification and generation of the ImageJ macro
	 */
	public void generateMacro() {
		////////////////////////////////////////////////////////
		// analysis and 
		// verification of the connection network
		////////////////////////////////////////////////////////
		
		final boolean networkOK = checkNetwork(connectionMap);
		
		
		////////////////////////////////////////////////////////
		// generation of the ImageJ macro
		////////////////////////////////////////////////////////
		
		// unitElements has to be ordered according to the correct processing sequence
		if(networkOK) {
			final String macro = MacroGenerator.generateMacrofromUnitList(unitElements);
			
			new ImageJ();
			IJ.log(macro);
			IJ.runMacro(macro, "");
		} else {
			System.out.println("Error in node network.");
		}
	}
	

	public boolean checkNetwork(final ConnectionList connectionMap) {
		boolean networkOK = true;
		
		//TODO check if all connections have in and output
		
//		for (final Connection (Connection)connection : connectionMap) {
		for (Iterator iterator = connectionMap.iterator(); iterator.hasNext();) {
			Connection connection = (Connection) iterator.next();
		
			switch(connection.checkConnection()) {
				case MISSING_BOTH:
				case MISSING_FROM_UNIT:
				case MISSING_TO_UNIT:
					networkOK = false;
					System.out.println(connection.checkConnection());
					System.out.println(connection.toString());
					break;					
			}
		}
		
		//TODO check if units got all the inputs they need
		for (final Object element : unitElements) {
			final UnitElement unit = (UnitElement) element;
			
			if (unit != null) { // just to avoid the first null element, which is kinda legacy
				if(unit.hasInputs()) {
					final ArrayList<Input> inputs = unit.getInputs();
					for (int i = 0; i < inputs.size(); i++) {
						final Input input = inputs.get(i);
						if(!input.isConnected() && input.isRequiredInput()) {
							networkOK = false;
						}
					}	
				}
			}
		}

		//TODO check parameters
		return networkOK;
	}

	/**
	 * @return the unitElements
	 */
	public UnitList getUnitElements() {
		return this.unitElements;
	}
	
	public static void main(final String[] args) {
		final GraphController controller = new GraphController();
		controller.generateMacro();
	}

	/**
	 * @return
	 */
	public Edges getConnections() {
		return this.connectionMap;
	}

}

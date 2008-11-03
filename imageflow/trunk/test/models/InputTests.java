/**
 * 
 */
package models;

import java.awt.Dimension;

import models.unit.UnitElement;
import models.unit.UnitFactory;
import junit.framework.TestCase;

/**
 * @author danielsenff
 *
 */
public class InputTests extends TestCase {

	public void testIsConnected() {
		
		// test output-only
		UnitElement sourceUnit = UnitFactory.createBackgroundUnit(new Dimension(12, 12));
		
		// test input/output case
		UnitElement filterUnit1 = UnitFactory.createAddNoiseUnit();
		UnitElement filterUnit2 = UnitFactory.createAddNoiseUnit();
		
		Input filterInput = filterUnit1.getInput(0);
		
		// test beforehand
		assertFalse("input not connected", filterInput.isConnected());
		
		Connection conn = new Connection(sourceUnit, 1, filterUnit1, 1);
		
		ConnectionList connList = new ConnectionList();
		connList.add(conn);
		
		// test after connecting
		assertTrue("input connected", filterInput.isConnected());	
	}
	
	public void testIsConnectedWith() {
		// test output-only
		UnitElement source1Unit = UnitFactory.createBackgroundUnit(new Dimension(12, 12));
		Output source1Output = source1Unit.getOutput(0);
		UnitElement source2Unit = UnitFactory.createBackgroundUnit(new Dimension(12, 12));
		Output source2Output = source2Unit.getOutput(0);
		
		// test input/output case
		UnitElement filterUnit1 = UnitFactory.createAddNoiseUnit();
		UnitElement filterUnit2 = UnitFactory.createAddNoiseUnit();
		
		Input filterInput = filterUnit1.getInput(0);
		
		// test beforehand
		assertFalse("input not connected with source1Output", 
				filterInput.isConnectedWith(source1Output));
		assertFalse("input not connected with source2Output", 
				filterInput.isConnectedWith(source2Output));
		
		Connection conn = new Connection(source1Unit, 1, filterUnit1, 1);
		ConnectionList connList = new ConnectionList();
		connList.add(conn);
		
		
		// test after connecting
		assertTrue("input not connected with source1Output", 
				filterInput.isConnectedWith(source1Output));
		assertFalse("input not connected with source2Output", 
				filterInput.isConnectedWith(source2Output));
		
	}
	
	
}
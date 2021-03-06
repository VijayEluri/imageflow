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
package de.danielsenff.imageflow.tasks;

import java.util.ArrayList;

import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

import de.danielsenff.imageflow.ImageFlow;
import de.danielsenff.imageflow.ImageFlowView;
import de.danielsenff.imageflow.ImageFlowView.CodePreviewDialog;
import de.danielsenff.imageflow.controller.GraphController;
import de.danielsenff.imageflow.imagej.MacroFlowRunner;
import de.danielsenff.imageflow.imagej.MacroGenerator.ImageJResult;

/**
 * Generate and display the macro based on the workflow.
 * @author Daniel Senff
 *
 */
public class GenerateMacroTask extends Task<Object, String> {

	protected GraphController graphController;
	protected boolean showCode;
	protected ArrayList<ImageJResult> openedImages;
	protected boolean silent;
	
	/**
	 * @param app
	 * @param graphController
	 */
	public GenerateMacroTask(final Application app, 
			final GraphController graphController) {
		super(app);
		this.graphController = graphController;
		this.showCode = true;
		this.silent = false;
	}
	
	/**
	 * Create macro from the {@link GraphController} stored in this Task.
	 * @return
	 */
	@Override 
	protected String doInBackground() throws InterruptedException {

		ImageFlowView.getProgressBar().setIndeterminate(true);
		ImageFlowView.getProgressBar().setVisible(true);
    	
    	final MacroFlowRunner macroFlowRunner = new MacroFlowRunner(graphController.getUnitElements());
    	// generates Macro with callback function (for progressBar)
    	final String macro = macroFlowRunner.generateMacro(true, this.silent);
    	openedImages = macroFlowRunner.getOpenedImages();
    	
		
		if(this.showCode && macro != null) {
			// generates cleaner Macro without callback function (for progressBar)
			final String reducedMacro = graphController.generateMacro(false, false);
			CodePreviewDialog previewBox = ((ImageFlowView)((ImageFlow)ImageFlow.getApplication()).getMainView()).showCodePreviewBox();
			previewBox.setVisible(true);
			previewBox.setMacroCode(reducedMacro);
						
		}
		ImageFlowView.getProgressBar().setIndeterminate(false);
		return macro;
	}

	
	/*@Override
	protected void succeeded(final Object superclass) {
    	ImageFlowView.getProgressBar().setVisible(false);
    	ImageFlowView.getProgressBar().setValue(0);
    }*/
	
}

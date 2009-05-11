package imageflow.tasks;

import imageflow.ImageFlow;
import imageflow.ImageFlowView;
import imageflow.backend.GraphController;

import org.jdesktop.application.Application;
import org.jdesktop.application.Task;


/**
 * @author danielsenff
 *
 */
public class RunMacroTask extends Task {
	
	GraphController graphController;
	private boolean showlog;
	
	public RunMacroTask(final Application app, GraphController graphController, boolean doShowLog) {
		super(app);
		this.graphController = graphController;
		this.showlog = showlog;
	}

	@Override 
	protected Void doInBackground() throws InterruptedException {
		
		String macro = graphController.generateMacro();
//		((ImageFlowView)ImageFlow.getApplication().getMainView()).
		//TODO use exceptions 
		if(macro != null) {
			System.out.println(macro);
			graphController.runImageJMacro(macro, this.showlog);	
		}
		
		return null;
	}
	
	/*@Override protected void done() {
		setMessage(isCancelled() ? "Canceled." : "Done.");
	}*/
}
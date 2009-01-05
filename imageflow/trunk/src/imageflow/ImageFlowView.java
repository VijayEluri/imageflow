package imageflow;

import graph.Node;
import graph.Selection;
import gui.GPanelPopup;
import gui.GraphPanel;
import gui.InsertUnitMenu;
import helper.FileDrop;
import imageflow.models.ConnectionList;
import imageflow.models.unit.CommentNode;
import imageflow.models.unit.UnitElement;
import imageflow.models.unit.UnitFactory;
import imageflow.models.unit.UnitList;
import imageflow.tasks.GenerateMacroTask;
import imageflow.tasks.ImportGraphTask;
import imageflow.tasks.LoadFlowGraphTask;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.ScrollPane;
import java.io.File;
import java.util.ArrayList;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

import visualap.Delegate;
import actions.CheckGraphAction;
import actions.CopyUnitAction;
import actions.CutUnitAction;
import actions.Example0_XML_Action;
import actions.Example1Action;
import actions.Example2Action;
import actions.PasteUnitAction;
import actions.ShowUnitParametersAction;
import backend.DelegatesController;
import backend.GraphController;
import backend.Model;
import backend.ModelListener;


public class ImageFlowView extends FrameView {

//	private static final Logger logger = Logger.getLogger(DocumentEditorView.class.getName());
	private JDialog aboutBox;
	
	private UnitList units;
	private ConnectionList connections;
	private GraphController graphController;




	private JTextArea macroArea;
	private GraphPanel graphPanel;
	private ArrayList<Delegate> unitDelegates;

	private File file;

	

	private boolean modified = false;
	private boolean selectedUnits = false;
	
	
	public ImageFlowView(Application app) {
		super(app);
		
		ResourceMap resourceMap = getResourceMap();
		this.graphController = new GraphController(this);
		this.units = this.graphController.getUnitElements();
		this.connections = this.graphController.getConnections();
		
		initComponents();
	}



	private void initComponents() {
		addComponents();
		
		addMenu();
		
		// register listeners
		registerModelListeners();
	}
	

	/**
	 * Register the Modellisteners.
	 */
	private void registerModelListeners() {
		// usually on startup this is empty
		for (Node node : units) {
			((CommentNode)node).addModelListener(
					new ModelListener () {
						public void modelChanged (final Model hitModel)	{
							graphPanel.invalidate();
							setModified(true);
						}
					});
		}
		
		units.addModelListener(new ModelListener() {
			public void modelChanged(Model model) {
				graphPanel.repaint();
				setModified(true);
			}
		});
		
		connections.addModelListener(new ModelListener() {
			public void modelChanged(Model model) {
				graphPanel.repaint();
				setModified(true);
			}
		});
	}

	
	/**
	 * Adds all components of
	 */
	private void addMenu() {
		JMenuBar menuBar = new JMenuBar();
		
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(getAction("open"));
		fileMenu.add(getAction("generateMacro"));
		fileMenu.add(getAction("saveAs"));
		fileMenu.add(getAction("importGraph"));
		fileMenu.add(new JSeparator());
		fileMenu.add(new Example0_XML_Action(graphController));
		fileMenu.add(new Example1Action(graphController));
		fileMenu.add(new Example2Action(graphController));
		
		JMenu editMenu = new JMenu("Edit");
		editMenu.add(getAction("cut"));
		editMenu.add(getAction("copy"));
		editMenu.add(getAction("paste"));
		editMenu.add(getAction("remove"));
		editMenu.add(getAction("clear"));
		
		JMenu insertMenu = new InsertUnitMenu(graphPanel, unitDelegates);
		JMenu windowMenu = new JMenu("Window");
		JMenu helpMenu = new JMenu("Help");
		
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(insertMenu);
		menuBar.add(windowMenu);
		menuBar.add(helpMenu);
		
		menuBar.setVisible(true);
		getFrame().setJMenuBar(menuBar);
	}

	/**
	 * Adds all components to the Jframe
	 */
	private void addComponents() {
		
		unitDelegates = DelegatesController.getInstance().getUnitDelegates();
		
		
		getRootPane().setLayout(new BorderLayout());
		
		//working area aka graphpanel
		
		GPanelPopup popup = new GPanelPopup(unitDelegates, graphController);
		graphPanel = new GraphPanel(unitDelegates , popup);
		popup.setActivePanel(graphPanel);
		graphPanel.setSize(400, 300);
		graphPanel.setGraphController(graphController);
		graphPanel.setPreferredSize(new Dimension(400, 300));
//		graphPanel.getSelection();

		
		
		ScrollPane graphScrollpane = new ScrollPane();
		graphScrollpane.add(graphPanel);

		
		new FileDrop( null, graphPanel, /*dragBorder,*/ new FileDrop.Listener()
	        {   
				Point coordinates =new Point(75, 75);
				
				public void filesDropped( java.io.File[] files )
	            {   
	        		for( int i = 0; i < files.length; i++ )
	                {   
	        			//TODO check filetype
	        			
	            		// add Source-Units
	        			graphController.getUnitElements().add(
	        					UnitFactory.createSourceUnit(files[i].getAbsolutePath(), coordinates));
	        			graphPanel.repaint();
	                }   // end for: through each dropped file
	            }   // end filesDropped
	        }); // end FileDrop.Listener
		
		
		
		getRootPane().add(graphScrollpane, BorderLayout.CENTER);
		
		// area for selecting unit to insert them in the graphpanel
//		JPanel unitSelectionPanel = new JPanel();
//		unitSelectionPanel.setName("Insert filter");
//		
//		JPanel selectUnitPanel = new InsertUnitPanel();
//		
//		
//		JScrollPane selectUnitScrollpane = new JScrollPane(selectUnitPanel); 
//		unitSelectionPanel.add(selectUnitScrollpane);
		
		//properties of the selected node
		JPanel propertiesPanel = new JPanel();
		propertiesPanel.setName("Properties");
		JButton buttonPara = new JButton(new ShowUnitParametersAction(graphPanel.getSelection()));
		propertiesPanel.add(buttonPara, BorderLayout.SOUTH);
		
		
		// logging the history?
		JPanel logPanel = new JPanel();
		logPanel.setName("Log");
		
		
		
		// display the generated macro
		JPanel macroPanel = new JPanel();
		macroPanel.setName("Macro");
		macroPanel.setLayout(new BorderLayout());
		
		macroArea = new JTextArea();
		JScrollPane macroAreaScrollpane = new JScrollPane(macroArea);
		macroPanel.add(macroAreaScrollpane, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		
		
		JButton buttonRun = new JButton(getAction("generateMacro"));
		buttonPanel.add(buttonRun);
		
		JButton buttoncheck = new JButton(new CheckGraphAction(graphController));
		buttonPanel.add(buttoncheck);
		macroPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		JTabbedPane functionTabPane = new JTabbedPane();
//		functionTabPane.add(unitSelectionPanel);
		functionTabPane.add(propertiesPanel);
		functionTabPane.add(logPanel);
		functionTabPane.add(macroPanel);
		getRootPane().add(functionTabPane, BorderLayout.SOUTH);
//		fr.injectComponents(functionTabPane);
	}
	

	public GraphController getGraphController() {
		return graphController;
	}

	public void setGraphController(GraphController graphController) {
		this.graphController = graphController;
		this.units = this.graphController.getUnitElements();
		this.connections = this.graphController.getConnections();
		graphPanel.setGraphController(this.graphController);
		graphPanel.invalidate();
	}
	
	/**
	 *  Set the bound file property and update the GUI.
     */
    public void setFile(final File file) {
        File oldValue = this.file;
        this.file = file;
        String appId = getResourceMap().getString("Application.id");
        getFrame().setTitle(file.getName() + " - " + appId);
        firePropertyChange("file", oldValue, this.file);
    }

    /**
     * True if the file value has been modified but not saved.  The 
     * default value of this property is false.
     * <p>
     * This is a bound read-only property.  
     * 
     * @return the value of the modified property.
     * @see #isModified
     */
    public boolean isModified() { 
        return this.modified;
    }
    
    public boolean hasSelectedUnits() { 
        return this.selectedUnits;
    }
    
    /**
     *  Set the bound modified property and update the GUI.
     */
    public void setModified(final boolean modified) {
        boolean oldValue = this.modified;
        this.modified = modified;
        firePropertyChange("modified", oldValue, this.modified);
    }
    
    public void setSelectedUnits(final boolean selected) {
        boolean oldValue = this.selectedUnits;
        this.modified = selected;
        firePropertyChange("selected", oldValue, this.selectedUnits);
    }
	
	public File getFile() {
		return file;
	}
	
	
	/*
	 * Action related stuff
	 * 
	 */
	
	
	@Action public Task generateMacro() {
	    return new GenerateMacroTask(getApplication(), graphController);
	}
	
	
	@Action public Task open() {
	    JFileChooser fc = new JFileChooser();

	    Task task = null;
	    int option = fc.showOpenDialog(null);
	    if (option == JFileChooser.APPROVE_OPTION) {
	    	task = new LoadFlowGraphTask(fc.getSelectedFile());
	    }
	    return task;
	}
	
	@Action public Task preview() {
		UnitElement unit = (UnitElement) graphPanel.getSelection().get(0);
		
	    Task task = null;
	    /*if (option == JFileChooser.APPROVE_OPTION) {
	    	task = new LoadFlowGraphTask(fc.getSelectedFile());
	    }*/
	    return task;
	}
	
	@Action public Task importGraph() {
	    JFileChooser fc = new JFileChooser();

	    Task task = null;
	    int option = fc.showOpenDialog(null);
	    if (option == JFileChooser.APPROVE_OPTION) {
	    	task = new ImportGraphTask(fc.getSelectedFile());
	    }
	    return task;
	}
	
	@Action public void unbind() {
		Selection<Node> selection = graphPanel.getSelection();
		for (Node unit : selection) {
			units.unbindUnit((UnitElement)unit);	
		}
	}

	@Action public void remove() {
		Selection<Node> selection = graphPanel.getSelection();
		for (Node unit : selection) {
			graphController.removeUnit((UnitElement)unit);
		}
	}
	
	@Action public void clear() {
		this.units.clear();
	}
	
    @Action(enabledProperty = "modified")
    public Task saveAs() {
        JFileChooser fc = createFileChooser("saveAsFileChooser");
        int option = fc.showSaveDialog(getFrame());
        Task task = null;
        if (JFileChooser.APPROVE_OPTION == option) {
//            task = new SaveFileTask(fc.getSelectedFile());
        	task = new GenerateMacroTask(this.getApplication(), graphController);
        }
        return task;
    }
    
    private JFileChooser createFileChooser(String name) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(getResourceMap().getString(name + ".dialogTitle"));
        String textFilesDesc = getResourceMap().getString("txtFileExtensionDescription");
//        fc.setFileFilter(new TextFileFilter(textFilesDesc));
        return fc;
    }
	
	private javax.swing.Action getAction(String actionName) {
//		ActionMap actionMap = Application.getInstance(ImageFlow.class).getContext().getActionMap(ImageFlowView.class, this);
		ActionMap actionMap = getContext().getActionMap(ImageFlowView.class, this);
		System.out.println(actionMap.allKeys());
		initActions(actionMap);
	    return actionMap.get(actionName);
	}
	


	private void initActions(ActionMap actionMap) {
		Selection<Node> selection = graphPanel.getSelection();
		ArrayList<Node> copyList = graphController.getCopyNodesList();
		actionMap.put("cut", 
				new CutUnitAction(graphController, graphPanel));
		actionMap.put("copy", 
				new CopyUnitAction(selection, copyList));
		actionMap.put("paste", 
				new PasteUnitAction(copyList, graphPanel));


		
		actionMap.put("checkGraph", 
				new CheckGraphAction(graphController));
	}

}

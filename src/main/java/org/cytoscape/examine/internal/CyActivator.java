package org.cytoscape.examine.internal;

import org.cytoscape.examine.internal.ViewerAction;
import org.cytoscape.examine.internal.taskfactories.CommandTaskFactory;
import org.cytoscape.examine.internal.tasks.ExamineCommand;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.osgi.framework.BundleContext;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.model.events.ColumnDeletedListener;
import org.cytoscape.model.events.ColumnNameChangedListener;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.events.SessionLoadedListener;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;

import java.util.Properties;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;

/**
 * Execution body.
 */
public class CyActivator extends AbstractCyActivator {

    /**
     * Base constructor.
     */
    public CyActivator() {
        super();
    }

    /**
     * Upon bundle activation (install or startup).
     */
    public void start(BundleContext bc) {
        // Manager services.
        //CySwingApplication desktopManager = getService(bc, CySwingApplication.class);
        
        // Basic access to current and/or currently selected networks, 
        // views and rendering engines in an instance of Cytoscape.
        CyApplicationManager applicationManager = getService(bc, CyApplicationManager.class);
        
        // Access to all root networks
        CyRootNetworkManager rootNetworkManager = getService(bc, CyRootNetworkManager.class);
        
        // Access to all networks
        CyNetworkManager networkManager = getService(bc, CyNetworkManager.class);
        
        // This object manages mapping from view model to VisualStyle. 
        // User objects can access all VisualStyles and VisualMappingFunctions through this class.
        VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
        
        // The CyGroupManager maintains information about all of the groups an instance of Cytoscape.
        CyGroupManager groupManager = getService(bc, CyGroupManager.class);
        
        // An interface describing a factory used for creating CyGroup objects.
        CyGroupFactory groupFactory = getService(bc, CyGroupFactory.class);
        
        DialogTaskManager taskManager = getService(bc, DialogTaskManager.class);
        
        // Action, the group viewer
        ViewerAction viewerAction =
                new ViewerAction(applicationManager,
                                 visualMappingManager,
                                 groupManager,
                                 groupFactory);
        
        // Action, the group selector
        /*GroupsFromColumnsAction groupsAction =
                new GroupsFromColumnsAction(applicationManager,
                                            groupManager,
                                            groupFactory);*/
        
        //Store services for later references TODO: Remove redundant fields throughout app
        CyReferences.getInstance().storeReferences(networkManager, rootNetworkManager, 
        		applicationManager, groupManager, groupFactory, taskManager);
        
        // The eXamine control panel
        ControlPanel controlPanel = new ControlPanel(networkManager, rootNetworkManager, 
        		applicationManager, groupManager, groupFactory, taskManager);

        // Register it as a service.
        registerService(bc, viewerAction, CyAction.class, new Properties());
        //registerService(bc, groupsAction, CyAction.class, new Properties());
        registerService(bc, controlPanel, CytoPanelComponent.class, new Properties());
        registerService(bc, controlPanel, SetCurrentNetworkListener.class, new Properties());
        registerService(bc, controlPanel, RowsSetListener.class, new Properties());
        registerService(bc, controlPanel, ColumnNameChangedListener.class, new Properties());
        registerService(bc, controlPanel, ColumnDeletedListener.class, new Properties());
        registerService(bc, controlPanel, ColumnCreatedListener.class, new Properties());
        registerService(bc, controlPanel, NetworkDestroyedListener.class, new Properties());
        registerService(bc, controlPanel, SessionLoadedListener.class, new Properties());
        
        //Register commands to allow access via CyRest
        
		TaskFactory commandTaskFactory_GENERATE_GROUPS = new CommandTaskFactory(ExamineCommand.GENERATE_GROUPS);
		Properties props_GENERATE_GROUPS = new Properties();
		props_GENERATE_GROUPS.setProperty(COMMAND_NAMESPACE, Constants.APP_COMMAND_PREFIX);
		props_GENERATE_GROUPS.setProperty(COMMAND, ExamineCommand.GENERATE_GROUPS.toString());
		props_GENERATE_GROUPS.setProperty(COMMAND_DESCRIPTION,"[Placeholder] This command generates groups");
		registerService(bc, commandTaskFactory_GENERATE_GROUPS, TaskFactory.class, props_GENERATE_GROUPS);
    }
    
    /**
     * Cleanup module resources. (Does this work?)
     */
    @Override
    protected void finalize() throws Throwable {
        Modules.dispose();
        super.finalize();
    }
    
}

package com.generalrobotix.ui.actions;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxItemChangeListener;
import com.generalrobotix.ui.GrxObserver;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.util.MessageBundle;

public class StartSimulate implements IWorkbenchWindowActionDelegate, GrxItemChangeListener, GrxObserver {
	private IAction action_ = null;
	private GrxSimulationItem simItem_=null;
	
	public StartSimulate() {
		setUp();
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		manager_.registerItemChangeListener(this, GrxSimulationItem.class);
	}

	public void setUp(){
		if(simItem_ != null)
			simItem_.deleteObserver(this);
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		simItem_ = manager_.<GrxSimulationItem>getSelectedItem(GrxSimulationItem.class, null);
		if(simItem_!=null){
			simItem_.addObserver(this);
		}
	}
	
	public void run(IAction action) {
		action_ = action;
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		if(simItem_==null){
			simItem_ = (GrxSimulationItem)manager_.createItem(GrxSimulationItem.class, null);
			simItem_.addObserver(this);
			manager_.itemChange(simItem_, GrxPluginManager.ADD_ITEM);
			manager_.setSelectedItem(simItem_, true);
		}
		if(simItem_.isSimulating()){
			action_.setEnabled(false);
			simItem_.stopSimulation();
		}else {
			action_.setEnabled(false);
			if(!simItem_.startSimulation(true))
				action_.setEnabled(true);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {}
	public void dispose() {
	}
	public void init(IWorkbenchWindow window) {}
	
	public void setActionImage(boolean start){
		if(action_==null)
			action_ = getStartSimulationAction();
		if(start){
			action_.setToolTipText(MessageBundle.get("GrxOpenHRPView.text.stop")); //$NON-NLS-1$
			action_.setText(MessageBundle.get("GrxOpenHRPView.menu.stop")); //$NON-NLS-1$
			action_.setImageDescriptor(Activator.getDefault().getDescriptor("sim_stop.png")); //$NON-NLS-1$
		}else{	
			action_.setToolTipText(MessageBundle.get("GrxOpenHRPView.text.start")); //$NON-NLS-1$
			action_.setText(MessageBundle.get("GrxOpenHRPView.menu.start")); //$NON-NLS-1$
			action_.setImageDescriptor(Activator.getDefault().getDescriptor("sim_start.png")); //$NON-NLS-1$
		}
	}
	
	private IAction getStartSimulationAction()
	{
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for(IWorkbenchWindow w : windows){
			if(!(w instanceof ApplicationWindow))
				continue;
			ApplicationWindow window = (ApplicationWindow)w;
			ICoolBarManager coolbar = window.getCoolBarManager2();
			if(coolbar == null)
				continue;
			IContributionItem setitem = coolbar.find("com.generalrobotix.ui.actionSet");
			if(setitem != null && setitem instanceof ToolBarContributionItem)
			{
				IToolBarManager toolbar = ((ToolBarContributionItem)setitem).getToolBarManager();
				if(toolbar == null)
					continue;
				IContributionItem actitem = toolbar.find("com.generalrobotix.ui.actions.StartSimulate");
				if(actitem != null && actitem instanceof ActionContributionItem)
					return ((ActionContributionItem)actitem).getAction();
			}
		}
		return null;
	}

	public void registerItemChange(GrxBaseItem item, int event) {
		if(item instanceof GrxSimulationItem){
    		GrxSimulationItem simItem = (GrxSimulationItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(simItem_!=simItem){
    				simItem_ = simItem;
    				simItem_.addObserver(this);
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(simItem_==simItem){
	    			simItem_.deleteObserver(this);
	    			simItem_ = null;
	    		}
	    		break;
	    	default:
	    		break;
    		}
    	}
		
	}

	public void update(GrxBasePlugin plugin, Object... arg) {
		if(simItem_==plugin){
			if(arg[0].equals("StartSimulation")){
				setActionImage(true);
				action_.setEnabled(true);
			}else if(arg[0].equals("StopSimulation")){
				setActionImage(false);	
				action_.setEnabled(true);
			}
		}
	}

}
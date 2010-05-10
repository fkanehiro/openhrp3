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
	
	public StartSimulate() {
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		GrxSimulationItem simItem = (GrxSimulationItem)manager_.getItem("simulation");
		simItem.addObserver(this);
	}

	public void run(IAction action) {
		action_ = action;
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		GrxSimulationItem simItem = (GrxSimulationItem)manager_.getItem("simulation");
		if (simItem.isSimulating()){
			simItem.stopSimulation();
		}else{
			simItem.startSimulation(true);
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
		// TODO Auto-generated method stub
		
	}

	public void update(GrxBasePlugin plugin, Object... arg) {
		if(arg[0].equals("StartSimulation"))
			setActionImage(true);
		else if(arg[0].equals("StopSimulation"))
			setActionImage(false);	
	}

}
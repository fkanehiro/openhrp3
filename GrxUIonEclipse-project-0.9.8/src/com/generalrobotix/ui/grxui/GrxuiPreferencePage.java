package com.generalrobotix.ui.grxui;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import com.generalrobotix.ui.grxui.Activator;

public class GrxuiPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public GrxuiPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}
	
	public void createFieldEditors() {
		Composite comp = getFieldEditorParent();
		
		Group group0 = new Group(comp, SWT.NONE);
		group0.setText("OPENHRP Project Directory");
		GridData group0Data = new GridData();
		group0Data.horizontalSpan = 3;
		group0.setLayoutData(group0Data);
		Label label0 = new Label(group0, SWT.NONE);
		label0.setText("set (OpenHRP Install Director)/share/OpenHRP-3.1/sample/project");
		GridData label0Data = new GridData();
		label0Data.horizontalSpan = 3;
		label0.setLayoutData(label0Data);
		addField(new DirectoryFieldEditor(PreferenceConstants.PROJECT_DIR, "", group0));
		
		addField(
			new StringFieldEditor(PreferenceConstants.BIN_SFX, "script files extension:", getFieldEditorParent()));
		
		Group group1 = new Group(comp, SWT.NONE);
		group1.setText("Jython lib Directory");
		GridData group1Data = new GridData();
		group1Data.horizontalSpan = 3;
		group1.setLayoutData(group1Data);
		Label label1 = new Label(group1, SWT.NONE);
		label1.setText("set (JYTHON Install Director)/LIB when you use Jython Script");
		GridData label1Data = new GridData();
		label1Data.horizontalSpan = 3;
		label1.setLayoutData(label1Data);
		addField(new DirectoryFieldEditor(PreferenceConstants.JYTHON_LIB, "", group1));
		
	}

	public void init(IWorkbench workbench) {
	}
	
}
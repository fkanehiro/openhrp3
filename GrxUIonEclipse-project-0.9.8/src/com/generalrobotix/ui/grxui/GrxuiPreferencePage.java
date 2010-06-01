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
import com.generalrobotix.ui.util.MessageBundle;

public class GrxuiPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public GrxuiPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}
	
	public void createFieldEditors() {
		Composite comp = getFieldEditorParent();
		
		Group group2 = new Group(comp, SWT.NONE);
		group2.setText(MessageBundle.get("GrxuiPreferencePage.serverDir.title")); //$NON-NLS-1$
		GridData group2Data = new GridData();
		group2Data.horizontalSpan = 3;
		group2.setLayoutData(group2Data);
		Label label2 = new Label(group2, SWT.NONE);
		label2.setText(MessageBundle.get("GrxuiPreferencePage.serverDir.message")); //$NON-NLS-1$
		GridData label2Data = new GridData();
		label2Data.horizontalSpan = 3;
		label2.setLayoutData(label2Data);
		addField(new DirectoryFieldEditor(PreferenceConstants.SERVER_DIR, "", group2)); //$NON-NLS-1$
		
		Label gap0 = new Label(comp, SWT.NONE);
		Group group0 = new Group(comp, SWT.NONE);
		group0.setText(MessageBundle.get("GrxuiPreferencePage.projectDir.title")); //$NON-NLS-1$
		GridData group0Data = new GridData();
		group0Data.horizontalSpan = 3;
		group0.setLayoutData(group0Data);
		Label label0 = new Label(group0, SWT.NONE);
		label0.setText(MessageBundle.get("GrxuiPreferencePage.projectDir.message")); //$NON-NLS-1$
		GridData label0Data = new GridData();
		label0Data.horizontalSpan = 3;
		label0.setLayoutData(label0Data);
		addField(new DirectoryFieldEditor(PreferenceConstants.PROJECT_DIR, "", group0)); //$NON-NLS-1$
		
		Label gap1 = new Label(comp, SWT.NONE);
		GridData gap1Data = new GridData();
		gap1Data.horizontalSpan = 3;
		gap1.setLayoutData(gap1Data);
		addField(
			new StringFieldEditor(PreferenceConstants.BIN_SFX, MessageBundle.get("GrxuiPreferencePage.binSfx.title"), getFieldEditorParent())); //$NON-NLS-1$
		
		Label gap2 = new Label(comp, SWT.NONE);
		Group group1 = new Group(comp, SWT.NONE);
		group1.setText(MessageBundle.get("GrxuiPreferencePage.jythonLib.title")); //$NON-NLS-1$
		GridData group1Data = new GridData();
		group1Data.horizontalSpan = 3;
		group1.setLayoutData(group1Data);
		Label label1 = new Label(group1, SWT.NONE);
		label1.setText(MessageBundle.get("GrxuiPreferencePage.jythonLib.message")); //$NON-NLS-1$
		GridData label1Data = new GridData();
		label1Data.horizontalSpan = 1;
		label1.setLayoutData(label1Data);
		addField(new StringFieldEditor(PreferenceConstants.JYTHON_LIB, "", group1)); //$NON-NLS-1$
		
		Label gap3 = new Label(comp, SWT.NONE);
		Group group3 = new Group(comp, SWT.NONE);
		group3.setText(MessageBundle.get("GrxuiPreferencePage.initialProject.title"));	 //$NON-NLS-1$
		GridData group3Data = new GridData();
		group3Data.horizontalSpan = 3;
		group3.setLayoutData(group3Data);
		Label label3 = new Label(group3, SWT.NONE);
		label3.setText(MessageBundle.get("GrxuiPreferencePage.initialProject.message"));	//$NON-NLS-1$
		GridData label3Data = new GridData();
		label3Data.horizontalSpan = 3;
		label3.setLayoutData(label3Data);
		addField(new FileFieldEditor(PreferenceConstants.INITIALPROJECT, "", group3)); //$NON-NLS-1$
		
	}

	public void init(IWorkbench workbench) {
	}
	
}
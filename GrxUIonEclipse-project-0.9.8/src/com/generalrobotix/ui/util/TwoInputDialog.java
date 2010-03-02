package com.generalrobotix.ui.util;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TwoInputDialog extends InputDialog {
	private String dialogMessage1_=null;
	private String initialValue1_=null;
	private String value1_=null;
	private Text text1_=null;
	
	public TwoInputDialog(Shell parentShell, String dialogTitle,
			String dialogMessage0, String initialValue0, IInputValidator validator, String dialogMessage1, String initialValue1) {
		super(parentShell, dialogTitle, dialogMessage0, initialValue0, validator);
		dialogMessage1_=dialogMessage1;
		initialValue1_=initialValue1;
	}

	protected Control createDialogArea(Composite parent) {
	    Composite composite = (Composite)super.createDialogArea(parent);
	    Label label1 = new Label(composite, SWT.NONE);
	    label1.setText(dialogMessage1_);
	    text1_ = new Text(composite, SWT.BORDER);
	    text1_.setText(initialValue1_);
	    GridData gridData = new GridData();
	    gridData.horizontalAlignment=GridData.FILL;
	    text1_.setLayoutData(gridData);
	    return composite;
	}
	
	 protected void buttonPressed(int buttonId) {
		value1_=text1_.getText();
		 super.buttonPressed(buttonId);
	 }
	 
	public String[] getValues(){
		String[] values = new String[2];
		values[0]=super.getValue();
		values[1]=value1_;
		return values;
	}
	
}

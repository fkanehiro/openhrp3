package com.generalrobotix.ui.grxui;


import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.generalrobotix.ui.util.MessageBundle;

public class PreferenceFontPage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

    public PreferenceFontPage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    public void createFieldEditors() {
        Composite comp = getFieldEditorParent();

        addField(new FontFieldEditor(PreferenceConstants.FONT_TABLE, MessageBundle.get("GrxuiPreferencePage.tableFont.title"), "Preview", comp));
        addField(new FontFieldEditor(PreferenceConstants.FONT_EDITER, MessageBundle.get("GrxuiPreferencePage.editerFont.title"), "Preview", comp));
    }
    
    public void init(IWorkbench workbench) {
    }

}
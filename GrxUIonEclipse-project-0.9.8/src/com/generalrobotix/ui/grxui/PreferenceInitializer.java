package com.generalrobotix.ui.grxui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.generalrobotix.ui.grxui.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault()
				.getPreferenceStore();
		store.setDefault(PreferenceConstants.PROJECT_DIR, "");
		store.setDefault(PreferenceConstants.JYTHON_LIB, "");
		store.setDefault(PreferenceConstants.SERVER_DIR, "");
		if (System.getProperty("os.name").equals("Linux"))
			store.setDefault(PreferenceConstants.BIN_SFX, ".sh");
		else if(System.getProperty("os.name").contains("Windows"))
			store.setDefault(PreferenceConstants.BIN_SFX, ".bat");
		else
			store.setDefault(PreferenceConstants.BIN_SFX, "");
			
	}

}

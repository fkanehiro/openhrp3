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
		if (System.getProperty("os.name").equals("Linux")){
			store.setDefault(PreferenceConstants.BIN_SFX, ".sh");
			store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.LOGGIR,
					"$(HOME)/.OpenHRP-3.1/omninames-log");
		}else if(System.getProperty("os.name").contains("Windows")){
			store.setDefault(PreferenceConstants.BIN_SFX, ".bat");
			store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.LOGGIR,
					"\"$(APPDATA)/OpenHRP-3.1/omninames-log\"");
			store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.COM+0,
					"\"$(OMNI_ROOT)/bin/x86_win32/omniNames\"");
		}else{
			store.setDefault(PreferenceConstants.BIN_SFX, "");
			store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.LOGGIR,
					"");
		}
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.COM,
				"omniNames");
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.HOST,
				"localhost");
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.PORT,
				2809);
		
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.ID, 
				PreferenceConstants.ALLSERVER+PreferenceConstants.SEPARATOR+PreferenceConstants.MODELLOADER+PreferenceConstants.SEPARATOR+
				PreferenceConstants.COLLISIONDETECTORFACTORY+PreferenceConstants.SEPARATOR+PreferenceConstants.DYNAMICSSIMULATORFACTORY);
		
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.DIR, "\"\"");
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.WAITCOUNT,	"500");
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.ARGS, "\"\"");
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.COM,
				"\"\""+PreferenceConstants.SEPARATOR+"\"openhrp-model-loader\""+PreferenceConstants.SEPARATOR+
				"\"openhrp-collision-detector\""+PreferenceConstants.SEPARATOR+"\"openhrp-aist-dynamics-simulator\"");
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.AUTOSTART,	
				"false"+PreferenceConstants.SEPARATOR+"true"+PreferenceConstants.SEPARATOR+
				"true"+PreferenceConstants.SEPARATOR+"true");		
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.HASSHUTDOWN, 
				"false"+PreferenceConstants.SEPARATOR+"true"+PreferenceConstants.SEPARATOR+
				"true"+PreferenceConstants.SEPARATOR+"true");
		store.setDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.USEORB, 
				"false"+PreferenceConstants.SEPARATOR+"true"+PreferenceConstants.SEPARATOR+
				"true"+PreferenceConstants.SEPARATOR+"true");
	}

}

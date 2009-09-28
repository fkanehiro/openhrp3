/**
 * 
 */
package com.generalrobotix.ui.depends;

/**
 * @author Tawara Takafumi
 * 
 */
public interface ISwitchDependVer {
	
    /**
     * @brief  Get dependency module name 
     * @return Module name String
     */
	public String getDependencyModuleName();

    /**
     * @brief  Get dependency module version 
     * @return Module version String
     */
	public String getDependencyModuleVersion();
}

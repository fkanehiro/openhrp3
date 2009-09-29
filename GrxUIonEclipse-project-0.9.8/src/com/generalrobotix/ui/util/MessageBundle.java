/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.util;

import java.util.Locale;
import java.util.ResourceBundle;
import com.generalrobotix.ui.util.GrxDebugUtil;

public class MessageBundle {
    private static MessageBundle this_;
    protected static ResourceBundle messages_;

    private MessageBundle() {
        messages_ =
            ResourceBundle.getBundle("com.generalrobotix.messages", Locale.US);
    }

    private  MessageBundle(Locale locale) {
        messages_ = ResourceBundle.getBundle("com.generalrobotix.messages", locale);
    }

    public static final void setLocale(Locale locale) {
        if (this_ == null) {
            this_ = new MessageBundle(locale);
        } else {
            messages_ =
                ResourceBundle.getBundle("com.generalrobotix.messages", locale);
        }
    }

    public static final void setLocale(String language, String country) {
        if (this_ == null) {
            this_ = new MessageBundle(new Locale(language, country));
        } else {
            messages_ =
                ResourceBundle.getBundle(
                    "com.generalrobotix.messages",
                    new Locale(language, country)
                );
        }
    }

    public static final String get(String key) {
        if (this_ == null) {
            this_ = new MessageBundle(Locale.getDefault());
        }
        try{
        	return messages_.getString(key);
        }catch(Exception e){
        	try{
        		GrxDebugUtil.println("not found " + key + "in messages_(defaultlocale).properties");
        		return ResourceBundle.getBundle("com.generalrobotix.messages", Locale.US).getString(key);
        	}catch(Exception e1){
        		GrxDebugUtil.println("not found " + key + "in messages_en.properties");
        		return "";
        	}
        }
    }
    
    public static final ResourceBundle getMessages() { return messages_; }
} 

package com.generalrobotix.ui.util;

import java.util.*;
import java.net.*;
import javax.swing.ImageIcon;

public class IconProperties {
    protected static IconProperties this_;
    protected static ResourceBundle resource_;

    public static final int WIDTH = 27;
    public static final int HEIGHT = 27;

    private IconProperties() {
        resource_ = ResourceBundle.getBundle("resources.icon");
    }

    public static ImageIcon get(String key) {
        if (this_ == null) {
            this_ = new IconProperties();
        }

        String str = resource_.getString(key);
        if (str.equals("none")) {
            return new ImageIcon();
        } else {
            URL url = IconProperties.class.getResource(str);
            if (url == null) {
                return new ImageIcon();
            }
            return new ImageIcon(url);
        }
    }
}


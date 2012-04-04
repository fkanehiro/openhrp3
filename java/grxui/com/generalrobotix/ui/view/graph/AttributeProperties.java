/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * AttributeProperties.java
 *
 * /resources/attribute.propertiesファイルを読み込んでアトリビュートの
 * プロパティを取得し、保持します。 
 *
 * @author  Kernel, Inc.
 * @version 1.0 (Mon Sep 17 2001)
 */

package com.generalrobotix.ui.view.graph;

import java.util.*;
import java.io.IOException;
import java.net.URL;

public class AttributeProperties {
    public  static final int PROPERTY_TYPE                =  0;
    public  static final int PROPERTY_SOURCE              =  1;
    public  static final int PROPERTY_DEFAULT_VALUE       =  2;
    public  static final int PROPERTY_RECORD_LEVEL        =  3;
    public  static final int PROPERTY_SAVE                =  4;
    public  static final int PROPERTY_EDITABLE            =  5;
    public  static final int PROPERTY_VISIBLE             =  6;
    public  static final int PROPERTY_RECORD_FLAG_LOCKED  =  7;
    public  static final int PROPERTY_SAVE_FROM_WORLD     =  8;
    public  static final int PROPERTY_PARAM_NAME          =  9;
    public  static final int PROPERTY_LINKINFO            = 10;
    public  static final int PROPERTY_SAVEFLAGS           = 11;//★斉藤追加(2003/6/7)

    private static final String[] propertyString__ = {
        "type",
        "source",
        "defaultValue",
        "recordLevel",
        "save",
        "editable",
        "visible",
        "recordFlagLocked",
        "saveFromWorld",
        "paramName",
        "linkInfo",
        "saveFlags",//★斉藤追加(2003/6/7)
    };

    private static final String
        ATTRIBUTE_PROPERTIES = "/resources/attribute.properties";
    private static final String NODE_NAMES = "nodeNames";
    private static final String ATTRIBUTE_NAMES = "attributeNames";

    private static Map<String, AttributeProperties> nodeProperties__;
    private Map attributeProperties_;
    private String[] attributeNames_;

    private AttributeProperties(Map attributes, String[] attrNames) {
         attributeProperties_ = attributes;
         attributeNames_ = attrNames;
    }

    /**
     * nodeProperties__---ノード名毎にAttributePropertiesを保持するテーブルを
     * 作成します。
     */
    private static void _makeNodeProperties() {
        nodeProperties__ = new HashMap<String, AttributeProperties>();
        URL url = AttributeProperties.class.getResource(ATTRIBUTE_PROPERTIES);
        Properties properties = new Properties();
        try {
            properties.load(url.openStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        StringTokenizer nodeNames =
            new StringTokenizer(properties.getProperty(NODE_NAMES), ",");

        // ノード名でループ
        while (nodeNames.hasMoreTokens()) {
            String nodeName = nodeNames.nextToken();
            Map<String, String> attributes = new HashMap<String, String>();

            StringBuffer attrNamesKey = new StringBuffer(nodeName);
            attrNamesKey.append(".");
            attrNamesKey.append(ATTRIBUTE_NAMES);
            StringTokenizer attrNameTokenizer =
                new StringTokenizer(
                    properties.getProperty(attrNamesKey.toString()),
                    ","
                );
            String[] attrNames = new String[attrNameTokenizer.countTokens()];

            // アトリビュートの数だけループ
            for (int i = 0; attrNameTokenizer.hasMoreTokens(); i ++) {
                attrNames[i] = attrNameTokenizer.nextToken();
                for (int j = 0; j < propertyString__.length; j ++) {
                    StringBuffer key = new StringBuffer(attrNames[i]);
                    key.append('.');
                    key.append(propertyString__[j]);

                    StringBuffer propertyKey = new StringBuffer(nodeName);
                    propertyKey.append('.');
                    propertyKey.append(key.toString());
                    String value = properties.getProperty(propertyKey.toString());

                    attributes.put(key.toString(), value);
                }
            }
            nodeProperties__.put(
                nodeName,
                new AttributeProperties(attributes, attrNames)
            );
        }
    }

    public static AttributeProperties getProperties(String nodeName) {
        if (nodeProperties__ == null) {
            _makeNodeProperties();
        }

        return (AttributeProperties)nodeProperties__.get(nodeName);
    }

    public String getProperty(String attrName, int property) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(attrName);
        buffer.append('.');
        buffer.append(propertyString__[property]);
        return (String)attributeProperties_.get(buffer.toString());
    }

    public StringExchangeable createAttributeValue(String attrName) {
        String initialValue = getProperty(attrName, PROPERTY_DEFAULT_VALUE);
        return createAttributeValue(attrName, initialValue);
    }

    public StringExchangeable createAttributeValue(
        String attrName,
        String initialValue
    ) {
        if (initialValue == null) return null;

        String typeString = getProperty(attrName, PROPERTY_TYPE);
        if (typeString.equals("Integer")) {
            return new SEInteger(initialValue);
        } else if (typeString.equals("IntArray")) {
            return new SEIntArray(initialValue);
        } else if (typeString.equals("Double")) {
            return new SEDouble(initialValue);
        } else if (typeString.equals("DoubleArray")) {
            return new SEDoubleArray(initialValue);
        } else if (typeString.equals("String")) {
            return new SEString(initialValue);
        } else if (typeString.equals("Boolean")) {
            return new SEBoolean(initialValue);
        } else if (typeString.equals("Enumeration")) {
            return new SEEnumeration(initialValue);
        } else if (typeString.equals("AxisAngle")) {
            return new SEAxisAngle(initialValue);
        } else if (typeString.equals("Translation")) {
            return new SETranslation(initialValue);
        }
        return null;
    }

    public int getAttributeFlag(String attrName) {
        int flag = 0;

        String recordLevel = getProperty(attrName, PROPERTY_RECORD_LEVEL);
        if (recordLevel.equals("optional")) {
            flag |= Attribute.RECORDABLE;
        } else if (recordLevel.equals("required")) {
            flag |= (
                Attribute.RECORDABLE |
                Attribute.RECORD_REQUIRED |
                Attribute.MUST_RECORD
            );
        }

        String save = getProperty(attrName, PROPERTY_SAVE);
        if (save != null && save.equals("true")) {
            flag |= Attribute.SAVE_REQUIRED;
        }

        //★斉藤追加(2003/6/7)
        String saveFlags = getProperty(attrName, PROPERTY_SAVEFLAGS);
        if (saveFlags != null && saveFlags.equals("true")) {
            flag |= Attribute.SAVE_FLAGS_REQUIRED;
        }


        String editable = getProperty(attrName, PROPERTY_EDITABLE);
        if (editable.equals("true")) {
            flag |= Attribute.EDITABLE;
        }

        String visible =  getProperty(attrName, PROPERTY_VISIBLE);
        if (visible == null || visible.equals("true")) {
            flag |= Attribute.VISIBLE;
        }

        String recordFlagLocked =
            getProperty(attrName, PROPERTY_RECORD_FLAG_LOCKED);
        if (recordFlagLocked != null && recordFlagLocked.equals("true")) {
            flag |= Attribute.RECORD_FLAG_LOCKED;
        }

        String saveFromWorld =
            getProperty(attrName, PROPERTY_SAVE_FROM_WORLD);
        if (saveFromWorld != null && saveFromWorld.equals("true")) {
            flag |= Attribute.SAVE_FROM_WORLD;
        }

        return flag;
    }

    public String[] getAttributeNames() { return attributeNames_; }
}

package com.generalrobotix.ui.view.graph;

import java.awt.datatransfer.*;
import java.io.*;

public class AttributeInfo implements Transferable, Serializable {
    public static final DataFlavor dataFlavor
        = new DataFlavor(AttributeInfo.class, null);

    public final String nodeType;
    public final String objectName;
    public final String nodeName;
    public final String attribute;
    public final int length;
    public final String fullAttributeName;

    public AttributeInfo(
        String nodeType,
        String objectName,
        String nodeName,
        String attribute,
        int length
    ) {
        this.nodeType   = nodeType;
        this.objectName = objectName;
        this.nodeName   = nodeName;
        this.attribute  = attribute;
        this.length     = length;
        fullAttributeName = nodeType + "." + attribute;
    }

    /**
     *
     * @return
     */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{dataFlavor};
    }

    /**
     *
     * @param   flavor
     * @return
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return (flavor.equals(dataFlavor));
    }

    /**
     *
     * @param   flavor
     * @return
     */
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
        if (flavor.equals(dataFlavor)) {
            return this;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /**
     * 文字列化
     *
     * @return  文字列表現
     */
    public String toString() {
        StringBuffer sb;
        if (objectName == null) {
            sb = new StringBuffer();
        } else {
            sb = new StringBuffer(objectName);
            sb.append(".");
        }
        sb.append(nodeName);
        sb.append(".");
        sb.append(attribute);
        sb.append(" (");
        sb.append(nodeType);
        sb.append(".");
        sb.append(attribute);
        if (length > 0) {
            sb.append("[");
            sb.append(length);
            sb.append("]");
        }
        sb.append(")");

        return sb.toString();
    }
}

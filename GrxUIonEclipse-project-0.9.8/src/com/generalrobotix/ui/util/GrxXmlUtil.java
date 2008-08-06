/*
 *  GrxXmlUtil.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro kawasumi (GeneralRobotix, Inc)
 */

package com.generalrobotix.ui.util;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GrxXmlUtil {
	public static Document doc_ = null;
	public static Element root_ = null;
	private static String fname_ = "property.xml";

	public static void initialize(String fname) {
		if (!fname.equals(fname_)) {
			fname_ = fname;
			root_ = null;
			update();
		}
	}

	public static void update() {
		try {
			DocumentBuilderFactory dbfactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = dbfactory.newDocumentBuilder();
			doc_ = builder.parse(new File(fname_));
			root_ = doc_.getDocumentElement();
		} catch (ParserConfigurationException e) {
			GrxDebugUtil.printErr("getRoot:", e);
		} catch (SAXException e) {
			GrxDebugUtil.printErr("getRoot:", e);
		} catch (IOException e) {
			GrxDebugUtil.printErr("getRoot:", e);
		}
	}

	public static NodeList getPropertyElements(String tag) {
		if (root_ == null)
			update();
		return root_.getElementsByTagName(tag);
	}

	public static Element getElement(String tag, String attrName, String attrValue) {
		if (root_ == null)
			update();
		return getElement(root_, tag, attrName, attrValue);
	}

	public static Element getElement(Element e, String tag, String attrName, String attrValue) {
		NodeList l = e.getElementsByTagName(tag);
		for (int i = 0; i < l.getLength(); i++) {
			Element ret = (Element) l.item(i);
			String mode = ret.getAttribute(attrName);
			if (mode.equals(attrValue)) {
				return ret;
			}
		}
		return null;
	}

	public static Element getElement(String[] path) {
		if (root_ == null)
			update();
		Element e = root_;
		for (int i = 0; i < path.length; i++) {
			if (e == null)
				return null;
			NodeList l = e.getElementsByTagName(path[i]);
			if (l != null)
				e = (Element) l.item(0);
		}
		return e;
	}

	public static double[] getXYZDouble(String[] path) {
		double[] ret = null;
		Element e = getElement(path);
		if (e != null) {
			ret = new double[3];
			try {
				ret[0] = Double.parseDouble(e.getAttribute("x"));
				ret[1] = Double.parseDouble(e.getAttribute("y"));
				ret[2] = Double.parseDouble(e.getAttribute("z"));
			} catch (Exception ex) {
				return null;
			}
		}
		return ret;
	}

	public static Vector3d getVector3d(String[] path) {
		double[] val = getXYZDouble(path);
		if (val != null)
			return new Vector3d(val);
		return null;
	}

	public static void setXYZDouble(String[] path, double[] val) {
		Element e = getElement(path);
		if (e != null && val.length == 3) {
			e.setAttribute("x", String.valueOf(val[0]));
			e.setAttribute("y", String.valueOf(val[1]));
			e.setAttribute("z", String.valueOf(val[2]));
		}
	}

	public static Quat4d getQuat4d(String[] path) {
		double[] val = getQuatDouble(path);
		if (val != null)
			return new Quat4d(val);
		return null;
	}

	public static double[] getQuatDouble(String[] path) {
		double[] ret = null;
		Element e = getElement(path);
		if (e != null) {
			ret = new double[4];
			try {
				ret[0] = Double.parseDouble(e.getAttribute("q1"));
				ret[1] = Double.parseDouble(e.getAttribute("q2"));
				ret[2] = Double.parseDouble(e.getAttribute("q3"));
				ret[3] = Double.parseDouble(e.getAttribute("q4"));
			} catch (Exception ex) {
				return null;
			}
		}
		return ret;
	}

	public static void setQuatDouble(String[] path, double[] val) {
		Element e = getElement(path);
		if (e != null && val.length == 4) {
			e.setAttribute("q1", String.valueOf(val[0]));
			e.setAttribute("q2", String.valueOf(val[1]));
			e.setAttribute("q3", String.valueOf(val[2]));
			e.setAttribute("q4", String.valueOf(val[3]));
		}
	}

	public static Double getDouble(String[] path, String atr) {
		return getDouble(getElement(path), atr);
	}

	public static Double getDouble(Element e, String atr) {
		String str = expandEnvVal(e.getAttribute(atr));
		if (!str.equals("")) {
				try {
					return Double.parseDouble(str);
				} catch (Exception ex) {
					return null;
				}
		}
		return null;
	}

	public static void setDouble(String[] path, String atr, double val) {
		setDouble(getElement(path), atr, val);
	}

	public static void setDouble(Element e, String atr, double val) {
		if (e != null)
			e.setAttribute(atr, String.valueOf(val));
	}

	public static Integer getInteger(String[] path, String atr, int defaultValue) {
		return getInteger(getElement(path), atr, defaultValue);
	}

	public static Integer getInteger(Element e, String atr, int defaultValue) {
        if (e != null) {
            String str = expandEnvVal(e.getAttribute(atr));
            if (!str.equals("")) {
                try {
                    return Integer.parseInt(str);
                } catch (Exception ex) {
                    return defaultValue;
                }
            }
        }
		return defaultValue;
	}

	public static void setInteger(String[] path, String atr, int val) {
		setInteger(getElement(path), atr, val);
	}

	public static void setInteger(Element e, String atr, int val) {
		if (e != null) {
			e.setAttribute(atr, String.valueOf(val));
		}
	}

	public static Boolean getBoolean(String[] path, String atr,
			boolean defaultValue) {
		return getBoolean(getElement(path), atr, defaultValue);
	}

	public static Boolean getBoolean(Element e, String atr, boolean defaultValue) {
		String str = expandEnvVal(e.getAttribute(atr));
 		if (!str.equals("")) {
 			try {
				if (!str.equals("")) {
					return Boolean.parseBoolean(str);
				} 
			} catch (Exception ex) {
 				return defaultValue;
 			}
		}
		return defaultValue;
	}

	public static void setBoolean(String[] path, String atr, boolean b) {
		setBoolean(getElement(path), atr, b);
	}

	public static void setBoolean(Element e, String atr, boolean b) {
		if (e != null)
			e.setAttribute(atr, String.valueOf(b));
	}

	public static String getString(String[] path, String atr,
			String defaultValue) {
		return getString(getElement(path), atr, defaultValue);
	}

	public static String getString(Element e, String atr, String defaultValue) {
		if (e == null)
			return defaultValue;

		String str = e.getAttribute(atr);
		if (str.equals(""))
			return defaultValue;

		return expandEnvVal(str);
	}

	public static String expandEnvVal(String str) {
		if (str == null)
			return null;
		for (int i = 0;; i++) {
			int idx1, idx2;
			if ((idx1 = str.indexOf("$(")) == -1)
				break;
			if ((idx2 = str.indexOf(")", idx1)) != -1) {
				String key = str.substring(idx1 + 2, idx2);
				String val = System.getProperty(key, System.getenv(key));
				if (val == null)
					val = "";
				str = str.replace("$(" + key + ")", val);
			}
		}
		return str;
	}

	public static void setString(String[] path, String atr, String str) {
		setString(getElement(path), atr, str);
	}

	public static void setString(Element e, String atr, String str) {
		if (e != null)
			e.setAttribute(atr, str);
	}

	public static Dimension getSize(String[] path, Dimension defaultValue) {
		return getSize(getElement(path), defaultValue);
	}

	public static Dimension getSize(Element e, Dimension defaultValue) {
		if (e != null) {
			Dimension ret = new Dimension();
			ret.width = getInteger(e, "width", defaultValue.width);
			ret.height = getInteger(e, "height", defaultValue.height);
			return ret;
		}
		return defaultValue;
	}

	public static void setSize(Element e, Dimension d) {
		if (e != null) {
			setInteger(e, "width", d.width);
			setInteger(e, "height", d.height);
		}
	}

	public static Element appendNewElement(Node n, String tagName, int depth) {
		return appendNewElement(doc_, n, tagName, depth);
	}
	
	public static Element appendNewElement(Document doc, Node n, String tagName, int depth) {
		String t = null;
		if (n == null)
			n = root_;
		if (!n.hasChildNodes() || n == root_) {
			t = "\n";
			for (int i = 0; i < depth; i++)
				t += "    ";
		} else
			t = "    ";
		n.appendChild(doc.createTextNode(t));

		Element e = doc.createElement(tagName);
		n.appendChild(e);

		t = "\n";
		for (int i = 0; i < depth - 1; i++)
			t += "    ";
		n.appendChild(doc.createTextNode(t));

		return e;
	}
	
	public static Element createElement(Document doc, String tagName) {
		return createElement(doc_, tagName);
	}

	public static Element createElement(String tagName) {
		return doc_.createElement(tagName);
	}

	public static void store() {
		store(fname_);
	}
	
	public static void store(String fname) {
		store(doc_, fname);
	}

	public static void store(Document doc, String fname) {
		//File f = new File(fname_);
		//f.renameTo(new File(fname_+"~"));
		TransformerFactory tff = TransformerFactory.newInstance();
		try {
			Transformer tf = tff.newTransformer();
			javax.xml.transform.dom.DOMSource src = new DOMSource();
			src.setNode(doc);
			javax.xml.transform.stream.StreamResult target = new javax.xml.transform.stream.StreamResult();
			target.setOutputStream(new FileOutputStream(new File(fname)));
			tf.transform(src, target);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
}

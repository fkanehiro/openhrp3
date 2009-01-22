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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;

import jp.go.aist.hrp.simulator.ServerObject;
import jp.go.aist.hrp.simulator.ServerObjectHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import com.sun.org.apache.xml.internal.serialize.Serializer;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

@SuppressWarnings("serial")
public class GrxServerManagerConfigXml {
    private File xmlFile = null;
    private static Document document = null;         
    private static Element elementRoot = null;
    
    public GrxServerManagerConfigXml( File refFile){
        xmlFile = refFile;
        try {
            initXml();
        } catch (ParserConfigurationException e) {
            GrxDebugUtil.printErr("GrxServerManagerConfigXml.initXml:", e);
        } catch (SAXException e) {
            GrxDebugUtil.printErr("GrxServerManagerConfigXml.initXml:", e);
        } catch (IOException e) {
            GrxDebugUtil.printErr("GrxServerManagerConfigXml.initXml:", e);
        } catch (Exception ex) {
            GrxDebugUtil.printErr("GrxServerManagerConfigXml.initXml:", ex);
        }
    }
    
    public String getRootElementName(){
        return elementRoot.getTagName();
    }
    
    public boolean isExistServer(String name){
        boolean ret = false;
        NodeList localList = elementRoot.getElementsByTagName("process");
        for (int i = 0; i < localList.getLength(); ++i ){
            NamedNodeMap hoge = localList.item(i).getAttributes();
              if( hoge.getNamedItem("id").getNodeValue().equals(name) ){
                ret = true;
                break;
            }
        }
        return ret; 
    }
    
    //server.nameがnameに等しいserverエレメントのノードを取得
    private Node getServerNode(String name){
        Node ret = null;
        NodeList localList = elementRoot.getElementsByTagName("process");
        for (int i = 0; i < localList.getLength(); ++i ){
            NamedNodeMap nodeMap = localList.item(i).getAttributes();
            if( nodeMap.getNamedItem("id").getNodeValue().equals(name) ){
                ret = localList.item(i);
                break;
            }
        }
        return ret;
    }

    public ProcessInfo getServerInfo(int index){
        ProcessInfo ret = null;
        NodeList localList = elementRoot.getElementsByTagName("process");
        if( index >= localList.getLength())
        	return ret ;
		Node node = localList.item(index);
        ret = new ProcessInfo();
        ret.id = GrxXmlUtil.getString((Element)node, "id", "");
        if( ret.id.equals(""))
        	return ret;
        ret.com.add( GrxXmlUtil.getString((Element)node , "com" , ""));
        ret.args =  GrxXmlUtil.getString((Element)node ,"args" , "");
        ret.autoStart = GrxXmlUtil.getBoolean((Element)node , "autostart", false);
        ret.useORB = GrxXmlUtil.getBoolean((Element)node , "useORB", false);
        return ret;
    }

    public void setServerNode(ProcessInfo refInfo){
    	Node node = getServerNode(refInfo.id);
    	if( node != null){
    	    GrxXmlUtil.setString((Element)node ,"com" ,refInfo.com.get(0));
            GrxXmlUtil.setString((Element)node , "args" ,refInfo.args);
            GrxXmlUtil.setBoolean((Element)node , "autostart" , refInfo.autoStart);
            GrxXmlUtil.setBoolean((Element)node , "useORB" ,refInfo.useORB);
        } else {
            createServerNode( refInfo );
        }
    }

    public void SaveServerInfo(){
        TransformerFactory tff = TransformerFactory.newInstance();
        try {
            DOMSource    source = new DOMSource(document);
            StringWriter outWriter = new StringWriter();
            Transformer tf = tff.newTransformer();
            //出力形式設定
            tf.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
            tf.setOutputProperty( javax.xml.transform.OutputKeys.METHOD, "xml");
            tf.setOutputProperty( OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4" );
            javax.xml.transform.stream.StreamResult target = new javax.xml.transform.stream.StreamResult( new FileOutputStream(xmlFile) );
            tf.transform(source, target);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
    
    public String getElementVal(String name, String element){
        String ref = null;
        NodeList localList = elementRoot.getElementsByTagName("process");
        for (int i = 0; i < localList.getLength(); ++i ){
            NamedNodeMap nodeMap = localList.item(i).getAttributes();
            if( nodeMap.getNamedItem(name).getNodeValue().equals(name) ){
                ref = nodeMap.getNamedItem(element).getNodeValue();
                break;
            }
        }
        return ref; 
    }

    // Xml ファイルハンドルの初期化
    private void initXml() throws Exception{
    	if( elementRoot != null )
    		return ;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();   
        DocumentBuilder builder = factory.newDocumentBuilder();         
        builder.setErrorHandler(new DefaultHandler());
        
        if( xmlFile.exists() ){
            //ファイルが存在するとき読み込み
            document = builder.parse(xmlFile);
        } else {
            //ファイルが存在しないとき新規作成
            DOMImplementation domImpl=builder.getDOMImplementation();
            document = domImpl.createDocument( "", "grxui", null );
        }
        elementRoot = document.getDocumentElement();
    }
    
    //server エレメントの生成
    private void createServerNode( ProcessInfo refInfo ){
        
        Element localElement = document.createElement("process");
        localElement.setAttribute( "id", refInfo.id );
        localElement.setAttribute( "com", refInfo.com.get(0));
        localElement.setAttribute( "args", refInfo.args );
        localElement.setAttribute( "autostart", Boolean.toString(refInfo.autoStart) );
        localElement.setAttribute( "useORB", Boolean.toString(refInfo.useORB) );
        elementRoot.appendChild(localElement);
    }

    //server エレメントの削除
    public static void deleteServerNode( ProcessInfo refInfo ){
        
        NodeList localList = elementRoot.getElementsByTagName("process");
        for( int i = 0 ; i < localList.getLength() ; i++)
        {
        	NamedNodeMap nodeMap ;
        	Node node = localList.item(i);
        	nodeMap = node.getAttributes();
        	if( nodeMap.getNamedItem("id").getNodeValue().equals(refInfo.id))
        	{
        		node.getParentNode().removeChild(node);
        		break ;
        	}
        }
    }
    public static void insertServerNode( ProcessInfo refInfo , ProcessInfo newInfo){
        
        NodeList localList = elementRoot.getElementsByTagName("process");
        for( int i = 0 ; i < localList.getLength() ; i++)
        {
        	NamedNodeMap nodeMap ;
        	Node node = localList.item(i);
        	nodeMap = node.getAttributes();
        	if( nodeMap.getNamedItem("id").getNodeValue().equals(refInfo.id))
        	{
                Element localElement = document.createElement("process");
                localElement.setAttribute( "id", newInfo.id );
                localElement.setAttribute( "com", newInfo.com.get(0));
                localElement.setAttribute( "args", newInfo.args );
                localElement.setAttribute( "autostart", Boolean.toString(newInfo.autoStart) );
                localElement.setAttribute( "useORB", Boolean.toString(newInfo.useORB) );
        		node.getParentNode().insertBefore( localElement , node);
        		break ;
        	}
        }
    }
    public static void addServerNode( ProcessInfo refInfo , ProcessInfo newInfo){
        
        NodeList localList = elementRoot.getElementsByTagName("process");
        for( int i = 0 ; i < localList.getLength() ; i++)
        {
        	NamedNodeMap nodeMap ;
        	Node node = localList.item(i);
        	nodeMap = node.getAttributes();
        	if( nodeMap.getNamedItem("id").getNodeValue().equals(refInfo.id))
        	{
                Element localElement = document.createElement("process");
                localElement.setAttribute( "id", newInfo.id );
                localElement.setAttribute( "com", newInfo.com.get(0));
                localElement.setAttribute( "args", newInfo.args );
                localElement.setAttribute( "autostart", Boolean.toString(newInfo.autoStart) );
                localElement.setAttribute( "useORB", Boolean.toString(newInfo.useORB) );
        		if( i == (localList.getLength() -1))
        		{
        			node.getParentNode().appendChild( localElement );
        		}
        		else{
        			node = localList.item(i+1);
        			node.getParentNode().insertBefore( localElement , node);
        		}
        		break ;
        	}
        }
    }
}

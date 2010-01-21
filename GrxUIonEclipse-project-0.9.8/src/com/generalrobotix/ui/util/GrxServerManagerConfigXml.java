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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.StringWriter;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

@SuppressWarnings("serial")
public class GrxServerManagerConfigXml {
    private static File xmlFile = null;
    private static Document document = null;         
    private static Element elementRoot = null;
    private static String  serverInfoDefaultDir_  = "";
    private static int     serverInfoDefaultWaitCount_ = 0;
    private final static String DEFAULT_NAME_SERVER_ID_ = "NameServer";
    private static final int    DEFAULT_NAMESERVER_PORT = 2809;
    private static final String DEFAULT_NAMESERVER_HOST = "localhost";
    private static int     NAME_SERVER_PORT_ = DEFAULT_NAMESERVER_PORT;
    private static String  NAME_SERVER_HOST_ = DEFAULT_NAMESERVER_HOST;
    private static String  NAME_SERVER_LOG_DIR_ = "";
    
    public static String getDefaultDir(){
        return serverInfoDefaultDir_;
    }
    public static int getDefaultWaitCount(){
        return serverInfoDefaultWaitCount_;
    }
    
    public static int getNameServerPort(){
        return NAME_SERVER_PORT_;
    }
    
    public static void setNameServerPort(int port){
        NAME_SERVER_PORT_ = port; 
    }
    
    public static String getNameServerHost(){
        return NAME_SERVER_HOST_;
    }
    
    public static void setNameServerHost(String host){
        NAME_SERVER_HOST_ = host;
    }
    
    public static String getNameServerLogDir(){
        return NAME_SERVER_LOG_DIR_;
    }
    
    public static void setElementRoot(Element root){
    	elementRoot = root;
    	NodeList localList = elementRoot.getElementsByTagName("process");
        if(localList != null){
            for(int i = 0; i < localList.getLength(); ++i){
                Node localNode = localList.item(i);
                if( GrxXmlUtil.getStringNoexpand((Element)localNode, "id", "").trim().equals("") ){
                    serverInfoDefaultDir_ = GrxXmlUtil.getStringNoexpand((Element)localNode ,"dir" , "").trim();
                    serverInfoDefaultWaitCount_ = GrxXmlUtil.getInteger((Element)localNode, "waitcount", 0);
                }
            }
            
        }
    }
    
    public static void setFileName( File refFile){
        xmlFile = refFile;
    }
    
    public static String getRootElementName(){
        return elementRoot.getTagName();
    }
    
    public static boolean isExistServer(String name){
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
    private static Node getServerNode(String name){
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
    
    //NameServerエレメントのノードを取得
    private static Node getNameServerNode(){
        NodeList localList = elementRoot.getElementsByTagName("nameserver");
        return localList.getLength() > 0 ? localList.item(0) : null;
    }
    

    public static ProcessInfo getServerInfo(int index){
        ProcessInfo ret = null;
        NodeList localList = elementRoot.getElementsByTagName("process");
        if( index >= localList.getLength())
        	return ret ;
		Node node = localList.item(index);
        ret = new ProcessInfo();
        ret.id = GrxXmlUtil.getStringNoexpand((Element)node, "id", "").trim();
        if( ret.id.equals("")){
        	return ret;
        }
        ret.com.add( GrxXmlUtil.getStringNoexpand((Element)node , "com" , "").trim());
        ret.args =  GrxXmlUtil.getStringNoexpand((Element)node ,"args" , "").trim();
        ret.autoStart = GrxXmlUtil.getBoolean((Element)node , "autostart", false);
        ret.useORB = GrxXmlUtil.getBoolean((Element)node , "useORB", false);
        ret.waitCount = GrxXmlUtil.getInteger((Element)node, "waitcount", serverInfoDefaultWaitCount_);
        ret.dir = GrxXmlUtil.getStringNoexpand((Element)node , "dir" , serverInfoDefaultDir_).trim();
        ret.isCorbaServer = GrxXmlUtil.getBoolean((Element)node, "iscorbaserver", false);
        ret.hasShutdown = GrxXmlUtil.getBoolean((Element)node, "hasshutdown", false);
        ret.doKillall = GrxXmlUtil.getBoolean((Element)node, "dokillall", false);
        ret.autoStop = GrxXmlUtil.getBoolean((Element)node, "autostop", true);        
        String str = null;
        for (int j = 0; !(str = GrxXmlUtil.getString((Element)node, "env" + j, "")).equals(""); j++) {
            ret.env.add(str);
        }
        return ret;
    }

    public static ProcessInfo getNameServerInfo(){
        ProcessInfo ret = null;
        NodeList localList = elementRoot.getElementsByTagName("nameserver");
        Node node = localList.item(0);
        ret = new ProcessInfo();
        ret.id = GrxXmlUtil.getStringNoexpand((Element)node, "id", DEFAULT_NAME_SERVER_ID_).trim();
        NAME_SERVER_LOG_DIR_ = GrxXmlUtil.getString((Element)node, "logdir", "").trim();
        
        if(!NAME_SERVER_LOG_DIR_.isEmpty()){
            String localStr = NAME_SERVER_LOG_DIR_.replaceFirst("^\"", "");
            File logDir = new File(localStr.replaceFirst("\"$", ""));
            if(!logDir.exists()){
                logDir.mkdirs();
            }
        }
        
        NAME_SERVER_PORT_ = GrxXmlUtil.getInteger((Element)node, "port", NAME_SERVER_PORT_);
        NAME_SERVER_HOST_ = GrxXmlUtil.getStringNoexpand((Element)node, "host", NAME_SERVER_HOST_);
        ret.args = "-ORBendPointPublish giop:tcp:"+ NAME_SERVER_HOST_ + ": -start " + 
                    Integer.toString(NAME_SERVER_PORT_) + " -logdir " + NAME_SERVER_LOG_DIR_;
        ret.com.add(GrxXmlUtil.getString((Element)node , "com" , "").trim() + " " + ret.args);
        ret.autoStart = true;
        ret.waitCount = GrxXmlUtil.getInteger((Element)node, "waitcount", serverInfoDefaultWaitCount_);
        ret.dir = GrxXmlUtil.getStringNoexpand((Element)node , "dir" , serverInfoDefaultDir_).trim();
        ret.isCorbaServer = GrxXmlUtil.getBoolean((Element)node, "iscorbaserver", false);
        ret.hasShutdown = GrxXmlUtil.getBoolean((Element)node, "hasshutdown", false);
        ret.doKillall = GrxXmlUtil.getBoolean((Element)node, "dokillall", false);
        ret.autoStop = GrxXmlUtil.getBoolean((Element)node, "autostop", true);
        String str = null;
        for (int j = 0; !(str = GrxXmlUtil.getString((Element)node, "env" + j, "")).equals(""); j++) {
            ret.env.add(str);
        }
        return ret;
    }
    
    public static void setServerNode(ProcessInfo refInfo){
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
    
    public static void setNameServer(int port, String host){
        NAME_SERVER_PORT_ = port;
        NAME_SERVER_HOST_ = host;
        setNameServerNode(port, host);
    }
    
    private static void setNameServerNode(int port, String host){
        Node node = getNameServerNode();
        if( node != null){
            GrxXmlUtil.setInteger((Element)node, "port", port);
            GrxXmlUtil.setString((Element)node, "host", host);
        }
    }

    public static void SaveServerInfo(int port, String host){
        setNameServerNode(port, host);
        TransformerFactory tff = TransformerFactory.newInstance();
        try {
            DOMSource    source = new DOMSource(elementRoot.getOwnerDocument());
            Transformer tf = tff.newTransformer();
            //出力形式設定
            tf.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
            tf.setOutputProperty( javax.xml.transform.OutputKeys.METHOD, "xml");
            tf.setOutputProperty( OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4" );
            javax.xml.transform.stream.StreamResult target = new javax.xml.transform.stream.StreamResult( new FileOutputStream(xmlFile) );
            tf.transform(source, target);
            target.getOutputStream().close();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch( IOException e){
        	e.printStackTrace();
        }
    }
    
    public static String getElementVal(String name, String element){
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

    public static String getNameServerElement(String element){
        String ret = null;
        NodeList localList = elementRoot.getElementsByTagName("nameserver");
        Node nodeNameserver = localList.item(0);
        if( nodeNameserver != null ){
            NamedNodeMap nodeMap = nodeNameserver.getAttributes();
            Node nodeElement = nodeMap.getNamedItem(element);
            if( nodeElement != null ){
                ret = nodeElement.getNodeValue();
            }
        }
        return ret;
    }
    
    
    
    
    /**
     * @brief grxuirc.xmlのversion比較
     *         具体的にはgrxuiタグのversionエレメントを比較
     *         
     * @param cmpOld   古いと疑わしい比較先File
     * @param cmpNew   比較元のFile
     * @return boolean
     *          true:更新必要
     *          false:更新不要
     */
    public static boolean isUpdatedVersion(File cmpOld, File cmpNew){
        boolean ret = false;
        try{
            
            String verOld = getGrxUIXmlVersion(cmpOld);
            String verNew = getGrxUIXmlVersion(cmpNew);
            
            //compare
            String [] splitVerOld = verOld.split("\\.");
            String [] splitVerNew = verNew.split("\\.");
            for(int i = 0; i < splitVerOld.length && i < splitVerNew.length; ++i){
                if( Integer.valueOf(splitVerOld[i]) < Integer.valueOf(splitVerNew[i]) ){
                    ret = true;
                    break;
                }
            }
            if (ret == false){
                if(splitVerOld.length < splitVerNew.length){
                    ret = true;
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }
    
    /**
     * @brief grxuirc.xmlのversion取得
     *         具体的にはgrxuiタグのversionエレメントを取得
     *         fileのversionエレメントがない場合は
     *         String "0.0" を返す
     * @param File 取得したいgrxuirc.xmlのFile
     * @return String バージョン番号
     */
    public static String getGrxUIXmlVersion(File file){
        String ret = "0.0";
        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            Document doc = builder.parse(file);
            NodeList listNode = doc.getChildNodes();
            for(int i = 0; i < listNode.getLength(); ++i){
                Node localNode = listNode.item(i);
                if( localNode.getNodeName().equals("grxui") ){
                    Node versionNode = localNode.getAttributes().getNamedItem("version");
                    if( versionNode != null ){
                        ret = versionNode.getNodeValue(); 
                    }
                    break;
                }
            }
        } catch(Exception ex){
            ex.printStackTrace();
        }
        return ret;
    }
    
    //server エレメントの生成
    private static void createServerNode( ProcessInfo refInfo ){
        
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

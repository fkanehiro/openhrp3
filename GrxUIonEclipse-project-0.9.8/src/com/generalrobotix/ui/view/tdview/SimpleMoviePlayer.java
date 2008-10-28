/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.view.tdview;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import com.generalrobotix.ui.util.ErrorDialog;
import com.sun.image.codec.jpeg.*; 

/**
 * 動画再生アプリケーション
 *
 * @author Keisuke Saito
 * @version 1.0 (2000/12/20)
 */
public class SimpleMoviePlayer extends JFrame implements ControllerListener {
    
    public boolean appFlag_=false;//単体アプリとして起動してるかflag

    private Component cmpVisual_=null;// 画面
    private Component cmpOpe_=null;// 操作板
    private Processor p_=null;// プロセッサ
    private FramePositioningControl frameCtrl_=null;// 位置コントロール
    private FrameGrabbingControl frameGrabCtrl_=null;// 画面いただきコントロール
    // 待ち用
    private Object waitSync = new Object();
    private  boolean stateTransitionOK = true;
    //表示用
    private final String STR_TITLE_="SMPlayer"; //ウィンドウタイトル文字列
    private final String STR_RIGHT_="(C) 2000 Kernel Inc"; //版権文字列（おまけ）

    /**
     * コンストラクタ
     *
     */
    public SimpleMoviePlayer(){
        this(null,false);
    }
    
    public SimpleMoviePlayer(boolean appFlag){
        this(null,appFlag);
    }
    /**
     * コンストラクタ
     *
     * @param   fileName   動画ファイル名 or URL
     */
    public SimpleMoviePlayer(String fileName){
        this(fileName,false);
    }
    public SimpleMoviePlayer(String fileName,boolean appFlag){
        super();
        
        appFlag_=appFlag;
        
        this.addWindowListener(
          new WindowAdapter() {
              public void windowClosing(WindowEvent evt) {
                  _quit();
              }
              public void windowClosed(WindowEvent evt) {
              }
          }
        );

        //メニュー

        Menu menu=new Menu("File");
        MenuBar bar=new MenuBar();
        bar.add(menu);

        MenuItem item;

        item = new MenuItem("Open...");
        item.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                _open();
            }            
        });
        menu.add(item);

        item = new MenuItem("Save Image As...");
        item.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                _saveImageAs();
            }            
        });
        menu.add(item);

        item = new MenuItem("Quit");
        item.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                _quit();
            }            
        });
        menu.add(item);
        
        this.setMenuBar(bar);


        //指定ファイルオープン
        if(_load(fileName)==false)_load(null);

        //見栄えその他
        this.setVisible(true);

    }

    /**
     * MediaLocator生成
     *   urlからMediaLocator生成(urlはファイル名でも可)
     *   url==nullならml=nullとなる
     *
     * @param   url   動画ファイル名 or URL
     */
    private MediaLocator _createMediaLocator(String url) {

        MediaLocator ml;
        if(url==null)return null;

        if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null)
            return ml;

        if (url.startsWith(File.separator)) {
            if ((ml = new MediaLocator("file:" + url)) != null)
            return ml;
        } else {
            String file = "file:" + System.getProperty("user.dir") + File.separator + url;
            if ((ml = new MediaLocator(file)) != null)
            return ml;
        }

        return null;
    }
    
    /**
     * 終了処理
     *
     */
    private void _quit(){
        _remove();
        if(appFlag_){
            System.exit(0);
        }else{
            setVisible(false);
        }
    }

    
    /**
     * 資源解放処理
     *
     */
    private void _remove(){
        //remove
        if (cmpVisual_!=null){
            this.getContentPane().remove(cmpVisual_);
            cmpVisual_=null;
        }
        if (cmpOpe_!=null){
            this.getContentPane().remove(cmpOpe_);
            cmpOpe_=null;
        }
        
        if (p_!=null){
            p_.removeControllerListener(this);
            p_.stop();
            p_.close();
            p_=null;
        }
    }

    /**
     * ロード
     *   fileNameで指定されたファイルをロード
     *   成功したらtrueを返す
     *   ml==nullなら空の状態にする
     *
     * @param   fileName   動画ファイル名 or URL
     * @return             成功ならtrue
     */
    private boolean _load(String fileName) {
        Component newVisual=null;
        Component newOpe=null;
        
        _remove();
        
        //System.out.println(fileName);
        
        //fileName==nullならml=nullとなる
        MediaLocator ml=_createMediaLocator(fileName);

        //System.out.println(ml);


        if(ml==null){
            newVisual =new JPanel();//ダミー
            ((JPanel)newVisual).setPreferredSize(new Dimension(160,120));
            newVisual.setBackground(Color.black);
            newOpe=new JLabel(STR_RIGHT_);
            this.setTitle(STR_TITLE_);
        }else{
            this.setTitle("Openning " + ml +"...");
            try {
                p_ = Manager.createProcessor(ml);
            } catch (Exception e) {
                System.err.println("Failed to create a processor from the given url: " + e);
                return false;
            }

            p_.addControllerListener(this);

            // Put the Processor into configured state.
            p_.configure();
            if (!_waitForState(Processor.Configured)) {
                System.err.println("Failed to configure the processor.");
                return false;
            }

            // So I can use it as a player.
            p_.setContentDescriptor(null);

            // Obtain the track controls.
            TrackControl tc[] = p_.getTrackControls();

            if (tc == null) {
                System.err.println("Failed to obtain track controls from the processor.");
                return false;
            }

            // Search for the track control for the video track.
            TrackControl videoTrack = null;

            for (int i = 0; i < tc.length; i++) {
                if (tc[i].getFormat() instanceof VideoFormat) {
                    videoTrack = tc[i];
                    break;
                }
            }

            if (videoTrack == null) {
                System.err.println("The input media does not contain a video track.");
                return false;
            }

            
            //FramePositioningControlを得る
            Object[] cnts;
            frameCtrl_=null;
            cnts=p_.getControls();
            for(int i=0;i<cnts.length;i++){
                if(cnts[i] instanceof FramePositioningControl){
                    frameCtrl_=(FramePositioningControl)cnts[i];
                    //System.out.println(cnts[i]);
                }
            }

            

            // Realize the processor.
            p_.prefetch();
            if (!_waitForState(Controller.Prefetched)) {
                System.err.println("Failed to realize the processor.");
                return false;
            }

            //Rendererを得る
            javax.media.Renderer renderer=null;
            frameGrabCtrl_=null;
            cnts=videoTrack.getControls();
            for(int i=0;i<cnts.length;i++){
                if(cnts[i] instanceof javax.media.Renderer){
                    renderer=(javax.media.Renderer)cnts[i];
                    //System.out.println(cnts[i]);
                }
            }
            
            //FrameGrabbingControlを得る
            frameGrabCtrl_=null;
            cnts=renderer.getControls();
            for(int i=0;i<cnts.length;i++){
                if(cnts[i] instanceof FrameGrabbingControl){
                    frameGrabCtrl_=(FrameGrabbingControl)cnts[i];
                    //System.out.println(cnts[i]);
                }
            }


            // Display the visual & control component if there's one.
            newVisual = p_.getVisualComponent();
            
            JPanel panel=new JPanel();
            panel.setLayout(new BorderLayout());
            Component cc;
            if ((cc = p_.getControlPanelComponent()) != null) {
                panel.add("Center", cc);
            }
            JButton btn=new JButton("Save"); 
            btn.addActionListener(new  ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        _saveImageAs();
                    }
                }
            );
            panel.add("East", btn);
            newOpe=panel;
            
            this.setTitle(STR_TITLE_ + " -" + ml);
        }
        

        //add
        this.getContentPane().setLayout(new BorderLayout());
        cmpVisual_=newVisual;
        if (cmpVisual_!= null) {
            getContentPane().add("Center", cmpVisual_);
        }
        
        cmpOpe_=newOpe;
        if (cmpOpe_ != null) {
            getContentPane().add("South", cmpOpe_);
        }
        this.pack();

        //はじめの画面を表示
        if(frameCtrl_!=null){
            frameCtrl_.skip(1);
            frameCtrl_.skip(-1);
         }

        return true;
    }

    /**
     * File-Save Image As
     *
     */
    private void _saveImageAs(){
        if (p_ == null) return;
        if (p_.getState()==Controller.Started)
            p_.stop();
        
        try{
            JFileChooser chooser =
                new JFileChooser(System.getProperty("user.dir"));
            ExampleFileFilter filter = new ExampleFileFilter();
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            filter.addExtension("jpg");
            filter.setDescription("Jpeg Image");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                FileOutputStream output =
                    new FileOutputStream(
                        chooser.getSelectedFile().getAbsolutePath()
                    );
                JPEGImageEncoder enc=JPEGCodec.createJPEGEncoder(output);
                enc.encode(_convertBufferedImage(frameGrabCtrl_.grabFrame() )); 
                //enc.encode(codec_.getLastBufferedImage()); 
                output.close();
            }
        }catch(Exception exception){
/*
            JOptionPane.showMessageDialog(
                this,
                "Can't Save Image :" + exception.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            ); 
*/
            new ErrorDialog(
                this,
                "Error",
                "Can't Save Image :" + exception.getMessage()
            );
        }
    }

    /**
     * File-Open...の処理
     *
     */
    private void _open(){
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        // Note: source for ExampleFileFilter can be found in FileChooserDemo,
        // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
        ExampleFileFilter filter = new ExampleFileFilter();
        filter.addExtension("mpg");
        filter.addExtension("avi");
        filter.addExtension("mov");
        filter.setDescription("Movie Files");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            if(!_load("file:" + chooser.getSelectedFile().getAbsolutePath())){
                //JOptionPane.showMessageDialog(this,  "Can't Open Movie.", "Error", JOptionPane.ERROR_MESSAGE); 
                new ErrorDialog(
                    this,
                    "Error",
                    "Can't Open Movie."
                );
                _load(null);
            }
        }
        
    }

    /**
     * Buffer から BufferedImageへ変換する
     * 
     * @param  buf  バッファ(フォーマットは RGB or YUV)
     * @return      BufferedImage,失敗ならnull
     *
     */
    private BufferedImage _convertBufferedImage(Buffer buf){
            MyBufferToImage bti =new MyBufferToImage((VideoFormat)buf.getFormat());
            Image img=bti.createImage(buf);
            if(img==null){
                System.out.println("Can't create Image in this format!");
                return null;
            }else{
                BufferedImage bimg=new BufferedImage(img.getWidth(null),img.getHeight(null),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=bimg.createGraphics();
                g.drawImage(img,0,0,null);
                return bimg;
            }
    }

    /**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     * 
     * @param  state  待つプロセッサの状態
     * @return        問題なければtrue,冷害が起こったらfalse
     *
     */
    private boolean _waitForState(int state) {
        synchronized (waitSync) {
            try {
                while (p_.getState() != state && stateTransitionOK)
                    waitSync.wait();
            } catch (Exception e) {}
        }
        return stateTransitionOK;
    }


    /**
     * Controller Listener(待ちに使用)
     *
     */
     public void controllerUpdate(ControllerEvent evt) {

        if (evt instanceof ConfigureCompleteEvent ||
            evt instanceof RealizeCompleteEvent ||
            evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
            
        }
    }



    /**
     * Main program
     *
     */
    public static void main(String [] args) {

        if (args.length == 0) {
            new SimpleMoviePlayer(true);
        }else{
            new SimpleMoviePlayer(args[0],true);
        }

    }

}

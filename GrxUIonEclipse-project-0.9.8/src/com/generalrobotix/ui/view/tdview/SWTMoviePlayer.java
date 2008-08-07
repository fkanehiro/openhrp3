package com.generalrobotix.ui.view.tdview;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.media.Buffer;
import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.control.TrackControl;
import javax.media.format.VideoFormat;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.sun.image.codec.jpeg.*; 

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class SWTMoviePlayer implements ControllerListener{
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

    Shell window_ = null;
    Frame frame_;
    Composite comp_;
    JPanel contentPane_;
    int frameX,frameY;
    
    /**
     * コンストラクタ
     * @param	shell 親にするシェル
     * @param   fileName   動画ファイル名 or URL
     */
    public SWTMoviePlayer( Shell shell, String fileName ){
    	
    	window_ = new Shell( shell, SWT.SHELL_TRIM );
		window_.setSize(200, 200);
		window_.setText("Movie Player");

		createMenu();
		
		FillLayout layout = new FillLayout();
		window_.setLayout(layout);

		//----
        // Linuxでリサイズイベントが発行されない問題対策
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=168330
		comp_ = new Composite( window_, SWT.EMBEDDED );
		frame_ = SWT_AWT.new_Frame( comp_ );

		comp_.addControlListener( new ControlListener() {
			public void controlMoved(ControlEvent e) {}
			public void controlResized(ControlEvent e) {
                frame_.setBounds(0, 0, comp_.getSize().x, comp_.getSize().y );
			}
        });
        //----

        contentPane_ = new JPanel();
        frame_.add(contentPane_);
		
        window_.open();
        window_.setSize(contentPane_.getPreferredSize().width, contentPane_.getPreferredSize().height);

        //枠線サイズを取得する
        frameX = window_.getSize().x - comp_.getSize().x;
		frameY = window_.getSize().y - comp_.getSize().y;
		System.out.println("frame size="+frameX+"-"+frameY);

        //指定ファイルオープン
        if(_load(fileName)==false)_load(null);
    }

    private void resizeWindow(){
		Display display = Display.getDefault();
		
		if ( display!=null && !display.isDisposed())
			// TODO: syncExecではこけるのだがなぜ？
			display.asyncExec(
				new Runnable(){
					public void run() {
						window_.setSize(contentPane_.getPreferredSize().width+frameX, contentPane_.getPreferredSize().height+frameY);
					}
				}
			);

    }
    
    private void createMenu() {
		Menu menubar = new Menu(window_,SWT.BAR);
		window_.setMenuBar(menubar);
	    
	    MenuItem item1 = new MenuItem(menubar,SWT.CASCADE);
	    item1.setText("File");
	    
	    Menu menu1 = new Menu(item1);
	    item1.setMenu(menu1);
	    
	    MenuItem item1_1 = new MenuItem(menu1,SWT.PUSH);
	    item1_1.setText("Open");
	    item1_1.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				_open();
			}
	    });

	    MenuItem item1_3 = new MenuItem(menu1,SWT.PUSH);
	    item1_3.setText("Save Image As");
	    item1_3.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				_saveImageAs();
			}
	    });
	    
	    MenuItem item1_4 = new MenuItem(menu1,SWT.PUSH);
	    item1_4.setText("Quit");
	    item1_4.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				window_.close();
			}
	    });
	    
    }


    /**
     * File-Open...の処理
     *
     */
    File currentFile;
    private void _open() {
        FileDialog fDialog = new FileDialog(window_,SWT.OPEN);

        String [] exts = {"*.mpg;*.avi;*.mov"};
		String [] filterNames = {"Movie Files(*.mpg,*.avi,*.mov)"};
		fDialog.setFilterExtensions(exts);
		fDialog.setFilterNames(filterNames);
		
		String openPath = fDialog.open();
		if( openPath != null ) {
			currentFile = new File( openPath );
			/*
            if(!_load("file:" + f.getAbsolutePath())){
            	MessageDialog.openError(window_, "Error", "Can't Open Movie." );
                _load(null);
            }
            */
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					public void run(){
			            if(!_load("file:" + currentFile.getAbsolutePath())){
			                _load(null);
			            }
					}
				});
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
    	
    	/*
    	JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        // Note: source for ExampleFileFilter can be found in FileChooserDemo,
        // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
        ExampleFileFilter filter = new ExampleFileFilter();
        filter.addExtension("mpg");
        filter.addExtension("avi");
        filter.addExtension("mov");
        filter.setDescription("Movie Files");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(frame_);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            if(!_load("file:" + chooser.getSelectedFile().getAbsolutePath())){
                //JOptionPane.showMessageDialog(frame_,  "Can't Open Movie.", "Error", JOptionPane.ERROR_MESSAGE);
                new ErrorDialog(
                    this,
                    "Error",
                    "Can't Open Movie."
                );
                _load(null);
            }
        }
        */
    }
    
    /**
     * 資源解放処理
     *
     */
    private void _remove(){
        //remove
        if (cmpVisual_!=null){
        	contentPane_.remove(cmpVisual_);
            cmpVisual_=null;
        }
        if (cmpOpe_!=null){
        	contentPane_.remove(cmpOpe_);
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
        
        System.out.println("filename is "+fileName);
        
        //fileName==nullならml=nullとなる
        MediaLocator ml=_createMediaLocator(fileName);

        System.out.println("ML="+ml);


        if(ml==null){
            newVisual =new JPanel();//ダミー
            ((JPanel)newVisual).setPreferredSize(new Dimension(160,120));
            newVisual.setBackground(Color.black);
            newOpe=new JLabel(STR_RIGHT_);
            //window_.setText(STR_TITLE_);
        }else{
            //window_.setText("Openning " + ml +"...");
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
            
            //window_.setText(STR_TITLE_ + " -" + ml);
        }
        

        //add
        contentPane_.setLayout(new BorderLayout());
        cmpVisual_=newVisual;
        if (cmpVisual_!= null) {
            contentPane_.add("Center", cmpVisual_);
        }
        
        cmpOpe_=newOpe;
        if (cmpOpe_ != null) {
            contentPane_.add("South", cmpOpe_);
        }

        //はじめの画面を表示
        if(frameCtrl_!=null){
            frameCtrl_.skip(1);
            frameCtrl_.skip(-1);
         }

        resizeWindow();

        return true;
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
     * File-Save Image As
     *
     */
    JPEGImageEncoder enc;
    private void _saveImageAs(){
        if (p_ == null) {
        	System.out.println("not init.");
        	return;
        }
		try {
			SwingUtilities.invokeAndWait(new Runnable(){
				public void run(){
			        if (p_.getState()==Controller.Started)
			            p_.stop();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
        try{
            FileDialog fDialog = new FileDialog(window_,SWT.SAVE);

            String [] exts = {"*.jpg"};
    		String [] filterNames = {"Jpeg Image(*.jpg)"};
    		fDialog.setFilterExtensions(exts);
    		fDialog.setFilterNames(filterNames);
    		
    		String openPath = fDialog.open();
    		if( openPath != null ) {
				File f = new File( openPath );

                FileOutputStream output =
                    new FileOutputStream(
                        f.getAbsolutePath()
                    );
                enc=JPEGCodec.createJPEGEncoder(output);

        		try {
        			SwingUtilities.invokeAndWait(new Runnable(){
        				public void run(){
        	                try {
								enc.encode(_convertBufferedImage(frameGrabCtrl_.grabFrame() ));
							} catch (ImageFormatException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} 
        				}
        			});
        		} catch (Exception e) {
        			e.printStackTrace();
        		}

                //enc.encode(codec_.getLastBufferedImage()); 
                output.close();
    		}

            /*        	
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
            */

        }catch(Exception exception){
        	/*
            JOptionPane.showMessageDialog(
                this,
                "Can't Save Image :" + exception.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            ); 
        	 */
        	MessageDialog.openError(window_, "Error", "Can't Save Image :" + exception.getMessage() );
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

}

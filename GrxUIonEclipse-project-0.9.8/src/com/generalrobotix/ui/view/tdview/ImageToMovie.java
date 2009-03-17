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

import java.io.*;
import java.util.*;
import java.awt.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.datasink.*;
import javax.media.format.*;
import javax.media.util.ImageToBuffer;


//======================================================================
//ImageToMovie
//
//フィールド
//    public static String QUICKTIME
//      .mov形式
//    public static String MSVIDEO
//      .avi形式
//
//コンストラクタ
//    public ImageToMovie (int width, int height, float frameRate,String outputUrl,String fileType){
//      引数の説明
//          width,height    ムービーサイズ
//          frameRate       フレームレート[1/s]
//          outputUrl       ファイル名
//          fileType        ファイルタイプ
//                              QUICKTIME or MSVIDEO
//
//メソッド
//    public Format[] getSupportedFormats()
//      出力可能なFormatを配列として返す。
//
//    public void setFormat(Format format)
//      出力形式を設定する。
//
//    public void startProcess()
//      処理を開始する。
//
//    public void pushImage(Image image){
//      ムービーの一コマのイメージを追加する
//
//    public int getImageStackSize(){
//      現在バッファにスタックされているイメージ数を返す
//
//    public void endProcess(){
//      ムービー作成のが終了する。
//      （バッファにたまっている画像を出力して終わる。ファイルが閉じられれるまでwaitする事に注意）
//
//======================================================================


public class ImageToMovie implements ControllerListener, DataSinkListener {
    //ファイル形式用定数
    public static String QUICKTIME=FileTypeDescriptor.QUICKTIME;
    public static String MSVIDEO=FileTypeDescriptor.MSVIDEO;
    
    private ImageDataSource ids_;//カスタムデータソース
    private Processor p_;//プロセッサ
    private DataSink dsink_;//データシンク
    private MediaLocator outML_;//出力ファイル

    private final boolean debugFlag_ = false;//デバッグ用

    //待ち用
    private Object waitSync_ = new Object();
    private boolean stateTransitionOK_ = true;
    private Object waitFileSync_ = new Object();
    private boolean fileDone_ = false;
    private boolean fileSuccess_ = true;


    // Create a media locator from the given string.
    static MediaLocator createMediaLocator(String url) {

        MediaLocator ml;

        if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null)
            return ml;

        if (url.startsWith(File.separator)) {
            if ((ml = new MediaLocator("file:" + url)) != null)
            return ml;
        } else {
            String file =
                "file:" + System.getProperty("user.dir") + File.separator + url;
            if ((ml = new MediaLocator(file)) != null) return ml;
        }

        return null;
    }

    //コンストラクタ
    //出力用MediaLocatorとプロセッサとデータソースを作りファイル作成の準備までする
    public ImageToMovie (
        int width,
        int height,
        float frameRate,
        String outputUrl,
        String fileType
    ){
        //出力用MediaLocator生成
        outML_ = createMediaLocator(outputUrl);
        if (debugFlag_) System.out.println("create:"+outML_);
        
        
        //データソース生成
        ids_ = new ImageDataSource(width, height, frameRate);

        //プロセッサ生成
        try {
            if (debugFlag_) {
                System.err.println(
                    "- create processor for the image datasource ..."
                );
            }
            p_ = Manager.createProcessor(ids_);
        } catch (Exception e) {
            System.err.println(
                "Yikes!  Cannot create a processor from the data source."
            );
            return;
        }

        
        p_.addControllerListener(this);

        // Put the Processor into configured state so we can set
        // some processing options on the processor.
        p_.configure();
        if (!_waitForState(p_, Processor.Configured)) {
            System.err.println("Failed to configure the processor.");
            return ;
        }

        // Set the output content descriptor to. 
        p_.setContentDescriptor(new ContentDescriptor(fileType));
    }
    
    //使用可能な形式を返す
    public Format[] getSupportedFormats(){
        TrackControl tcs[] = p_.getTrackControls();
        return tcs[0].getSupportedFormats();
    }

    //形式を設定
    public void setFormat(Format format){
        TrackControl tcs[] = p_.getTrackControls();
        tcs[0].setFormat(format);
    }
    
    //ムービー処理開始
    public boolean startProcess() {
        p_.realize();
        if (!_waitForState(p_, Controller.Realized)) {
            System.err.println("Failed to realize the processor.");
            return false;
        }
        // Now, we'll need to create a DataSink.
        if ((dsink_ = _createDataSink(p_, outML_)) == null) {
            System.err.println(
                "Failed to create a DataSink for the given output " +
                "MediaLocator: " + outML_
            );
            return false;
        }
        dsink_.addDataSinkListener(this);
        fileDone_ = false;
        
        if (debugFlag_) System.err.println("start processing...");

        // OK, we can now start the actual transcoding.
        try {
            p_.start();
            dsink_.start();
        } catch (IOException e) {
            System.err.println("IO error during processing");
            return false;
        }
        return true;
    }
    
    
    //イメージセットメソッド
    public void pushImage(Image image){
         ids_.pushImage(image);
    }

    //現在バッファにスタックされているイメージ数を返す
    public int getImageStackSize(){
         return ids_.getImageStackSize();
    }

    //終了させる
    public void endProcess(){
        ids_.endImage();
        
        _waitForFileDone();

        // Cleanup.
        try {
            dsink_.close();
        } catch (Exception ex) {}
        p_.removeControllerListener(this);

        if (debugFlag_) System.err.println("...done processing.");
             
    }


    //DataSinkを作成する下請け関数
    private DataSink _createDataSink(Processor p, MediaLocator outML) {

        DataSource ds;

        if ((ds = p.getDataOutput()) == null) {
            System.err.println(
                "Something is really wrong: the processor does not have " +
                "an output DataSource"
            );
            return null;
        }

        DataSink dsink;

        try {
            if (debugFlag_) {
                System.err.println("- create DataSink for: " + outML);
            }
            dsink = Manager.createDataSink(ds, outML);
            dsink.open();
        } catch (Exception e) {
            System.err.println("Cannot create the DataSink: " + e);
            return null;
        }

        return dsink;
    }


     //* Block until the processor has transitioned to the given state.
     //* Return false if the transition failed.
    boolean _waitForState(Processor p, int state) {
        synchronized (waitSync_) {
            try {
            while (p.getState() < state && stateTransitionOK_)
                waitSync_.wait();
            } catch (Exception e) {}
        }
        return stateTransitionOK_;
    }


     //* Controller Listener.
    public void controllerUpdate(ControllerEvent evt) {

        if (evt instanceof ConfigureCompleteEvent ||
            evt instanceof RealizeCompleteEvent ||
            evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync_) {
            stateTransitionOK_ = true;
            waitSync_.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync_) {
            stateTransitionOK_ = false;
            waitSync_.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            evt.getSourceController().stop();
            evt.getSourceController().close();
        }
    }




    // Block until file writing is done. 
    boolean _waitForFileDone() {
        synchronized (waitFileSync_) {
            try {
            while (!fileDone_)
                waitFileSync_.wait();
            } catch (Exception e) {}
        }
        return fileSuccess_;
    }


    //Event handler for the file writer.
    public void dataSinkUpdate(DataSinkEvent evt) {

        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync_) {
            fileDone_ = true;
            waitFileSync_.notifyAll();
            }
        } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync_) {
            fileDone_ = true;
            fileSuccess_ = false;
            waitFileSync_.notifyAll();
            }
        }
    }
    
    
    ///////////////////////////////////////////////
    // Inner classes.
    ///////////////////////////////////////////////
    class ImageDataSource extends PullBufferDataSource {

        ImageSourceStream streams_[];

        ImageDataSource(int width, int height, float frameRate) {
            streams_ = new ImageSourceStream[1];
            streams_[0] = new ImageSourceStream(width, height, frameRate);
        }
        //イメージセットメソッド
        public void pushImage(Image image){
             streams_[0].pushImage(image);
        }

        //現在バッファにスタックされているイメージ数を返す
        public int getImageStackSize(){
             return streams_[0].getImageStackSize();
        }

        //終了させる
        public void endImage(){
            streams_[0].endImage();
        }

        public void setLocator(MediaLocator source) {
        }

        public MediaLocator getLocator() {
            return null;
        }

        //Content type is of RAW since we are sending buffers of video
        // frames without a container format.
        public String getContentType() {
            return ContentDescriptor.RAW;
        }

        public void connect() {
        }

        public void disconnect() {
        }

        public void start() {
        }

        public void stop() {
        }

        public PullBufferStream[] getStreams() {
            return streams_;
        }

        public javax.media.Time getDuration() {
            return DURATION_UNKNOWN;
        }

        public Object[] getControls() {
            return new Object[0];
        }

        public Object getControl(String type) {
            return null;
        }
    }


    // The source stream to go along with ImageDataSource.
    class ImageSourceStream implements PullBufferStream {

        int width_, height_;
        float frameRate_;
        VideoFormat format_;
        //Format format_;
        Vector<Buffer> imageStack_; //イメージを貯えておくスタック
        boolean ending_ = false;//終了の要請あり
        boolean ended_ = false;//終了した

        public ImageSourceStream(int width, int height, float frameRate) {
            this.width_ = width;
            this.height_ = height;
            frameRate_=frameRate;
            
            imageStack_= new Vector<Buffer>();

            format_ =
                new RGBFormat(
                   new Dimension(width, height),
                   Format.NOT_SPECIFIED,
                   Format.intArray,
                   (float)frameRate,
                   32,
                   0xff0000,
                   0x00ff00,
                   0x0000ff
               );
        }

        //イメージセットメソッド
        public void pushImage(Image image){
            imageStack_.add(ImageToBuffer.createBuffer(image, frameRate_));
        }

        //終了させる
        public void endImage(){
            ending_=true;
        }

        //現在バッファにスタックされているイメージ数を返す
        public int getImageStackSize(){
             return imageStack_.size();
        }

        //イメージがセットされていればブロック解除
        //作者注：特に動作していないようです
        public boolean willReadBlock() {
            //System.out.println("willReadBlock");
            return (ending_==false && imageStack_.isEmpty());
            //return false;
        }

        // This is called from the Processor to read a frame worth
        // of video data.
        public void read(Buffer buf) throws IOException {

            // 終了の知らせあり
            if (ending_ && imageStack_.isEmpty()) {
                // We are done.  Set EndOfMedia.
                System.err.println("Done reading all images.");
                buf.setEOM(true);
                buf.setOffset(0);
                buf.setLength(0);
                ended_=true;

            //通常の処理
            } else if (!(imageStack_.isEmpty())){ //スタックにデータあり
                //スタックからイメージデータを取り出しbufに入れる
                buf.copy((Buffer)imageStack_.remove(0));
                buf.setFlags(buf.getFlags() | Buffer.FLAG_KEY_FRAME);
                buf.setFormat(format_);
                if (debugFlag_) {
                    /*
                    System.out.println(
                        "flag = " + buf.getFlags() +
                        " format = " + buf.getFormat() +
                        " data = " + buf.getData()
                    );
                    */
                    //System.out.println("writing image to file.");
                }
            } else { //スタックにはデータなし
                //System.err.println("una aho na!.");
                buf.setFlags(Buffer.FLAG_DISCARD );//ダミーを示すフラグをセット
            }

        }
        public Format getFormat() {
            return format_;
        }

        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        public long getContentLength() {
            return 0;
        }

        public boolean endOfStream() {
            return ended_;
        }

        public Object[] getControls() {
            return new Object[0];
        }

        public Object getControl(String type) {
            return null;
        }
        //
        //class StackEx extends Stack{
        //    public StackEx(){
        //        super();
        //    }
        //    public Object popFromEnd(){
        //        if (this.empty()){
        //            return null;
        //        }else{
        //            return this.remove(0);
        //        }
        //    }
        //}
    }

}

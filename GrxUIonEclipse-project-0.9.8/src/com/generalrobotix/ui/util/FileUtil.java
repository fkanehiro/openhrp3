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
 * FileUtil.java
 *
 * @author  T.Tawara
 * @version  1.0 (Wed Nov 21 2001)
 */

package com.generalrobotix.ui.util;

import java.io.*;
import java.nio.channels.*;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;

public class FileUtil {

    /**
     * @brief コピー元のパス[srcPath]から、コピー先のパス[destPath]へ
     *        ファイルのコピーを行います。
     *        コピー処理にはFileChannel#transferToメソッドを利用します。
     *        尚、コピー処理終了後、入力・出力のチャネルをクローズします。
     * @param srcPath    コピー元のパス
     * @param destPath    コピー先のパス
     * @throws IOException    何らかの入出力処理例外が発生した場合
     */
    public static void copyTransfer(String srcPath, String destPath) 
        throws IOException {
        
        FileChannel srcChannel = new FileInputStream(srcPath).getChannel();
        FileChannel destChannel = new FileOutputStream(destPath).getChannel();
        try {
            srcChannel.transferTo(0, srcChannel.size(), destChannel);
        } finally {
            srcChannel.close();
            destChannel.close();
        }
    }
    
    /**
     * @biref コピー元のリソース名から、コピー先のパス[destFile]へ
     *        ファイルのコピーを行います。
     * @param grxPluginManager  コピー元のリソースを保持する管理するGrxPluginManager
     * @param srcName           コピー元のリソース名
     * @param destFile          コピー先のFile
     * @throws IOException    何らかの入出力処理例外が発生した場合
     */
    public static void resourceToFile(Class<? extends GrxPluginManager> grxPluginManager, String srcName, File destFile)
    throws IOException {
        InputStream in = grxPluginManager.getResourceAsStream(srcName.toString());
        OutputStream out = new FileOutputStream(destFile.toString());
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
            out.close();
        }
    }
    
    /**
     * @brief omniName サーバのログファイル削除
     */
    public static void deleteNameServerLog(String logPath)
    {
        // log のクリア
        String[] com;
        if (System.getProperty("os.name").equals("Linux") || System.getProperty("os.name").equals("Mac OS X")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            com = new String[] { "/bin/sh", "-c", "rm " + logPath + File.separator + "*" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        } else {
            logPath = logPath.replaceFirst("^\"", "");
            logPath = logPath.replaceFirst("\"$", "");
            com = new String[] { "cmd", "/c", "del " + "\"" + logPath + File.separator + "omninames-*.*" + "\""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
        }
        try {
            Process pr = Runtime.getRuntime().exec(com);
            InputStream is = pr.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            pr.waitFor();
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

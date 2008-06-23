/**
 * ProjectErrorHandler.java
 *
 * @author  Kernel, Inc.
 * @version  1.0
 */

package com.generalrobotix.ui.view.tdview;

/**
 * プロジェクトロード時に起きたエラーを捕らえて、エラー処理をするためのハンドラ
 */
public interface ProjectErrorHandler {
    /**
     * モデルファイル読み込み時に起こるエラーをハンドル。
     *
     * @param  errorNo    エラー番号。
     *                    ProjectManager.FILE_NOT_FOUND,
     *                    ProjectManager.FILE_IO_ERROR,
     *                    ProjectManager.SERVER_ERROR,
     *                    ProjectManager.FILE_FORMAT_ERROR
     *                    のいずれか。
     * @param  objectType オブジェクトの種別。
     *                    ProjectManager.ROBOT_NODE,ProjectManager.ENVIRONMENT_NODE
     *                    のいずれか。
     * @param  objectName オブジェクト名
     * @param  url        ロードに失敗したurl
     *
     * @return エラーハンドラから抜けた後どうするか。ProjectManager.RELOAD,
     *         ProjectManager.SKIP_LOADING, ProjectManager.ABORTのいずれか。
     */
    public int loadVRMLFailed(
        int errorNo, 
        int objectType,
        String objectName,
        String url
    );

    /**
     * @return 読み直すための新しいurl
     */
    public String getURL();

    /**
     * アトリビュートを反映するときに起こるエラーをハンドル。
     *
     * @param errorNo    エラー番号。
     *                   ProjectManager.COLLISION_PAIR_LINK_FAILURE,
     *                   ProjectManager.VLINK_FAILURE
     *                   のいずれか。
     * @param objectName エラーの起こったオブジェクトの名前
     */
    public void reflectAttributeError(int errorNo, String objectName);
}

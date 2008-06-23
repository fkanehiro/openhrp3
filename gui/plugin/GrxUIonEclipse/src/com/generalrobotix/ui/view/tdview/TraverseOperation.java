/**
 * TraverseOperation.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.tdview;

import javax.media.j3d.Node;

public interface TraverseOperation {
    /**
     * オペレーション
     * @param   node
     * @param   parent
     */
    public void operation(Node node, Node parent);
}

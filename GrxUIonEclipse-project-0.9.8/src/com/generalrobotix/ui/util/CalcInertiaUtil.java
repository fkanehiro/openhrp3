package com.generalrobotix.ui.util;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

public class CalcInertiaUtil {
	
	public static Vector3d calcScale(Matrix3d I, double m){
		Matrix3d R = new Matrix3d();
		Matrix3d II = new Matrix3d();
		if (diagonalize(I,R,II)){
			double sum = II.m00+II.m11+II.m22;
			Vector3d sv = new Vector3d(
					m*Math.sqrt(sum/II.m00),
					m*Math.sqrt(sum/II.m11),
					m*Math.sqrt(sum/II.m22));
			return sv;
		}else{
			System.out.println("diagonalization failed"); //$NON-NLS-1$
		}	
		return new Vector3d(0,0,0);
	}
	
	/**
     * @brief diagonalize symmetric matrix
     * @param a symmetric matrix
     * @param U orthogonal matrix
     * @param W diagonal matrix
     * @return true if diagonalized successfully, false otherwise
     */
    private static boolean diagonalize(Matrix3d a, Matrix3d U, Matrix3d W){
    	int i=0,j=0,l,m,p,q,count;
    	double max,theta;
    	Matrix3d oldU = new Matrix3d();
    	Matrix3d newW = new Matrix3d();
    	W.set(a);

    	//計算結果としてだされた直行行列を格納するための配列を単位行列に初期化しておく。
    	for(p=0;p<3;p++) {
    		for(q=0;q<3;q++) {
    			if(p==q){
    				U.setElement(p, q, 1.0);
    			}else{
    				U.setElement(p, q, 0.0);
    			}
    		}
    	}

    	for(count=0;count<=10000;count++) {

    		//配列olduは新たな対角化計算を行う前にかけてきた直行行列を保持する。
    		for(p=0;p<3;p++) {
    			for(q=0;q<3;q++) {
    				oldU.setElement(p, q, U.getElement(p, q));
    			}
    		}
    		//非対角要素の中から絶対値の最大のものを見つける
    		max=0.0;
    		for(p=0;p<3;p++) {
    			for(q=0;q<3;q++) {
    				if(max<Math.abs(W.getElement(p, q)) && p!=q) {
    					max=Math.abs(W.getElement(p, q));
    					//その最大のものの成分の行と列にあたる数を記憶しておく。
    					i=p;
    					j=q;
    				}
    			}
    		}
    		/*先ほど選んだ最大のものが指定の値より小さければ対角化終了*/
    		if(max < 1.0e-10) {
    			break;
    		}
    		/*条件によってシータの値を決める*/
    		if(W.getElement(i,i)==W.getElement(j,j)){
    			theta=Math.PI/4.0;
    		}else{
    			theta=Math.atan(-2*W.getElement(i,j)/(W.getElement(i,i)-W.getElement(j,j)))/2.0;
    		}

    		//ここでこのときに実対称行列にかける個々の直行行列uが決まるが 特にここでの計算の意味はない。(する必要はない。)*/
    		double sth = Math.sin(theta);
    		double cth = Math.cos(theta);

    		/*ここでいままで実対称行列にかけてきた直行行列を配列Uに入れる。*/
    		for(p=0;p<3;p++) {
    			U.setElement(p,i,oldU.getElement(p,i)*cth-oldU.getElement(p,j)*sth);
    			U.setElement(p,j,oldU.getElement(p,i)*sth+oldU.getElement(p,j)*cth);
    		}

    		//対角化計算によってでた新たな実対称行列の成分を配列newaに入れる。
    		newW.setElement(i,i,W.getElement(i,i)*cth*cth
    				+W.getElement(j,j)*sth*sth-2.0*W.getElement(i,j)*sth*cth);
    		newW.setElement(j, j, W.getElement(i,i)*sth*sth
    				+W.getElement(j,j)*cth*cth+2.0*W.getElement(i,j)*sth*cth);
    		newW.setElement(i,j,0.0);
    		newW.setElement(j,i,0.0);
    		for(l=0;l<3;l++) {
    			if(l!=i && l!=j) {
    				newW.setElement(i,l,W.getElement(i,l)*cth-W.getElement(j,l)*sth);
    				newW.setElement(l,i,newW.getElement(i,l));
    				newW.setElement(j,l,W.getElement(i,l)*sth+W.getElement(j,l)*cth);
    				newW.setElement(l,j,newW.getElement(j,l));
    			}
    		}
    		for(l=0;l<3;l++) {
    			for(m=0;m<3;m++) {
    				if(l!=i && l!=j && m!=i && m!=j) newW.setElement(l, m, W.getElement(l,m));
    			}
    		}

    		//次の対角化計算を行う行列の成分を配列aへ上書きする。
    		W.set(newW);

    	}
    	if(count==10000) {
    		System.out.println("対角化するためにはまだ作業を繰り返す必要があります"); //$NON-NLS-1$
    		return false;
    	}else{
    		return true;
    	}
    }

}

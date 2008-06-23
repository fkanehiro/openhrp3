package jp.go.aist.hrp.simulator;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;


/**
 * PROTO の情報を取り出すユーティリティクラス
 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/02/15)
 */
public class ProtoFieldGettor
{
    //get...はsceneからsという名前のノードのs1というフィールドから値を取るメソッド
    //フィールドがなければdfltを返す。
    //変数名判りずらくてすみません。
    static public String[] getStringArray(String s, VrmlSceneEx scene, String s1,String[] dflt)
    {
        MFString sfstring = (MFString)scene.getField(s, s1);
        if(sfstring==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        String af[] = new String[sfstring.getSize()];
        sfstring.getValue(af);
        return af;
    }

    static public String getStringValue(String s, VrmlSceneEx scene, String s1,String dflt)
    {
        SFString sfstring = (SFString)scene.getField(s, s1);
        if(sfstring==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        return sfstring.getValue();
    }

    static public double[] getVectorValue(String s, VrmlSceneEx scene, String s1,double[] dflt)
    {
        float af[] = new float[3];
        SFVec3f sfvec3f = (SFVec3f)scene.getField(s, s1);
        if(sfvec3f==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        sfvec3f.getValue(af);
        double ad[] = new double[af.length];
        for(int i = 0; i < af.length; i++)
            ad[i] = af[i];

        return ad;
    }

    static public double[] getRotationValue(String s, VrmlSceneEx scene, String s1,double[] dflt)
    {
        float af[] = new float[4];
        SFRotation sfrotation = (SFRotation)scene.getField(s, s1);
        if(sfrotation==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        sfrotation.getValue(af);
        double ad[] = new double[af.length];
        for(int i = 0; i < af.length; i++)
            ad[i] = af[i];

        return ad;
    }

    static public float[] getFloatArray(String s, VrmlSceneEx scene, String s1,float[] dflt)
    {
        MFFloat mffloat = (MFFloat)scene.getField(s, s1);
        if(mffloat==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        float af[] = new float[mffloat.getSize()];
        if(af.length==0){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        mffloat.getValue(af);
        return af;
    }

    static public double[] getDoubleArray(String s, VrmlSceneEx scene, String s1,double[] dflt)
    {
        MFFloat mffloat = (MFFloat)scene.getField(s, s1);
        if(mffloat==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        float af[] = new float[mffloat.getSize()];
        if(af.length==0){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        mffloat.getValue(af);
        double ad[] = new double[af.length];
        for(int i = 0; i < af.length; i++)
            ad[i] = af[i];

        return ad;
    }

    static public double getDoubleValue(String s, VrmlSceneEx scene, String s1,double dflt)
    {
        SFFloat sffloat = (SFFloat)scene.getField(s, s1);
        if(sffloat==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        return (double)sffloat.getValue();
    }
    

    static public int getIntValue(String s, VrmlSceneEx scene, String s1,int dflt)
    {
        SFInt32 sffloat = (SFInt32)scene.getField(s, s1);
        if(sffloat==null){
            System.out.println(s +" has no " +s1 + ".");
            return dflt;
        }
        return (int)sffloat.getValue();
    }
}

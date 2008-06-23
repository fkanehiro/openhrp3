package jp.go.aist.hrp.simulator.PathPlannerPackage;


/**
* jp/go/aist/hrp/simulator/PathPlannerPackage/PointArrayHolder.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

public final class PointArrayHolder implements org.omg.CORBA.portable.Streamable
{
  public double value[][] = null;

  public PointArrayHolder ()
  {
  }

  public PointArrayHolder (double[][] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHelper.type ();
  }

}

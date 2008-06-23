package jp.go.aist.hrp.simulator.PathPlannerPackage;


/**
* jp/go/aist/hrp/simulator/PathPlannerPackage/PropertyHolder.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

public final class PropertyHolder implements org.omg.CORBA.portable.Streamable
{
  public String value[][] = null;

  public PropertyHolder ()
  {
  }

  public PropertyHolder (String[][] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = jp.go.aist.hrp.simulator.PathPlannerPackage.PropertyHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    jp.go.aist.hrp.simulator.PathPlannerPackage.PropertyHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return jp.go.aist.hrp.simulator.PathPlannerPackage.PropertyHelper.type ();
  }

}

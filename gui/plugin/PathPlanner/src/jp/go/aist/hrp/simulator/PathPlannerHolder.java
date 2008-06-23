package jp.go.aist.hrp.simulator;

/**
* jp/go/aist/hrp/simulator/PathPlannerHolder.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

public final class PathPlannerHolder implements org.omg.CORBA.portable.Streamable
{
  public jp.go.aist.hrp.simulator.PathPlanner value = null;

  public PathPlannerHolder ()
  {
  }

  public PathPlannerHolder (jp.go.aist.hrp.simulator.PathPlanner initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = jp.go.aist.hrp.simulator.PathPlannerHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    jp.go.aist.hrp.simulator.PathPlannerHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return jp.go.aist.hrp.simulator.PathPlannerHelper.type ();
  }

}

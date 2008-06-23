package jp.go.aist.hrp.simulator;


/**
* jp/go/aist/hrp/simulator/PathPlannerHelper.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

abstract public class PathPlannerHelper
{
  private static String  _id = "IDL:OpenHRP/PathPlanner:1.0";

  public static void insert (org.omg.CORBA.Any a, jp.go.aist.hrp.simulator.PathPlanner that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static jp.go.aist.hrp.simulator.PathPlanner extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (jp.go.aist.hrp.simulator.PathPlannerHelper.id (), "PathPlanner");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static jp.go.aist.hrp.simulator.PathPlanner read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_PathPlannerStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, jp.go.aist.hrp.simulator.PathPlanner value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static jp.go.aist.hrp.simulator.PathPlanner narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof jp.go.aist.hrp.simulator.PathPlanner)
      return (jp.go.aist.hrp.simulator.PathPlanner)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      jp.go.aist.hrp.simulator._PathPlannerStub stub = new jp.go.aist.hrp.simulator._PathPlannerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static jp.go.aist.hrp.simulator.PathPlanner unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof jp.go.aist.hrp.simulator.PathPlanner)
      return (jp.go.aist.hrp.simulator.PathPlanner)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      jp.go.aist.hrp.simulator._PathPlannerStub stub = new jp.go.aist.hrp.simulator._PathPlannerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}

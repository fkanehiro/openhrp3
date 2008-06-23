package jp.go.aist.hrp.simulator.PathPlannerPackage;


/**
* jp/go/aist/hrp/simulator/PathPlannerPackage/PointArrayHelper.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

abstract public class PointArrayHelper
{
  private static String  _id = "IDL:OpenHRP/PathPlanner/PointArray:1.0";

  public static void insert (org.omg.CORBA.Any a, double[][] that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static double[][] extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_double);
      __typeCode = org.omg.CORBA.ORB.init ().create_sequence_tc (3, __typeCode);
      __typeCode = org.omg.CORBA.ORB.init ().create_alias_tc (jp.go.aist.hrp.simulator.DblSequence3Helper.id (), "DblSequence3", __typeCode);
      __typeCode = org.omg.CORBA.ORB.init ().create_sequence_tc (0, __typeCode);
      __typeCode = org.omg.CORBA.ORB.init ().create_alias_tc (jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHelper.id (), "PointArray", __typeCode);
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static double[][] read (org.omg.CORBA.portable.InputStream istream)
  {
    double value[][] = null;
    int _len0 = istream.read_long ();
    value = new double[_len0][];
    for (int _o1 = 0;_o1 < value.length; ++_o1)
      value[_o1] = jp.go.aist.hrp.simulator.DblSequence3Helper.read (istream);
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, double[][] value)
  {
    ostream.write_long (value.length);
    for (int _i0 = 0;_i0 < value.length; ++_i0)
      jp.go.aist.hrp.simulator.DblSequence3Helper.write (ostream, value[_i0]);
  }

}

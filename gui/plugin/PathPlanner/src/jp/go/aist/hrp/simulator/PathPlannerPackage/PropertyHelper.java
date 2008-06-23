package jp.go.aist.hrp.simulator.PathPlannerPackage;


/**
* jp/go/aist/hrp/simulator/PathPlannerPackage/PropertyHelper.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

abstract public class PropertyHelper
{
  private static String  _id = "IDL:OpenHRP/PathPlanner/Property:1.0";

  public static void insert (org.omg.CORBA.Any a, String[][] that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static String[][] extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_string_tc (0);
      __typeCode = org.omg.CORBA.ORB.init ().create_sequence_tc (2, __typeCode);
      __typeCode = org.omg.CORBA.ORB.init ().create_sequence_tc (0, __typeCode);
      __typeCode = org.omg.CORBA.ORB.init ().create_alias_tc (jp.go.aist.hrp.simulator.PathPlannerPackage.PropertyHelper.id (), "Property", __typeCode);
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static String[][] read (org.omg.CORBA.portable.InputStream istream)
  {
    String value[][] = null;
    int _len0 = istream.read_long ();
    value = new String[_len0][];
    for (int _o1 = 0;_o1 < value.length; ++_o1)
    {
      int _len1 = istream.read_long ();
      if (_len1 > (2))
        throw new org.omg.CORBA.MARSHAL (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
      value[_o1] = new String[_len1];
      for (int _o2 = 0;_o2 < value[_o1].length; ++_o2)
        value[_o1][_o2] = istream.read_string ();
    }
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, String[][] value)
  {
    ostream.write_long (value.length);
    for (int _i0 = 0;_i0 < value.length; ++_i0)
    {
      if (value[_i0].length > (2))
        throw new org.omg.CORBA.MARSHAL (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
      ostream.write_long (value[_i0].length);
      for (int _i1 = 0;_i1 < value[_i0].length; ++_i1)
        ostream.write_string (value[_i0][_i1]);
    }
  }

}

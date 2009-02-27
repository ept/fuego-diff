/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.raxs.exper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import fc.util.Debug.Measure;
import fc.util.Debug.Time;
import fc.util.log.Log;

public class ResultLog {

  private String store;
  private String test;
  private String by;
  private String units;
  private int run=0;
  private PrintStream out;
  private Object[] markers;
  private Field[] fields; 
  
  public ResultLog(String test, String store, String by, Class[] fieldClasses,
      Object[] points, PrintStream out) {
    char SEP = ' ';
    ArrayList<Object> markers = new ArrayList<Object>();
    ArrayList<Field> fields = new ArrayList<Field>();
    units = null;
    try {
      for( Object point : points ) {
        for( Class c : fieldClasses ) {
          for( Field f : c.getFields() ) {
            if( f.get(null) == point ) {
              fields.add(f);
              markers.add(point);
              units=(units==null ? "" : units+ SEP) + f.getName();
            }
          }
        }
      }
    } catch (Exception e) {
      Log.fatal("Error creating result log",e);
    }
    this.markers = markers.toArray();
    this.fields = fields.toArray(new Field[] {});
    this.test = test;
    this.store = store;
    this.by = by;
    this.out = out;
    out.println("# Results-of:"+SEP+test+SEP+store+SEP+by+SEP+units);
  }

  public ResultLog(String test, String store, String by, String units, PrintStream out) {
    this.test = test;
    this.store = store;
    this.by = by;
    this.units = units;
    this.out = out;
    char SEP = ' ';
    out.println("# Results-of:"+SEP+test+SEP+store+SEP+by+SEP+units);
  }

  public void result(int point) {
    String[] result = new String[markers.length];
    for( int i=0;i<markers.length;i++)
      result[i] =  String.valueOf( Measure.get(markers[i]) );
    result( String.valueOf( point ), result );
  }
  
  public void result(double point, double val) {
    result(String.valueOf(point),new String [] {String.valueOf(val)});
  }

  public void result(long point, long val) {
    result(String.valueOf(point),new String[] {String.valueOf(val)});
  }

  public void result(long point, long val0, long val1) {
    result(String.valueOf(point),new String[] {String.valueOf(val0),
      String.valueOf(val1)});
  }

  public void result(long point, long val0, long val1, long val2) {
    result(String.valueOf(point),new String[] {String.valueOf(val0),
      String.valueOf(val1),String.valueOf(val2)});
  }

  public void result(long point, long val0, long val1, long val2, long val3) {
    result(String.valueOf(point),new String[] {String.valueOf(val0),
      String.valueOf(val1),String.valueOf(val2),String.valueOf(val3)});
  }
  
  public void result(long point, long val0, long val1, long val2, long val3, 
      long val4) {
    result(String.valueOf(point),new String[] {String.valueOf(val0),
      String.valueOf(val1),String.valueOf(val2),String.valueOf(val3),
      String.valueOf(val4)});
  }

  public void result(long point, long val0, long val1, long val2, long val3, 
      long val4,long val5) {
    result(String.valueOf(point),new String[] {String.valueOf(val0),
      String.valueOf(val1),String.valueOf(val2),String.valueOf(val3),
      String.valueOf(val4),String.valueOf(val5)});
  }

  public void result(long point, long val0, long val1, long val2, long val3, 
      long val4, long val5, long val6) {
    result(String.valueOf(point),new String[] {String.valueOf(val0),
      String.valueOf(val1),String.valueOf(val2),String.valueOf(val3),
      String.valueOf(val4),String.valueOf(val5),String.valueOf(val6)});
  }

  public void result(long point, long val0, long val1, long val2, long val3, 
      long val4, long val5, long val6, long val7) {
    result(String.valueOf(point),new String[] {String.valueOf(val0),
      String.valueOf(val1),String.valueOf(val2),String.valueOf(val3),
      String.valueOf(val4),String.valueOf(val5),String.valueOf(val6),
      String.valueOf(val7)});
  }
  
  private void result(String point,String[] val) {
    char SEP = ' ';
    StringBuilder sb = new StringBuilder();
    for(int i =0;i<val.length;i++ ) {
      if( i > 0)
        sb.append(SEP);
      sb.append(val[i]);
    }
    out.println((run>0 ? ""+run+SEP : "") + point+SEP+sb);
  }

  public void comment(String cmt) {
    out.println("# "+cmt);
  }


  public void finish() {
    out.println("# Results-end");
    out.println("# arch-tag: 51081cba-421c-46fe-a531-67fd4e79d932-"+
        Long.toString( 0xffffffffl&(test+store+by+units).hashCode(), 16) );
  }

  public void setRun(int run) {
    this.run = run;
  }
  
  public static class TeeStream extends OutputStream {
    
    private OutputStream out1;
    private OutputStream out2;
    private boolean close2;
    
    public TeeStream(OutputStream out1, OutputStream out2, boolean close2) {
      this.out1 = out1;
      this.out2 = out2;
      this.close2 = close2;
    }

    public void close() throws IOException {
      out1.close();
      if( close2 )
        out2.close();
    }

    public void flush() throws IOException {
      out1.flush();
      out2.flush();
    }

    public void write(byte[] b, int off, int len) throws IOException {
      out1.write(b, off, len);
      out2.write(b, off, len);
    }

    public void write(byte[] b) throws IOException {
      out1.write(b);
      out2.write(b);
    }

    public void write(int b) throws IOException {
      out1.write(b);
      out2.write(b);
    }
    
  }

  
}
// arch-tag: 2646aae7-4929-4819-860c-884191cb7613
//

/**
 *
 */
package com.generalrobotix.ui.util;

/**
 * @author Tawara
 *
 */

public class SynchronizedAccessor < T extends java.lang.Object >{
    private T value_ = null;
    public SynchronizedAccessor(T val){
        value_ = val;
    }

    public synchronized T get(){ return value_; }
    public synchronized void set(T val){ value_ = val; }
}

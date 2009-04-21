/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * SynchronizedAccessor.java
 *
 *
 * @author  T.Tawara
 * @version 1.0 (2009/04/16)
 */

package com.generalrobotix.ui.util;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

public class SynchronizedAccessor < T extends java.lang.Object >
    implements Lock
{
    private T value_ = null;
    private ReentrantLock reentrantLock_ = new ReentrantLock(); 

    public SynchronizedAccessor(T val){
        value_ = val;
    }
    
    public synchronized T get(){ return value_; }
    public synchronized void set(T val){ value_ = val; }

    public void lock()
    {
        // TODO 自動生成されたメソッド・スタブ
        reentrantLock_.lock();
    }

    public void lockInterruptibly()
            throws InterruptedException
    {
        // TODO 自動生成されたメソッド・スタブ
        reentrantLock_.lockInterruptibly();
    }

    public Condition newCondition()
    {
        // TODO 自動生成されたメソッド・スタブ
        return reentrantLock_.newCondition();
    }

    public boolean tryLock()
    {
        // TODO 自動生成されたメソッド・スタブ
        return reentrantLock_.tryLock();
    }

    public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException
    {
        // TODO 自動生成されたメソッド・スタブ
        return reentrantLock_.tryLock(time, unit);
    }

    public void unlock()
    {
        // TODO 自動生成されたメソッド・スタブ
        reentrantLock_.unlock();
    }
}

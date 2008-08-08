/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.util;
import java.util.*;

@SuppressWarnings("unchecked")
public class OrderedHashMap extends HashMap {
    private List<Object> keyList_;
    private List<Object> valList_;
    
    public OrderedHashMap() {
        keyList_ = new ArrayList<Object>();
        valList_ = new ArrayList<Object>();
    }

    public Object put(Object key,Object obj) {
        if (!containsKey(key)) {
            keyList_.add(key);
            valList_.add(obj);
        }
        return super.put(key, obj);
    }

    public Iterator keys() {
        return keyList_.iterator();
    }

    public Collection values() {
        return valList_;
    }

    public Object[] toArray() {
        return keyList_.toArray();
    }

    public Object[] toArray(Object[] array) {
        return keyList_.toArray(array);
    }
    
    public Object remove(Object key) {
        keyList_.remove(key);
        Object val = super.remove(key);
        valList_.remove(val);
        return val;
    }

    public void clear(){
        keyList_.clear();
        valList_.clear();
        super.clear();
    }
}

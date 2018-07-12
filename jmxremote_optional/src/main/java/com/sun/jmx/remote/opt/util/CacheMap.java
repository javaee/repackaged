/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.jmx.remote.opt.util;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * <p>Like WeakHashMap, except that the keys of the <em>n</em> most
 * recently-accessed entries are kept as {@link SoftReference soft
 * references}.  Accessing an element means creating it, or retrieving
 * it with {@link #get(Object) get}.  Because these entries are kept
 * with soft references, they will tend to remain even if their keys
 * are not referenced elsewhere.  But if memory is short, they will
 * be removed.</p>
 */
public class CacheMap extends WeakHashMap {
    /**
     * <p>Create a <code>CacheMap</code> that can keep up to
     * <code>nSoftReferences</code> as soft references.</p>
     *
     * @param nSoftReferences Maximum number of keys to keep as soft
     * references.  Access times for {@link #get(Object) get} and
     * {@link #put(Object, Object) put} have a component that scales
     * linearly with <code>nSoftReferences</code>, so this value
     * should not be too great.
     *
     * @throws IllegalArgumentException if
     * <code>nSoftReferences</code> is negative.
     */
    public CacheMap(int nSoftReferences) {
	if (nSoftReferences < 0) {
	    throw new IllegalArgumentException("nSoftReferences = " +
					       nSoftReferences);
	}
	this.nSoftReferences = nSoftReferences;
    }

    public Object put(Object key, Object value) {
	cache(key);
	return super.put(key, value);
    }

    public Object get(Object key) {
	cache(key);
	return super.get(key);
    }

    /* We don't override remove(Object) or try to do something with
       the map's iterators to detect removal.  So we may keep useless
       entries in the soft reference list for keys that have since
       been removed.  The assumption is that entries are added to the
       cache but never removed.  But the behaviour is not wrong if
       they are in fact removed -- the caching is just less
       performant.  */

    private void cache(Object key) {
	Iterator it = cache.iterator();
	while (it.hasNext()) {
            SoftReference sref = (SoftReference) it.next();
            Object key1 = sref.get();
	    if (key1 == null)
                it.remove();
	    else if (key.equals(key1)) {
		// Move this element to the head of the LRU list
		it.remove();
		cache.add(0, sref);
		return;
	    }
	}

	int size = cache.size();
	if (size == nSoftReferences) {
	    if (size == 0)
		return;  // degenerate case, equivalent to WeakHashMap
	    it.remove();
	}

	cache.add(0, new SoftReference(key));
    }

    /* List of soft references for the most-recently referenced keys.
       The list is in most-recently-used order, i.e. the first element
       is the most-recently referenced key.  There are never more than
       nSoftReferences elements of this list.
    
       If we didn't care about J2SE 1.3 compatibility, we could use
       LinkedHashSet in conjunction with a subclass of SoftReference
       whose equals and hashCode reflect the referent.  */
    private final LinkedList/*<SoftReference>*/ cache = new LinkedList();
    private final int nSoftReferences;
}

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

package com.sun.jmx.remote.opt.internal;

import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import javax.security.auth.Subject;


/**
 * <p>An identified listener.  A listener has an Integer id that is
 * unique per connector server.  It selects notifications based on the
 * ObjectName of the originator and an optional
 * NotificationFilter.</p>
 *
 * <p>Two ListenerInfo objects are considered equal if and only if
 * they have the same listenerId.  This means that ListenerInfo
 * objects can be stored in a Set or Map and retrieved using another
 * ListenerInfo with the same listenerId but arbitrary ObjectNme and
 * NotificationFilter values.</p>
 */
public class ListenerInfo {
    public ListenerInfo(Integer listenerID,
			ObjectName name,
			NotificationFilter filter) {
	this.listenerID = listenerID;
	this.name = name;
	this.filter = filter;
    }

    public ListenerInfo(Integer listenerID,
			ObjectName name,
			NotificationListener listener,
			NotificationFilter filter,
			Object handback,
			Subject delegationSubject) {
	this.listenerID = listenerID;
	this.name = name;
	this.listener = listener;
	this.filter = filter;
	this.handback = handback;
	this.delegationSubject = delegationSubject;
    }

    public boolean equals(Object o) {
	if (!(o instanceof ListenerInfo)) {
	    return false;
	}

	return listenerID.equals(((ListenerInfo)o).listenerID);
    }

    public int hashCode() {
	return listenerID.intValue();
    }

    public ObjectName getObjectName() {
	return name;
    }

    public Integer getListenerID() {
	return listenerID;
    }

    public NotificationFilter getNotificationFilter() {
	return filter;
    }

    public NotificationListener getListener() {
	return listener;
    }

    public Object getHandback() {
	return handback;
    }

    public Subject getDelegationSubject() {
	return delegationSubject;
    }

    private ObjectName name;
    private Integer listenerID;
    private NotificationFilter filter;

    private NotificationListener listener = null;
    private Object handback = null;
    private Subject delegationSubject = null;
}

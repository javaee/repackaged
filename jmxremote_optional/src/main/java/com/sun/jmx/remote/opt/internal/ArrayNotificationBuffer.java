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

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryEval;
import javax.management.QueryExp;

import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;

import com.sun.jmx.remote.opt.util.EnvHelp;
import com.sun.jmx.remote.opt.util.ClassLogger;

/** A circular buffer of notifications received from an MBean server. */
public class ArrayNotificationBuffer implements NotificationBuffer {
    
    private boolean disposed = false;
    
    // FACTORY STUFF, INCLUDING SHARING
    
    private static final
	HashMap/*<MBeanServer,ArrayNotificationBuffer>*/ mbsToBuffer =
	new HashMap(1);
    private final Collection/*<ShareBuffer>*/ sharers = new HashSet(1);

    public static synchronized NotificationBuffer
	    getNotificationBuffer(MBeanServer mbs, Map env) {
	
	//Find out queue size	
	int queueSize = EnvHelp.getNotifBufferSize(env);
	
	ArrayNotificationBuffer buf = (ArrayNotificationBuffer)mbsToBuffer.get(mbs);
	if (buf == null) {
	    buf = new ArrayNotificationBuffer(mbs, queueSize);
	    mbsToBuffer.put(mbs, buf);
	}
	return buf.new ShareBuffer(queueSize);
    }
    
    public static synchronized void removeNotificationBuffer(MBeanServer mbs) {
	mbsToBuffer.remove(mbs);
    }
    
    synchronized void addSharer(ShareBuffer sharer) {
	if (sharer.getSize() > queueSize)
	    resize(sharer.getSize());
	sharers.add(sharer);
    }

    void removeSharer(ShareBuffer sharer) {
        boolean empty;
        synchronized (this) {
            sharers.remove(sharer);
            empty = sharers.isEmpty();
            if (!empty) {
                int max = 0;
		for (Iterator it = sharers.iterator(); it.hasNext(); ) {
		    ShareBuffer buf = (ShareBuffer) it.next();
		    int bufsize = buf.getSize();
		    if (bufsize > max)
			max = bufsize;
		}
                if (max < queueSize)
                    resize(max);
            }
        }
        if (empty)
            dispose();
    }

    private void resize(int newSize) {
	if (newSize == queueSize)
	    return;
	while (queue.size() > newSize)
	    dropNotification();
	queue.resize(newSize);
	queueSize = newSize;
    }

    private class ShareBuffer implements NotificationBuffer {
	ShareBuffer(int size) {
	    this.size = size;
	    addSharer(this);
	}

	public NotificationResult
	    fetchNotifications(Set/*<ListenerInfo>*/ listeners,
			       long startSequenceNumber,
			       long timeout,
			       int maxNotifications)
		throws InterruptedException {
	    NotificationBuffer buf = ArrayNotificationBuffer.this;
	    return buf.fetchNotifications(listeners, startSequenceNumber,
					  timeout, maxNotifications);
	}

	public void dispose() {
	    ArrayNotificationBuffer.this.removeSharer(this);
	}

	int getSize() {
	    return size;
	}

	private final int size;
    }


    // ARRAYNOTIFICATIONBUFFER IMPLEMENTATION

    private ArrayNotificationBuffer(MBeanServer mbs, int queueSize) {
        if (logger.traceOn())
            logger.trace("Constructor", "queueSize=" + queueSize);

        if (mbs == null || queueSize < 1)
            throw new IllegalArgumentException("Bad args");

        this.mBeanServer = mbs;
        this.queueSize = queueSize;
        this.queue = new ArrayQueue(queueSize);
        this.earliestSequenceNumber = System.currentTimeMillis();
        this.nextSequenceNumber = this.earliestSequenceNumber;

        createListeners();

        logger.trace("Constructor", "ends");
    }

    private synchronized boolean isDisposed() {
	return disposed;
    }

    public void dispose() {
        logger.trace("dispose", "starts");

	synchronized(this) {
	    removeNotificationBuffer(mBeanServer);
	    disposed = true;
	    //Notify potential waiting fetchNotification call
	    notifyAll();
	}

        destroyListeners();
	
        logger.trace("dispose", "ends");
    }

    /**
     * <p>Fetch notifications that match the given listeners.</p>
     *
     * <p>The operation only considers notifications with a sequence
     * number at least <code>startSequenceNumber</code>.  It will take
     * no longer than <code>timeout</code>, and will return no more
     * than <code>maxNotifications</code> different notifications.</p>
     *
     * <p>If there are no notifications matching the criteria, the
     * operation will block until one arrives, subject to the
     * timeout.</p>
     *
     * @param listeners a Set of {@link ListenerInfo} that reflects
     * the filters to be applied to notifications.  Accesses to this
     * Set are synchronized on the Set object.  The Set is consulted
     * for selected notifications that are present when the method
     * starts, and for selected notifications that arrive while it is
     * executing.  The contents of the Set can be modified, with
     * appropriate synchronization, while the method is running.
     * @param startSequenceNumber the first sequence number to
     * consider.
     * @param timeout the maximum time to wait.  May be 0 to indicate
     * not to wait if there are no notifications.
     * @param maxNotifications the maximum number of notifications to
     * return.  May be 0 to indicate a wait for eligible notifications
     * that will return a usable <code>nextSequenceNumber</code>.  The
     * {@link TargetedNotification} array in the returned {@link
     * NotificationResult} may contain more than this number of
     * elements but will not contain more than this number of
     * different notifications.
     */
    public NotificationResult
        fetchNotifications(Set/*<ListenerInfo>*/ listeners,
                           long startSequenceNumber,
                           long timeout,
                           int maxNotifications)
            throws InterruptedException {

        logger.trace("fetchNotifications", "starts");

	if (startSequenceNumber < 0 || isDisposed()) {
	    synchronized(this) {
		return new NotificationResult(earliestSequenceNumber(), 
					      nextSequenceNumber(), 
					      new TargetedNotification[0]);
	    }
	}
	
        // Check arg validity
        if (listeners == null
            || startSequenceNumber < 0 || timeout < 0
            || maxNotifications < 0) {
            logger.trace("fetchNotifications", "Bad args");
            throw new IllegalArgumentException("Bad args to fetch");
        }

        if (logger.debugOn()) {
            logger.trace("fetchNotifications",
                  "listener-length=" + listeners.size() + "; startSeq=" +
                  startSequenceNumber + "; timeout=" + timeout +
                  "; max=" + maxNotifications);
        }

        if (startSequenceNumber > nextSequenceNumber()) {
            final String msg = "Start sequence number too big: " +
                startSequenceNumber + " > " + nextSequenceNumber();
            logger.trace("fetchNotifications", msg);
            throw new IllegalArgumentException(msg);
        }

        /* Determine the end time corresponding to the timeout value.
           Caller may legitimately supply Long.MAX_VALUE to indicate no
           timeout.  In that case the addition will overflow and produce
           a negative end time.  Set end time to Long.MAX_VALUE in that
           case.  We assume System.currentTimeMillis() is positive.  */
        long endTime = System.currentTimeMillis() + timeout;
        if (endTime < 0) // overflow
            endTime = Long.MAX_VALUE;

        if (logger.debugOn())
            logger.debug("fetchNotifications", "endTime=" + endTime);

        /* We set earliestSeq the first time through the loop.  If we
           set it here, notifications could be dropped before we
           started examining them, so earliestSeq might not correspond
           to the earliest notification we examined.  */
        long earliestSeq = -1;
        long nextSeq = startSequenceNumber;
        List/*<TargetedNotification>*/ notifs = new ArrayList();

        /* On exit from this loop, notifs, earliestSeq, and nextSeq must
           all be correct values for the returned NotificationResult.  */
        while (true) {
            logger.debug("fetchNotifications", "main loop starts");

            NamedNotification candidate;

            /* Get the next available notification regardless of filters,
               or wait for one to arrive if there is none.  */
            synchronized (this) {
		
                /* First time through.  The current earliestSequenceNumber
                   is the first one we could have examined.  */
                if (earliestSeq < 0) {
                    earliestSeq = earliestSequenceNumber();
                    if (logger.debugOn()) {
                        logger.debug("fetchNotifications",
                              "earliestSeq=" + earliestSeq);
                    }
                    if (nextSeq < earliestSeq) {
                        nextSeq = earliestSeq;
                        logger.debug("fetchNotifications", 
				     "nextSeq=earliestSeq");
                    }
                } else
                    earliestSeq = earliestSequenceNumber();

                /* If many notifications have been dropped since the
                   last time through, nextSeq could now be earlier
                   than the current earliest.  If so, notifications
                   may have been lost and we return now so the caller
                   can see this next time it calls.  */
                if (nextSeq < earliestSeq) {
                    logger.trace("fetchNotifications",
                          "nextSeq=" + nextSeq + " < " + "earliestSeq=" +
                          earliestSeq + " so may have lost notifs");
                    break;
                }

                if (nextSeq < nextSequenceNumber()) {
                    candidate = notificationAt(nextSeq);
                    if (logger.debugOn()) {
                        logger.debug("fetchNotifications", "candidate: " + 
				     candidate);
                        logger.debug("fetchNotifications", "nextSeq now " + 
				     nextSeq);
                    }
                } else {
                    /* nextSeq is the largest sequence number.  If we
                       already got notifications, return them now.
                       Otherwise wait for some to arrive, with
                       timeout.  */
                    if (notifs.size() > 0) {
                        logger.debug("fetchNotifications",
                              "no more notifs but have some so don't wait");
                        break;
                    }
                    long toWait = endTime - System.currentTimeMillis();
                    if (toWait <= 0) {
                        logger.debug("fetchNotifications", "timeout");
                        break;
                    }
		    
		    /* dispose called */
		    if (isDisposed()) {
			if (logger.debugOn())
			    logger.debug("fetchNotifications", 
					 "dispose callled, no wait");
			return new NotificationResult(earliestSequenceNumber(),
						  nextSequenceNumber(), 
						  new TargetedNotification[0]);
		    }
		    
		    if (logger.debugOn())
			logger.debug("fetchNotifications", 
				     "wait(" + toWait + ")");
		    wait(toWait);
		    
                    continue;
                }
            }
	    
            /* We have a candidate notification.  See if it matches
               our filters.  We do this outside the synchronized block
               so we don't hold up everyone accessing the buffer
               (including notification senders) while we evaluate
               potentially slow filters.  */
            ObjectName name = candidate.getObjectName();
            Notification notif = candidate.getNotification();
            List/*<TargetedNotification>*/ matchedNotifs = new ArrayList();
            logger.debug("fetchNotifications", 
			 "applying filters to candidate");
            synchronized (listeners) {
                for (Iterator it = listeners.iterator(); it.hasNext(); ) {
                    ListenerInfo li = (ListenerInfo) it.next();
                    ObjectName pattern = li.getObjectName();
                    NotificationFilter filter = li.getNotificationFilter();

                    if (logger.debugOn()) {
                        logger.debug("fetchNotifications",
                              "pattern=<" + pattern + ">; filter=" + filter);
                    }

                    if (pattern.apply(name)) {
                        logger.debug("fetchNotifications", "pattern matches");
                        if (filter == null
                            || filter.isNotificationEnabled(notif)) {
                            logger.debug("fetchNotifications", 
					 "filter matches");
                            Integer listenerID = li.getListenerID();
                            TargetedNotification tn =
                                new TargetedNotification(notif, listenerID);
                            matchedNotifs.add(tn);
                        }
                    }
                }
            }

            if (matchedNotifs.size() > 0) {
                /* We only check the max size now, so that our
                   returned nextSeq is as large as possible.  This
                   prevents the caller from thinking it missed
                   interesting notifications when in fact we knew they
                   weren't.  */
                if (maxNotifications <= 0) {
                    logger.debug("fetchNotifications", 
				 "reached maxNotifications");
                    break;
                }
                --maxNotifications;
                if (logger.debugOn())
                    logger.debug("fetchNotifications", "add: " + 
				 matchedNotifs);
                notifs.addAll(matchedNotifs);
            }

            ++nextSeq;
        } // end while

        /* Construct and return the result.  */
        int nnotifs = notifs.size();
        TargetedNotification[] resultNotifs =
            new TargetedNotification[nnotifs];
        notifs.toArray(resultNotifs);
        NotificationResult nr =
            new NotificationResult(earliestSeq, nextSeq, resultNotifs);
        if (logger.debugOn())
            logger.debug("fetchNotifications", nr.toString());
        logger.trace("fetchNotifications", "ends");

        return nr;
    }

    synchronized long earliestSequenceNumber() {
        return earliestSequenceNumber;
    }

    synchronized long nextSequenceNumber() {
        return nextSequenceNumber;
    }

    synchronized void addNotification(NamedNotification notif) {
        if (logger.traceOn())
            logger.trace("addNotification", notif.toString());

        while (queue.size() >= queueSize) {
	    dropNotification();
            if (logger.debugOn()) {
                logger.debug("addNotification",
                      "dropped oldest notif, earliestSeq=" +
                      earliestSequenceNumber);
            }
        }
        queue.add(notif);
        nextSequenceNumber++;
        if (logger.debugOn())
            logger.debug("addNotification", "nextSeq=" + nextSequenceNumber);
        notifyAll();
    }

    private void dropNotification() {
	queue.remove(0);
	earliestSequenceNumber++;
    }

    synchronized NamedNotification notificationAt(long seqNo) {
        long index = seqNo - earliestSequenceNumber;
        if (index < 0 || index > Integer.MAX_VALUE) {
            final String msg = "Bad sequence number: " + seqNo + " (earliest "
                + earliestSequenceNumber + ")";
            logger.trace("notificationAt", msg);
            throw new IllegalArgumentException(msg);
        }
        return (NamedNotification) queue.get((int) index);
    }

    private static class NamedNotification {
        NamedNotification(ObjectName sender, Notification notif) {
            this.sender = sender;
            this.notification = notif;
        }

        ObjectName getObjectName() {
            return sender;
        }

        Notification getNotification() {
            return notification;
        }

        public String toString() {
            return "NamedNotification(" + sender + ", " + notification + ")";
        }

        private final ObjectName sender;
        private final Notification notification;
    }

    /*
     * Add our listener to every NotificationBroadcaster MBean
     * currently in the MBean server and to every
     * NotificationBroadcaster later created.
     *
     * It would be really nice if we could just do
     * mbs.addNotificationListener(new ObjectName("*:*"), ...);
     * Definitely something for the next version of JMX.
     *
     * There is a nasty race condition that we must handle.  We
     * first register for MBean-creation notifications so we can add
     * listeners to new MBeans, then we query the existing MBeans to
     * add listeners to them.  The problem is that a new MBean could
     * arrive after we register for creations but before the query has
     * completed.  Then we could see the MBean both in the query and
     * in an MBean-creation notification, and we would end up
     * registering our listener twice.
     *
     * To solve this problem, we arrange for new MBeans that arrive
     * while the query is being done to be added to the Set createdDuringQuery
     * and we do not add a listener immediately.  When the query is done,
     * we atomically turn off the addition of new names to createdDuringQuery
     * and add all the names that were there to the result of the query.
     * Since we are dealing with Sets, the result is the same whether or not
     * the newly-created MBean was included in the query result.
     *
     * It is important not to hold any locks during the operation of adding
     * listeners to MBeans.  An MBean's addNotificationListener can be
     * arbitrary user code, and this could deadlock with any locks we hold
     * (see bug 6239400).  The corollary is that we must not do any operations
     * in this method or the methods it calls that require locks.
     */
    private void createListeners() {
        logger.debug("createListeners", "starts");
        
        synchronized (this) {
            createdDuringQuery = new HashSet();
        }

        try {
            addNotificationListener(delegateName,
                                    creationListener, creationFilter, null);
            logger.debug("createListeners", "added creationListener");
        } catch (Exception e) {
            final String msg = "Can't add listener to MBean server delegate: ";
            RuntimeException re = new IllegalArgumentException(msg + e);
            EnvHelp.initCause(re, e);
            logger.fine("createListeners", msg + e);
            logger.debug("createListeners", e);
            throw re;
        }

        /* Spec doesn't say whether Set returned by QueryNames can be modified
           so we clone it. */
        Set names = queryNames(null, broadcasterQuery);
        names = new HashSet(names);

        synchronized (this) {
            names.addAll(createdDuringQuery);
            createdDuringQuery = null;
        }

	for (Iterator iter=names.iterator(); iter.hasNext();) {
	    ObjectName name = (ObjectName)iter.next();
            addBufferListener(name);
	}

        logger.debug("createListeners", "ends");
    }

    private void addBufferListener(ObjectName name) {
        if (logger.debugOn())
            logger.debug("addBufferListener", name.toString());
        try {
            addNotificationListener(name, bufferListener, null, name);
        } catch (Exception e) {
            logger.trace("addBufferListener", e);
            /* This can happen if the MBean was unregistered just
               after the query.  Or user NotificationBroadcaster might
               throw unexpected exception.  */
        }
    }
    
    private void removeBufferListener(ObjectName name) {
        if (logger.debugOn())
            logger.debug("removeBufferListener", name.toString());
        try {
            removeNotificationListener(name, bufferListener);
        } catch (Exception e) {
            logger.trace("removeBufferListener", e);
        }
    }
    
    private void addNotificationListener(final ObjectName name,
                                         final NotificationListener listener,
                                         final NotificationFilter filter,
                                         final Object handback)
            throws Exception {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws InstanceNotFoundException {
                    mBeanServer.addNotificationListener(name,
                                                        listener,
                                                        filter,
                                                        handback);
                    return null;
                }
            });
        } catch (Exception e) {
            throw extractException(e);
        }
    }
    
    private void removeNotificationListener(final ObjectName name,
                                            final NotificationListener listener)
            throws Exception {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    mBeanServer.removeNotificationListener(name, listener);
                    return null;
                }
            });
        } catch (Exception e) {
            throw extractException(e);
        }
    }
    
    private Set queryNames(final ObjectName name,
                                       final QueryExp query) {
        PrivilegedAction act =
            new PrivilegedAction() {
                public Object run() {
                    return (Set)mBeanServer.queryNames(name, query);
                }
            };
        try {
            return (Set)AccessController.doPrivileged(act);
        } catch (RuntimeException e) {
            logger.fine("queryNames", "Failed to query names: " + e);
	    logger.debug("queryNames", e);
            throw e;
        }
    }
    
    private static boolean isInstanceOf(final MBeanServer mbs,
                                        final ObjectName name,
                                        final String className) {
        PrivilegedExceptionAction act =
            new PrivilegedExceptionAction() {
                public Object run() throws InstanceNotFoundException {
                    return new Boolean(mbs.isInstanceOf(name, className));
                }
            };
        try {
            return ((Boolean)AccessController.doPrivileged(act)).booleanValue();
        } catch (Exception e) {
            logger.fine("isInstanceOf", "failed: " + e);
            logger.debug("isInstanceOf", e);
            return false;
        }
    }

    /* This method must not be synchronized.  See the comment on the
     * createListeners method.
     *
     * The notification could arrive after our buffer has been destroyed
     * or even during its destruction.  So we always add our listener
     * (without synchronization), then we check if the buffer has been
     * destroyed and if so remove the listener we just added.
     */
    private void createdNotification(MBeanServerNotification n) {
        final String shouldEqual =
            MBeanServerNotification.REGISTRATION_NOTIFICATION;
        if (!n.getType().equals(shouldEqual)) {
            logger.warning("createNotification", "bad type: " + n.getType());
            return;
        }

        ObjectName name = n.getMBeanName();
        if (logger.debugOn())
            logger.debug("createdNotification", "for: " + name);
        
        synchronized (this) {
            if (createdDuringQuery != null) {
                createdDuringQuery.add(name);
                return;
            }
        }

        if (isInstanceOf(mBeanServer, name, broadcasterClass)) {
            addBufferListener(name);
            if (isDisposed())
                removeBufferListener(name);
        }
    }

    private class BufferListener implements NotificationListener {
	public void handleNotification(Notification notif, Object handback) {
	    if (logger.debugOn()) {
		logger.debug("BufferListener.handleNotification",
		      "notif=" + notif + "; handback=" + handback);
	    }
	    ObjectName name = (ObjectName) handback;
	    addNotification(new NamedNotification(name, notif));
	}
    }

    private final NotificationListener bufferListener = new BufferListener();

    private static class BroadcasterQuery
            extends QueryEval implements QueryExp {
        public boolean apply(final ObjectName name) {
            final MBeanServer mbs = QueryEval.getMBeanServer();
            return isInstanceOf(mbs, name, broadcasterClass);
        }
    }
    private static final QueryExp broadcasterQuery = new BroadcasterQuery();

    private static final NotificationFilter creationFilter;
    static {
        NotificationFilterSupport nfs = new NotificationFilterSupport();
        nfs.enableType(MBeanServerNotification.REGISTRATION_NOTIFICATION);
        creationFilter = nfs;
    }

    private final NotificationListener creationListener =
	new NotificationListener() {
	    public void handleNotification(Notification notif,
					   Object handback) {
		logger.debug("creationListener", "handleNotification called");
		createdNotification((MBeanServerNotification) notif);
	    }
	};

    private void destroyListeners() {
        logger.debug("destroyListeners", "starts");
        try {
            removeNotificationListener(delegateName,
                                       creationListener);
        } catch (Exception e) {
            logger.warning("remove listener from MBeanServer delegate", e);
        }
        Set names = queryNames(null, broadcasterQuery);
	for (Iterator iter = names.iterator(); iter.hasNext();) {
	    ObjectName name = (ObjectName)iter.next();
            if (logger.debugOn())
                logger.debug("destroyListeners", 
			     "remove listener from " + name);
            removeBufferListener(name);
        }
        logger.debug("destroyListeners", "ends");
    }

    /**
     * Iterate until we extract the real exception
     * from a stack of PrivilegedActionExceptions.
     */
    private static Exception extractException(Exception e) {
        while (e instanceof PrivilegedActionException) {
            e = ((PrivilegedActionException)e).getException(); 
        }
        return e;
    }

    private static final ClassLogger logger =
	new ClassLogger("javax.management.remote.misc",
			"ArrayNotificationBuffer");

    private static final ObjectName delegateName;
    static {
        try {
            delegateName =
                ObjectName.getInstance("JMImplementation:" +
                                       "type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            RuntimeException re =
                new RuntimeException("Can't create delegate name: " + e);
            EnvHelp.initCause(re, e);
            logger.error("<init>", "Can't create delegate name: " + e);
	    logger.debug("<init>",e);
            throw re;
        }
    }

    private final MBeanServer mBeanServer;
    private final ArrayQueue queue;
    private int queueSize;
    private long earliestSequenceNumber;
    private long nextSequenceNumber;
    private Set createdDuringQuery;

    static final String broadcasterClass =
        NotificationBroadcaster.class.getName();
}

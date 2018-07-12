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

import java.util.ArrayList;

class JobExecutor extends Thread {

    /**
     * Constructs a JobExecutor.
     *
     * @param job
     */
    private JobExecutor(Runnable job) {
	super(tgroup, tname + counter++);
	setDaemon(true);

	this.job = job;
    }

    public static void setPoolSize(int size) {
	if (size < 0) {
	    throw new IllegalArgumentException("Negative size.");
	}

	poolSize = size;
    }

    public static int getPoolSize() {
	return poolSize;
    }


    public static void setWaitingTime(long time) {
	if (time < 0) {
	    throw new IllegalArgumentException("Negative waiting time.");
	}

	waitingTime = time;
    }

    public static long getWaitingTime() {
	return waitingTime;
    }


    public void run() {
	while (true) {
	    if (job != null) {
		try {
		    job.run();
		} catch (Exception e) {
		    if (logger.warningOn()) {
			logger.warning("run", "Got an unexpected exception.", e);
		    }
		} finally {
		    job = null;
		}
	    }

	    synchronized(lock) {
		synchronized(waitingList) {
		    if (waitingList.size() >= poolSize) {
			// too many waiting threads, dying
			terminated = true;

			break;
		    } else { // waiting a job
			waitingList.add(this);
		    }
		}

		// waiting	    
		long remainingTime = waitingTime;
		final long startTime = System.currentTimeMillis();

		while (job == null && remainingTime > 0) {
		    try {
			lock.wait(remainingTime);
		    } catch (InterruptedException ie) {
			// in this step, should not happen
			// clean
			this.interrupted();
		    }

		    remainingTime = waitingTime - (System.currentTimeMillis() - startTime);
		}

		// end waiting
		boolean removed;
		synchronized(waitingList) {
		    removed = waitingList.remove(this);
		}
		
		if (!removed) { // let's wait new job
		    while (job == null) {
			try {
			    lock.wait();
			} catch (InterruptedException ire) {
			    // in this step, should not happen
			    // clean
			    this.interrupted();
			}
		    }

		    // go back to execute the job.
		    continue;
		} else { // bye
		    terminated = true;
		    break;		
		}
	    }
	}
    }

    /**
     * Executes a job.
     */
    public static void handoff(Runnable job) {
	JobExecutor je = null;

	synchronized(waitingList) {
	    if (waitingList.size() > 0) {
		je = (JobExecutor)waitingList.remove(0);
	    }
	}

	if (je != null) {
	    synchronized(je.lock) {
		if (!je.terminated) {
		    je.job = job;
		    je.lock.notify(); // wakeup the thread
		} else {
		    je = null;
		}
	    }
	}

	if (je == null) { // no more free thread, create a new one
	    je = new JobExecutor(job);
	    je.start();
	}    }

// instance variables
    private Runnable job = null;
    private int[] lock = new int[0];
    private boolean terminated = false;

// private variables
    private static int poolSize = 20;
    private static ArrayList waitingList = new ArrayList(poolSize);

    private static long counter = 0;
    private static long waitingTime = 300000; // 5 minutes

    private static final ThreadGroup tgroup = new ThreadGroup("Job_Executor");
    private static final String tname = "Job_Executor";

    private static final ClassLogger logger = 
	new ClassLogger("com.sun.jmx.remote.opt.util", 
			"JobExecutor");
}

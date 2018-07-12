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

package com.sun.jmx.remote.opt.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;
import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * <p>This class represents a username/password based implementation of
 * the {@link JMXAuthenticator} interface.</p>
 *
 * <p>The set of username/password pairs is passed either as a
 * filename which denotes a properties file on disk, or directly as an
 * instance of the {@link Properties} class.  In both cases, the name
 * of each property represents a username, and the value of the
 * property is the associated password.  Thus, any given username
 * either does not exist in the properties or has exactly one
 * password.  The same password can be shared by several
 * usernames.</p>
 *
 * <p>If authentication is successful then an authenticated {@link Subject}
 * filled in with a {@link JMXPrincipal} is returned.  Authorization checks
 * will then be performed based on this <code>Subject</code>.</p>
 */
public class JMXPasswordAuthenticator implements JMXAuthenticator {

    /**
     * Creates an instance of <code>JMXPasswordAuthenticator</code>
     * and initializes it from the specified properties file.
     *
     * @param pwFile name of the file which denotes a properties
     * file on disk containing the username/password entries.
     *
     * @exception IOException if the file does not exist, is a
     * directory rather than a regular file, or for some other
     * reason cannot be opened for reading.
     */
    public JMXPasswordAuthenticator(String pwFile) throws IOException {
	this.pwFile = pwFile;
	props = propertiesFromFile(pwFile);
    }

    /**
     * Creates an instance of <code>JMXPasswordAuthenticator</code>
     * and initializes it from the specified properties instance.
     * This constructor makes a copy of the properties instance using
     * its <code>clone</code> method and it is the copy that is
     * consulted to check the username and password of an incoming
     * connection.  The original properties object can be modified
     * without affecting the copy.  If the {@link #refresh} method is
     * then called, the authenticator will make a new copy of the
     * properties object at that time.
     *
     * @param pwProps properties list containing the username/password
     * entries.
     *
     * @exception IllegalArgumentException if <code>pwProps</code> is
     * null.
     */
    public JMXPasswordAuthenticator(Properties pwProps) {
	if (pwProps == null)
	    throw new IllegalArgumentException("Null properties");
	originalProps = pwProps;
	props = (Properties) pwProps.clone();
    }

    /**
     * Authenticate the <code>MBeanServerConnection</code> client
     * with the given client credentials.
     *
     * @param credentials the user-defined credentials to be passed in
     * to the server in order to authenticate the user before creating
     * the <code>MBeanServerConnection</code>.  This parameter must
     * be a two-element <code>String[]</code> containing the client's
     * username and password in that order.
     *
     * @return the authenticated subject containing a
     * <code>JMXPrincipal(username)</code>.
     *
     * @exception SecurityException if the server cannot authenticate the user
     * with the provided credentials.
     */
    public Subject authenticate(Object credentials) {
	// Verify that credentials is of type String[].
	//
	if (!(credentials instanceof String[])) {
	    // Special case for null so we get a more informative message
	    if (credentials == null)
		authenticationFailure("authenticate", "Credentials required");

	    final String message =
		"Credentials should be String[] instead of " +
		((credentials == null) ? "null" :
		 credentials.getClass().getName());
	    authenticationFailure("authenticate", message);
	}
	// Verify that the array contains two elements.
	//
	final String[] aCredentials = (String[]) credentials;
	if (aCredentials.length != 2) {
	    final String message =
		"Credentials should have 2 elements not " +
		aCredentials.length;
	    authenticationFailure("authenticate", message);
	}
	// Verify that username exists and the associated
	// password matches the one supplied by the client.
	//
	final String username = (String) aCredentials[0];
	final String password = (String) aCredentials[1];
	if (username == null || password == null) {
	    final String message = "Username or password is null";
	    authenticationFailure("authenticate", message);
	}
	String localPasswd;
	synchronized (props) {
	    localPasswd = props.getProperty(username);  // can be null
	}
	if (password.equals(localPasswd)) {
	    return
		new Subject(true,
			    Collections.singleton(new JMXPrincipal(username)),
			    Collections.EMPTY_SET,
			    Collections.EMPTY_SET);
	} else {
	    if (props.containsKey(username)) {
		final String message =
		    "Invalid password for username [" + username + "]";
		authenticationFailure("authenticate", message);
	    } else {
		final String message = "Invalid username/password";
		authenticationFailure("authenticate", message);
	    }
	}
	return null;
    }

    /**
     * <p>Refresh the set of username/password entries.</p>
     *
     * <p>If this instance was created using the {@link
     * #JMXPasswordAuthenticator(String)} constructor to specify a
     * file from which the entries are read, the file is re-read.</p>
     *
     * <p>If this instance was created using the
     * {@link #JMXPasswordAuthenticator(Properties)} constructor
     * then a new copy of the <code>Properties</code> object is made.</p>
     *
     * @exception IOException if the file does not exist, is a
     * directory rather than a regular file, or for some other
     * reason cannot be opened for reading.
     */
    public void refresh() throws IOException {
	synchronized (props) {
	    if (pwFile == null)
		props = (Properties) originalProps.clone();
	    else
		props = propertiesFromFile(pwFile);
	}
    }

    private static Properties propertiesFromFile(String fname)
	throws IOException {
	FileInputStream fin = new FileInputStream(fname);
	Properties p = new Properties();
	p.load(fin);
	fin.close();
	return p;
    }

    private static void authenticationFailure(String method, String message)
	throws SecurityException {
	final String msg = "Authentication failed! " + message;
	final SecurityException e = new SecurityException(msg);
	logException(method, msg, e);
	throw e;
    }

    private static void logException(String method,
				     String message,
				     Exception e) {
	if (logger.traceOn()) {
	    logger.trace(method, message);
	}
	if (logger.debugOn()) {
	    logger.debug(method, e);
	}
    }

    private Properties props;
    private Properties originalProps;
    private String pwFile;
    private static final ClassLogger logger =
	new ClassLogger("javax.management.remote.misc",
			"JMXPasswordAuthenticator");
}

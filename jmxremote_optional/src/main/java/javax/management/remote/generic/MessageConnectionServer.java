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

package javax.management.remote.generic;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

/**
 * <p>Interface specifying how a connector server creates new
 * connections to clients.</p>
 *
 * <p>An instance of this interface can be communicated to the Generic
 * Conenctor Server using the attribute {@link
 * GenericConnectorServer#MESSAGE_CONNECTION_SERVER} in the
 * <code>Map</code> passed to its constructor.</p>
 */
public interface MessageConnectionServer {

    /**
     * <p>Activates this server for new client connections.  Before
     * this call is made, new client connections are not accepted.
     * The behavior is unspecified if this method is called more than
     * once.
     *
     * @param env the properties of the connector server.
     *
     * @exception IOException if the server cannot be activated.
     */
    public void start(Map env) throws IOException;

    /**
     * <p>Listens for a connection to be made to this server and
     * accepts it.  The method blocks until a connection is made.</p>
     * 
     * @return a new <code>MessageConnection</code> object.
     *
     * @exception IOException if an I/O error occurs when waiting for
     * a connection.
     */
    public MessageConnection accept() throws IOException;

    /**
     * <p>Terminates this server.  On return from this method, new
     * connection attempts are refused.  Existing connections are
     * unaffected by this call.  The behavior is unspecified if
     * this method is called before the {@link #start} method.</p>
     *
     * @exception IOException if an I/O error occurs when stopping the
     * server.  A best effort will have been made to clean up the
     * server's resources.  The caller will not call {@link #accept}
     * after <code>stop()</code>, whether or not it gets
     * <code>IOException</code>.
     */
    public void stop() throws IOException;

    /**
     * <p>The address of this connection server.</p>
     *
     * @return the address of this connection server, or null if it does
     * not have one.
     */
    public JMXServiceURL getAddress();
}

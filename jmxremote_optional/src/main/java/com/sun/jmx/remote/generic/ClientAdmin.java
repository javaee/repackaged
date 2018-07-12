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

package com.sun.jmx.remote.generic;

import java.io.IOException;

import javax.management.remote.generic.MessageConnection;

/**
 * This interface specifies methods to administrate a client transport connection.
 */
public interface ClientAdmin {
    /**
     * Tells that a transport connection has been established and an
     * implementation of this method is called to verify this connection,
     * for example, to do authentication, to add a encription level to secure
     * the connection etc.
     *
     * @param mc an <code>MessageConnection</code> object which has been established.
     * @return an <code>MessageConnection</code> object enhansed.
     * @exception IOException Any of the usual Input/Output related exceptions during the verification.
     * @exception SecurityException if the verification failed.
     */
    public MessageConnection connectionOpen(MessageConnection mc) throws IOException;

    /**
     * Informs that a transport connection has been closed.
     *
     * @param mc a <code>MessageConnection</code> object which has been closed.
     */
    public void connectionClosed(MessageConnection mc);

    /**
     * Returns a client id specified by its server. This id is received from the <code>HandshakeBeginMessage</code>
     */
    public String getConnectionId();
}

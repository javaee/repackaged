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

package javax.management.remote.message;

/**
 * <p>Handshake end message exchanged between the client and the server.
 * From client to server it signals the completion of the profile
 * exchanges.  From server to client it signals the acceptance of the
 * negotiated profiles by the server.</p>
 *
 * <p>When the connection between the client and the server is
 * established the server sends a {@link HandshakeBeginMessage
 * handshake begin} message to the client with all the server's
 * supported profiles. Then, the client selects the profiles it wants
 * to use and starts exchanging messages with the server for the
 * selected profiles. Once the profile exchanges between the client
 * and server are completed the client sends this message to notify
 * the server that the handshake exchanges have been completed.  The
 * server replies with its own <code>HandshakeEndMessage</code> to
 * confirm that it is ready to operate with the selected profiles, and
 * to indicate the connection ID it has created; or, if it is not
 * prepared to operate with the selected profiles, it replies with a
 * {@link HandshakeErrorMessage}.</p>
 *
 * <p>If an error is encountered at any time, either on the client or
 * the server side, either peer can send an {@link
 * HandshakeErrorMessage indication} as to why the operation
 * failed.</p>
 *
 * <p>The context object exchanged between the client and the server
 * is an opaque (serializable) object that is conveyed within this
 * <code>HandshakeEndMessage</code> message.</p>
 */
public class HandshakeEndMessage implements Message {

    private static final long serialVersionUID = 4962683653394718305L;

    /**
     * @serial The context object (opaque).
     * @see #getContext()
     **/
    private final Object context;

    /**
     * @serial The connection ID.
     * @see #getConnectionId()
     **/
    private final String connectionId;

    /**
     * Constructs a new HandshakeEndMessage with the opaque context
     * object and connection ID.
     *
     * @param context an opaque serializable object to be sent to the
     * other end of the connection.
     *
     * @param connectionId the ID that the server has assigned to this
     * connection.  This parameter is ignored if this is a message
     * from the client to the server.
     */
    public HandshakeEndMessage(Object context, String connectionId) {
	this.context = context;
	this.connectionId = connectionId;
    }

    /**
     * The context object. The actual implementation of this object
     * is opaque.
     * @return The opaque context object.
     */
    public Object getContext() {
	return context;
    }

    /**
     * The connection ID.
     * @return The connection ID.
     */
    public String getConnectionId() {
	return connectionId;
    }
}

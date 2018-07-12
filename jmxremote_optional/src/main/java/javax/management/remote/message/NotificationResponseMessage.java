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

import javax.management.remote.NotificationResult;
import javax.management.remote.generic.ObjectWrapping;

/**
 * <p>Message that transports one or more notifications emitted from
 * an MBean server through a connector server to listeners in a remote
 * client.</p>
 *
 * <p>This message is sent from a server to a client in response to a
 * previous {@link NotificationRequestMessage} from the client.</p>
 */
public class NotificationResponseMessage implements Message {

    private static final long serialVersionUID = -4727296267713643966L;

    /**
     * <p>Constructs a <code>NotificationResponseMessage</code> object.
     *
     * @param wrappedNotificationResult notifications returned to the
     * caller.  This is a {@link NotificationResult} object wrapped
     * using the {@link ObjectWrapping} for the connection using this
     * message.
     *
     * @exception NullPointerException if
     * <code>wrappedNotificationResult</code> is null.
     */
    public NotificationResponseMessage(Object wrappedNotificationResult) {
	if (wrappedNotificationResult == null)
	    throw new NullPointerException("wrappedNotificationResult");
	this.wrappedNotificationResult = wrappedNotificationResult;
    }

    /**
     * Returns the notification result.
     *
     * @return an object that wraps a {@link NotificationResult} using
     * the {@link ObjectWrapping} for the connection using this message.
     **/
    public Object getWrappedNotificationResult() {
	return wrappedNotificationResult;
    }

    /**
     * @serial Notifications returned to the caller.  This is a {@link
     * NotificationResult} object wrapped using the {@link
     * ObjectWrapping} for the connection using this message.
     */
    private final Object wrappedNotificationResult;
}

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

import java.util.List;
import java.util.Map;

public interface CheckProfiles {
    /**
     * Check that the negotiated profiles are acceptable for the server's
     * defined security policy. This method is called just before the initial
     * handshake is completed with a
     * {@link javax.management.remote.message.HandshakeEndMessage} sent from
     * the server to the client. If the method throws an exception, then a
     * {@link javax.management.remote.message.HandshakeErrorMessage} will be
     * sent instead.
     *
     * @param env the environment map passed in at connection server
     * creation time. It might contain the server supported profiles
     * passed in using the jmx.remote.profiles property.
     * @param clientProfiles the list of the names of the profiles
     * negotiated between the client and the server.
     * @param clientContext the context sent by the client.
     * @param connectionId the connection id assigned by the server
     * to this given connection.
     *
     * @exception Exception if the negotiated profiles do not fulfil
     * the expectations of the server's defined security policy.
     */
    public void checkProfiles(Map env,
			      List clientProfiles,
			      Object clientContext,
			      String connectionId)
	throws Exception;
}

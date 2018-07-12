/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1999-2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.sasl.misc; 

import com.sun.security.sasl.preview.*;
import java.io.*;

/**
  * Implements the PLAIN SASL mechanism. 
  * (<A 
  * HREF="http://ftp.isi.edu/in-notes/rfc2595.txt">RFC 2595</A></td>)
  *
  * @author Rosanna Lee
  */
public class Plain implements SaslClient {
    private boolean completed = false;
    private byte[] pw;
    private String authorizationID;
    private String authenticationID;
    private static byte SEP = 0; // US-ASCII <NUL>

    /**
     * Creates a SASL mechanism with client credentials that it needs 
     * to participate in Plain authentication exchange with the server.
     *
     * @param authorizationID A possibly null string representing the principal 
     *  for which authorization is being granted; if null, usame as
     *  authenticationID
     * @param authenticationID A non-null string representing the principal 
     * being authenticated. pw is associated with with this principal.
     * @param pw A non-null byte[] containing the password. 
     */
    public Plain(String authorizationID, String authenticationID, byte[] pw) 
    throws SaslException {
	if (authenticationID == null || pw == null) {
	    throw new SaslException(
		"PLAIN: authorization ID and password must be specified");
	}
	
	this.authorizationID = authorizationID;
	this.authenticationID = authenticationID;
	this.pw = pw;  // caller should have already cloned
    }

    /**
     * Retrieves this mechanism's name for to initiate the PLAIN protocol
     * exchange.
     *
     * @return  The string "PLAIN".
     */
    public String getMechanismName() {
	return "PLAIN";
    }

    public boolean hasInitialResponse() {
	return true;
    }

    public void dispose() throws SaslException {
    }

    /**
     * Retrieves the initial response for the SASL command, which for
     * PLAIN is the concatenation of authorization ID, authentication ID
     * and password, with each component separated by the US-ASCII <NUL> byte.
     * 
     * @param challengeData Ignored
     * @return A non-null byte array containing the response to be sent to the server.
     * @throws SaslException If cannot encode ids in UTF-8, or if already completed.
     */
    public byte[] evaluateChallenge(byte[] challengeData) 
	throws SaslException {
	if (completed) {
	    throw new SaslException("Already completed");
	}
	completed = true;

	try {
	    byte[] authz = (authorizationID != null)? 
		authorizationID.getBytes("UTF8") :
		null;
	    byte[] auth = authenticationID.getBytes("UTF8");

	    byte[] answer = new byte[pw.length + auth.length + 2 +
		(authz == null ? 0 : authz.length)];

	    int pos = 0;
	    if (authz != null) {
		System.arraycopy(authz, 0, answer, 0, authz.length);
		pos = authz.length;
	    }
	    answer[pos++] = SEP;
	    System.arraycopy(auth, 0, answer, pos, auth.length);

	    pos += auth.length;
	    answer[pos++] = SEP;

	    System.arraycopy(pw, 0, answer, pos, pw.length);

	    clearPassword();
	    return answer;
	} catch (java.io.UnsupportedEncodingException e) {
	    throw new SaslException("Cannot get UTF-8 encoding of ids", e);
	}
    }

    /**
     * Determines whether this mechanism has completed.
     * Plain completes after returning one response.
     *
     * @return true if has completed; false otherwise;
     */
    public boolean isComplete() {
	return completed;
    }

    /**
      * Unwraps the incoming buffer.
      *
      * @throws SaslException Not applicable to this mechanism.
      */
    public byte[] unwrap(byte[] incoming, int offset, int len)
	throws SaslException {
	if (completed) {
	    throw new SaslException(
		"PLAIN supports neither integrity nor privacy");
	} else {
	    throw new SaslException("Not completed");
	}
    }

    /**
      * Wraps the outgoing buffer.
      *
      * @throws SaslException Not applicable to this mechanism.
      */
    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
	if (completed) {
	    throw new SaslException(
		"PLAIN supports neither integrity nor privacy");
	} else {
	    throw new SaslException("Not completed");
	}
    }

    /**
     * Retrieves the negotiated property.
     * This method can be called only after the authentication exchange has
     * completed (i.e., when <tt>isComplete()</tt> returns true); otherwise, a
     * <tt>SaslException</tt> is thrown.
     * 
     * @return value of property; only QOP is applicable to PLAIN.
     * @exception SaslException if this authentication exchange has not completed
     */
    public String getNegotiatedProperty(String propName) throws SaslException {
	if (completed) {
	    if (propName.equals(Sasl.QOP)) {
		return "auth";
	    } else {
		return null;
	    }
	} else {
	    throw new SaslException("Not completed");
	}
    }

    private void clearPassword() {
	if (pw != null) {
	    // zero out password
	    for (int i = 0; i < pw.length; i++) {
		pw[i] = (byte)0;
	    }
	    pw = null;
	}
    }

    protected void finalize() {
	clearPassword();
    }
}

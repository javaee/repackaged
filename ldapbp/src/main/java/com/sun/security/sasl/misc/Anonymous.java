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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.sun.security.sasl.preview.*;

/**
  * Implements the ANONYMOUS SASL mechanism. 
  * (<A HREF="ftp://ftp.isi.edu/in-notes/rfc2245.txt">RFC 2245</A>).
  * The Anonymous mechanism sends the trace information that it is given
  * (such as an email or distinguished name) as the initial response
  * and then it is complete.
  *
  * @author Rosanna Lee
  */
public class Anonymous implements SaslClient {
    private byte[] trace;
    private boolean completed = false;

    /**
     * Constructs an Anonymous mechanism with optional trace information.
     * 
     * @param traceInfo If non-null, the string will be used for trace information.
     * @exception SaslException If cannot encode traceInfo in UTF-8
     */
    public Anonymous(String traceInfo) throws SaslException {
	if (traceInfo instanceof String) {
	    try {
		trace = ((String)traceInfo).getBytes("UTF8");
	    } catch (java.io.UnsupportedEncodingException e) {
		throw new SaslException("Cannot encode trace info in UTF-8", e);
	    }
	} else {
	    trace = new byte[0];
	}
    }

    /**
     * Retrieves this mechanim's name for initiating the Anonymous protocol
     * exchange.
     *
     * @return  The string "ANONYMOUS".
     */
    public String getMechanismName() {
	return "ANONYMOUS";
    }

    public boolean hasInitialResponse() {
	return true;
    }

    public void dispose() throws SaslException {
    }

    /**
     * Processes the challenge data. 
     * It returns the ANONYMOUS mechanism's initial response, 
     * which is the trace information encoded in UTF-8.
     * This is the optional information that is sent along with the SASL command.
     * After this method is called, isComplete() returns true.
     * 
     * @param challengeData Ignored.
     * @return The possibly empty initial response (username) 
     * @throws SaslException If authentication already been complete.
     */
    public byte[] evaluateChallenge(byte[] challengeData) throws SaslException {
	if (completed) {
	    throw new SaslException("Already completed");
	}
	completed = true;
	return trace;
    }

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
	    throw new SaslException("ANONYMOUS has no supported QOP");
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
	    throw new SaslException("ANONYMOUS has no supported QOP");
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
     * @return null No property is applicable to this mechanism.
     * @exception SaslException if this authentication exchange has not completed
     */
    public String getNegotiatedProperty(String propName) throws SaslException {
	if (completed) {
	    return null;
	} else {
	    throw new SaslException("Not completed");
	}
    }
}

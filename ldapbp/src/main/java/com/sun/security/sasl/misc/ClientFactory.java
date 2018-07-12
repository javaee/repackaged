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
import com.sun.security.sasl.util.Policy;

import java.util.Map;
import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
  * Client factory for CRAM-MD5, ANONYMOUS, PLAIN.
  *
  * @author Rosanna Lee
  */
public class ClientFactory implements SaslClientFactory {
    private static final String myMechs[] = {
	"CRAM-MD5", // 
	"PLAIN",    // noplaintext
	"ANONYMOUS",// noplaintext
    };

    private static final int mechPolicies[] = {
	Policy.NOPLAINTEXT|Policy.NOANONYMOUS,      // CRAM-MD5
	Policy.NOANONYMOUS, 			    // PLAIN
	Policy.NOPLAINTEXT		            // ANONYMOUS
    };

    private static final int CRAMMD5 = 0;
    private static final int PLAIN = 1;
    private static final int ANONYMOUS = 2;

    private byte[] bytepw;
    private String authId;

    public ClientFactory() {
    }

    public SaslClient createSaslClient(String[] mechs,
	String authorizationId,
	String protocol,
	String serverName,
	Map props,
	CallbackHandler cbh) throws SaslException {

	    for (int i = 0; i < mechs.length; i++) {
		if (mechs[i].equals(myMechs[CRAMMD5])
		    && Policy.checkPolicy(mechPolicies[CRAMMD5], props)) {
		    getUserInfo("CRAM-MD5", authorizationId, cbh);

		    // Callee responsible for clearing bytepw
		    return new CramMD5(authId, bytepw);

		} else if (mechs[i].equals(myMechs[PLAIN])
		    && Policy.checkPolicy(mechPolicies[PLAIN], props)) {
		    getUserInfo("PLAIN", authorizationId, cbh);

		    // Callee responsible for clearing bytepw
		    return new Plain(authorizationId, authId, bytepw);

		} else if (mechs[i].equals(myMechs[ANONYMOUS])
		    && Policy.checkPolicy(mechPolicies[ANONYMOUS], props)) {
		    return new Anonymous(authorizationId);
		}
	    }
	    return null;
    };

    public String[] getMechanismNames(Map props) {
	return Policy.filterMechs(myMechs, mechPolicies, props);
    }

    /**
     * Gets the authentication id and password. The
     * password is converted to bytes using UTF-8 and stored in bytepw.
     * The authentication id is stored in authId.
     *
     * @param prefix The non-null prefix to use for the prompts (e.g., mechanism
     *  name)
     * @param authorizationId The possibly null authorization id. This is used
     * as a default for the NameCallback. If null, no prefix.
     * @param cbh The non-null callback handler to use.
     */
    private void getUserInfo(String prefix, String authorizationId, 
	CallbackHandler cbh) throws SaslException {
	try {
	    String userPrompt = prefix + " authentication id: ";
	    String passwdPrompt = prefix + " password: ";

	    NameCallback ncb = authorizationId == null? 
		new NameCallback(userPrompt) :
		new NameCallback(userPrompt, authorizationId);

	    PasswordCallback pcb = new PasswordCallback(passwdPrompt, false);

	    cbh.handle(new Callback[]{ncb,pcb});

	    char[] pw = pcb.getPassword();

	    if (pw != null) {
		bytepw = new String(pw).getBytes("UTF8");
		pcb.clearPassword();
	    } else {
		bytepw = null;
	    }

	    authId = ncb.getName();

	} catch (IOException e) {
	    throw new SaslException("Cannot get password", e);
	} catch (UnsupportedCallbackException e) {
	    throw new SaslException("Cannot get userid/password", e);
	}
    }
}

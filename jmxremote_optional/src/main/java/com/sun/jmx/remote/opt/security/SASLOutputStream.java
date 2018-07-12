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

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.jmx.remote.opt.util.ClassLogger;

public class SASLOutputStream extends OutputStream {

    private int rawSendSize = 65536;
    private byte[] lenBuf = new byte[4];  // buffer for storing length
    private OutputStream out;		  // underlying output stream
    private SaslClient sc;
    private SaslServer ss;

    public SASLOutputStream(SaslClient sc, OutputStream out)
	throws IOException {

	super();
	this.out = out;
	this.sc = sc;
	this.ss = null;

	String str = (String) sc.getNegotiatedProperty(Sasl.RAW_SEND_SIZE);
	if (str != null) {
	    try {
		rawSendSize = Integer.parseInt(str);
	    } catch (NumberFormatException e) {
		throw new IOException(Sasl.RAW_SEND_SIZE +
		    " property must be numeric string: " + str);
	    }
	}
    }

    public SASLOutputStream(SaslServer ss, OutputStream out)
	throws IOException {

	super();
	this.out = out;
	this.ss = ss;
	this.sc = null;

	String str = (String) ss.getNegotiatedProperty(Sasl.RAW_SEND_SIZE);
	if (str != null) {
	    try {
		rawSendSize = Integer.parseInt(str);
	    } catch (NumberFormatException e) {
		throw new IOException(Sasl.RAW_SEND_SIZE +
		    " property must be numeric string: " + str);
	    }
	}
    }

    public void write(int b) throws IOException {
	byte[] buffer = new byte[1];
	buffer[0] = (byte)b;
	write(buffer, 0, 1);
    }

    public void write(byte[] buffer, int offset, int total) throws IOException {
	int count;
	byte[] wrappedToken, saslBuffer;
	    
	// "Packetize" buffer to be within rawSendSize
	if (logger.traceOn()) {
	    logger.trace("write", "Total size: " + total);
	}

	for (int i = 0; i < total; i += rawSendSize) {

	    // Calculate length of current "packet"
	    count = (total - i) < rawSendSize ? (total - i) : rawSendSize;

	    // Generate wrapped token 
	    if (sc != null)
		wrappedToken = sc.wrap(buffer, offset+i, count);
	    else
		wrappedToken = ss.wrap(buffer, offset+i, count);

	    // Write out length
	    intToNetworkByteOrder(wrappedToken.length, lenBuf, 0, 4);

	    if (logger.traceOn()) {
		logger.trace("write", "sending size: " + wrappedToken.length);
	    }

	    out.write(lenBuf, 0, 4);

	    // Write out wrapped token
	    out.write(wrappedToken, 0, wrappedToken.length);
	}
    }

    public void close() throws IOException {
	if (sc != null)
	    sc.dispose();
	else
	    ss.dispose();
	out.close();
    }

    /**
     * Encodes an integer into 4 bytes in network byte order in the buffer
     * supplied.
     */
    private void intToNetworkByteOrder(int num, byte[] buf,
				       int start, int count) {
	if (count > 4) {
	    throw new IllegalArgumentException("Cannot handle more " +
					       "than 4 bytes");
	}
	for (int i = count-1; i >= 0; i--) {
	    buf[start+i] = (byte)(num & 0xff);
	    num >>>= 8;
	}
    }

    private static final ClassLogger logger =
	new ClassLogger("javax.management.remote.misc", "SASLOutputStream");
}

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

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.io.Serializable;

/**
 * <p>An interface that defines how to wrap parameters that use a
 * non-default class loader.  A {@link GenericConnector} can specify
 * an instance of this interface to define a connection-specific
 * wrapping.</p>
 *
 * <p>Certain parameters to {@link
 * javax.management.MBeanServerConnection MBeanServerConnection}
 * methods have to be wrapped because their class loader is not
 * necessarily known to the remote connector server.</p>
 *
 * <p>For example, when calling {@link
 * javax.management.MBeanServerConnection#setAttribute setAttribute} on an MBean
 * <em>X</em>, the attribute value <em>v</em> to be set might be of a
 * class that is known to <em>X</em>'s class loader but not to the
 * class loader of the connector server.  If <em>v</em> were not
 * wrapped, the connector server would receive it at the same time as
 * it received other information such as <em>X</em>'s
 * <code>ObjectName</code>.  The whole request would fail because of
 * the inability to find the <em>v</em>'s class.</p>
 *
 * <p>Object wrapping solves this problem by encoding <em>v</em>
 * inside an object of a type that is known to the connector server,
 * such as <code>byte[]</code> or <code>String</code>.  Then
 * <em>v</em> is recreated using <em>X</em>'s class loader, which the
 * connector server can know once <em>X</em>'s name has been
 * successfully received.</p>
 *
 * <p>An instance of this class can be communicated to the Generic
 * Connector or Generic Connector Server using the attribute {@link
 * GenericConnector#OBJECT_WRAPPING}.</p>
 *
 * <p>The default <code>ObjectWrapping</code> wraps objects in a byte
 * array that contains the output of {@link
 * java.io.ObjectOutputStream#writeObject(Object)
 * ObjectOutputStream.writeObject} for the given object in a new
 * <code>ObjectOutputStream</code>.
 */
public interface ObjectWrapping {
    /**
     * Wraps an object.
     *
     * @param obj the object to be wrapped.
     * @return the wrapped object.
     * @exception IOException if the object cannot be wrapped
     * for some reason.
     */
    public Object wrap(Object obj) throws IOException;

    /**
     * Unwraps an object.
     *
     * @param wrapped the wrapped object to be unwrapped.
     * @param cloader the class loader to be used to load the object's
     * class.  Can be null, meaning the bootstrap class loader.
     * @return the unwrapped object.
     * @exception ClassNotFoundException if the class that the
     * unwrapped object should have is not known to the given class
     * loader.
     * @exception IOException if the object cannot be unwrapped
     * for some reason.
     */
    public Object unwrap(Object wrapped, ClassLoader cloader)
	    throws IOException, ClassNotFoundException;

}

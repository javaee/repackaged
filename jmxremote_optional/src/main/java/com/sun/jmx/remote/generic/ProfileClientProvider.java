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

import java.util.Map;

/**
 * <p>A provider for creating profiles given a profile name. Instances of
 * this interface are created by {@link ProfileClientFactory} as part of its
 * {@link ProfileClientFactory#createProfile(String,Map) createProfile}
 * method.</p>
 */
public interface ProfileClientProvider {

    /**
     * <p>Create a new profile. Each successful call to this method
     * produces a different <code>ProfileClient</code> object.</p>
     *
     * @param profile the name of the profile to be created.
     *
     * @param environment a read-only Map containing named attributes
     * to determine how the profile is created. Keys in this map must
     * be Strings. The appropriate type of each associated value
     * depends on the attribute.</p>
     *
     * @return a <code>ProfileClient</code> representing the new profile.
     * Each successful call to this method produces a different object.
     *
     * @exception NullPointerException if <code>profile</code> or
     * <code>environment</code> is null.
     */
    public ProfileClient createProfile(String profile, Map environment)
	throws ProfileProviderException;
}

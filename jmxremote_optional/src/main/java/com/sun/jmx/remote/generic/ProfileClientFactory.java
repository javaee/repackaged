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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <p>Factory to create profiles. There are no instances of this class.</p>
 *
 * <p>Each profile is created by an instance of {@link ProfileClientProvider}.
 * This instance is found as follows. Suppose the given
 * <code><em>profile</em></code> looks like <code>TLS</code>. Then the factory
 * will attempt to find the appropriate {@link ProfileClientProvider} for
 * <code><em>tls</em></code>. Suppose the given <code><em>profile</em></code>
 * looks like <code>SASL/PLAIN</code>. Then the factory will attempt to find
 * the appropriate {@link ProfileClientProvider} for <code><em>sasl</em></code>.
 * The <code><em>profile</em></code> string passed in to the factory is
 * converted into lowercase and all the characters after the <code>/</code>
 * character are discarded.</p>
 *
 * <p>A <em>provider package list</em> is searched for as follows:</p>
 *
 * <ol>
 *
 * <li>If the <code>environment</code> parameter to {@link
 * #createProfile(String, Map) createProfile} contains the key
 * <code>jmx.remote.profile.provider.pkgs</code> then the associated
 * value is the provider package list.
 *
 * <li>Otherwise, if the system property
 * <code>jmx.remote.profile.provider.pkgs</code> exists, then its value
 * is the provider package list.
 *
 * <li>Otherwise, there is no provider package list.
 *
 * </ol>
 *
 * <p>The provider package list is a string that is interpreted as a
 * list of non-empty Java package names separated by vertical bars
 * (<code>|</code>). If the string is empty, then so is the provider
 * package list. If the provider package list is not a String, or if
 * it contains an element that is an empty string, a {@link
 * ProfileProviderException} is thrown.</p>
 *
 * <p>If the provider package list exists and is not empty, then for
 * each element <code><em>pkg</em></code> of the list, the factory
 * will attempt to load the class
 *
 * <blockquote>
 * <code><em>pkg</em>.<em>profile</em>.ClientProvider</code>
 * </blockquote>
 *
 * <p>If the <code>environment</code> parameter to {@link
 * #createProfile(String, Map) createProfile} contains the key
 * <code>jmx.remote.profile.provider.class.loader</code> then the
 * associated value is the class loader to use to load the provider.
 * If the associated value is not an instance of 
 * {@link java.lang.ClassLoader}, an {@link
 * java.lang.IllegalArgumentException} is thrown.</p>
 * 
 * <p>If the <code>jmx.remote.profile.provider.class.loader</code>
 * key is not present in the <code>environment</code> parameter, the
 * class loader that loaded the <code>ProfileClientFactory</code> class 
 * is used.</p>
 *
 * <p>If the attempt to load this class produces a {@link
 * ClassNotFoundException}, the search for a provider continues with
 * the next element of the list.</p>
 *
 * <p>Otherwise, a problem with the found provider is signalled by a
 * {@link ProfileProviderException} whose {@link
 * ProfileProviderException#getCause() <em>cause</em>} indicates the underlying
 * exception, as follows:</p>
 *
 * <ul>
 *
 * <li>if the attempt to load the class produces an exception other
 * than <code>ClassNotFoundException</code>, that is the
 * <em>cause</em>;
 *
 * <li>if {@link Class#newInstance()} for the class produces an
 * exception, that is the <em>cause</em>.
 *
 * </ul>
 *
 * <p>If no provider is found by the above steps, including the
 * default case where there is no provider package list, then the
 * implementation will use its own provider for
 * <code><em>profile</em></code>, or it will throw a
 * <code>IllegalArgumentException</code> if there is none.</p>
 *
 * <p>Once a provider is found, the result of the
 * <code>createProfile</code> method is the result of calling {@link
 * ProfileClientProvider#createProfile(String,Map) createProfile}
 * on the provider.</p>
 *
 * <p>The <code>Map</code> parameter passed to the
 * <code>ProfileClientProvider</code> is a new read-only copy of the
 * <code>environment</code> parameter to {@link
 * #createProfile(String, Map)
 * ProfileClientFactory.createProfile}, or an empty <code>Map</code> if
 * that parameter is null.  If the
 * <code>jmx.remote.profile.provider.class.loader</code> key is not
 * present in the <code>environment</code> parameter, it is added to the new
 * read-only <code>Map</code>. The associated value is the class loader that
 * loaded the <code>ProfileClientFactory</code> class.</p>
 */
public class ProfileClientFactory {

    /**
     * <p>Name of the attribute that specifies the provider packages
     * that are consulted when looking for the provider for a profile.
     * The value associated with this attribute is a string with
     * package names separated by vertical bars (<code>|</code>).</p>
     */
    public static final String PROFILE_PROVIDER_PACKAGES =
        "jmx.remote.profile.provider.pkgs";

    /**
     * <p>Name of the attribute that specifies the class
     * loader for loading profile providers.
     * The value associated with this attribute is an instance
     * of {@link ClassLoader}.</p>
     */
    public static final String PROFILE_PROVIDER_CLASS_LOADER =
        "jmx.remote.profile.provider.class.loader";

    private static final String PROFILE_PROVIDER_DEFAULT_PACKAGE =
        "com.sun.jmx.remote.profile";

    /**
     * There are no instances of this class.
     */
    private ProfileClientFactory() {
    }

    /**
     * <p>Create a profile.</p>
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
     * @exception NullPointerException if <code>profile</code> is null.
     */
    public static ProfileClient createProfile(String profile, Map environment)
	throws ProfileProviderException {

        final String pkgs = resolvePkgs(environment);

        final ClassLoader loader = resolveClassLoader(environment);

        if (environment == null)
            environment = new HashMap();
        else
            environment = new HashMap(environment);

        environment.put(PROFILE_PROVIDER_CLASS_LOADER, loader);
        environment = Collections.unmodifiableMap(environment);

        final ProfileClientProvider provider =
	    getProvider(profile, pkgs, loader);

        if (provider == null) {
            throw new IllegalArgumentException("Unsupported profile: " +
					       profile);
        }

        return provider.createProfile(profile, environment);
    }

    private static final String resolvePkgs(Map env) {

        String pkgs = null;

        if (env != null)
            pkgs = (String) env.get(PROFILE_PROVIDER_PACKAGES);

        if (pkgs == null)
            pkgs = (String)
		AccessController.doPrivileged(new PrivilegedAction() {
		    public Object run() {
			return System.getProperty(PROFILE_PROVIDER_PACKAGES);
		    }
		});
        if (pkgs == null || pkgs.trim().equals(""))
            pkgs = PROFILE_PROVIDER_DEFAULT_PACKAGE;
        else
            pkgs += "|" + PROFILE_PROVIDER_DEFAULT_PACKAGE;

        return pkgs;
    }

    private static final ProfileClientProvider getProvider(String profile,
							   String pkgs,
							   ClassLoader loader)
	throws ProfileProviderException {
        Class providerClass = null;
        ProfileClientProvider provider = null;
        Object obj = null;

        StringTokenizer tokenizer = new StringTokenizer(pkgs, "|");

	String p = profile.toLowerCase();
	if (p.indexOf("/") != -1) {
	    p = p.substring(0, p.indexOf("/"));
	}
        while (tokenizer.hasMoreTokens()) {
            String pkg = (String) tokenizer.nextToken();
            String className = (pkg + "." + p + ".ClientProvider");
            try {
                providerClass = loader.loadClass(className);
            } catch (ClassNotFoundException e) {
                continue;
            }

            try {
                obj = providerClass.newInstance();
            } catch (Exception e) {
                final String msg =
                    "Exception when instantiating provider [" + className + "]";
                throw new ProfileProviderException(msg, e);
            }

            if (!(obj instanceof ProfileClientProvider)) {
                final String msg =
                    "Provider not an instance of " +
                    ProfileClientProvider.class.getName() + ": " +
                    obj.getClass().getName();
                throw new IllegalArgumentException(msg);
            }

            return (ProfileClientProvider) obj;
        }

        return null;
    }

    private static final ClassLoader resolveClassLoader(Map environment) {
        ClassLoader loader = null;

        if (environment != null) {
            try {
                loader = (ClassLoader)
		    environment.get(PROFILE_PROVIDER_CLASS_LOADER);
            } catch (ClassCastException e) {
                final String msg =
                    "ClassLoader not an instance of java.lang.ClassLoader : " +
                    loader.getClass().getName();
                throw new IllegalArgumentException(msg); 
            }
        }

        if (loader == null)
            loader = ProfileClientFactory.class.getClassLoader();

        return loader;
    }
}

/*
 * Copyright (c) 2007, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.jmx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.management.remote.JMXConnector;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.jmx.impl.JmxApplication;
import org.netbeans.api.keyring.Keyring;

/**
 * EnvironmentProvider adding the {@code JMXConnector.CREDENTIALS} property to the JMX
 * environment map.
 *
 * There are two subclasses of EnvironmentProvider available, typically you want
 * to use the EnvironmentProvider.Custom class to provide a custom credentials
 * for a JMX connection. The EnvironmentProvider.Persistent class is used for
 * handling credentials for persisted connections.
 *
 * Note that if the credentials provided by this provider are incorrect a dialog
 * requesting new credentials will be displayed by the framework. If the
 * user-provided credentials are correct they will override the credentials
 * defined by this provider. The user-provided credentials are never persisted.
 *
 * @author Jiri Sedlacek
 */
public abstract class CredentialsProvider extends EnvironmentProvider {

    private static final String PROPERTY_USER = "prop_credentials_user"; // NOI18N
    private static final String PROPERTY_PWORD = "prop_credentials_pword"; // NOI18N

    private static Persistent PERSISTENT_PROVIDER;


    static synchronized Persistent persistent() {
        if (PERSISTENT_PROVIDER == null) PERSISTENT_PROVIDER = new Persistent();
        return PERSISTENT_PROVIDER;
    }


    /**
     * Returns a unique String identifying the CredentialsProvider. Must be
     * overridden to return a different identificator when subclassing the
     * CredentialsProvider.
     *
     * @return unique String identifying the CredentialsProvider
     */
    public String getId() {
        return CredentialsProvider.class.getName();
    }
    
    
    abstract String getUsername(Storage storage);
    
    abstract boolean hasPassword(Storage storage);

    public abstract boolean isPersistent(Storage storage);


    /**
     * CredentialsProvider to provide custom settings.
     *
     * @author Jiri Sedlacek
     */
    public static class Custom extends CredentialsProvider {

        private final String user;
        private final char[] pword;
        private final boolean persistent;


        /**
         * Creates new instance of CredentialsProvider.Custom.
         *
         * @param username username
         * @param password password
         * @param persistent true if the credentials should be persisted for another VisualVM sessions, false otherwise
         */
        public Custom(String username, char[] password, boolean persistent) {
            this.user = username;
            this.pword = encodePassword(password);
            this.persistent = persistent;
        }


        public Map<String, ?> getEnvironment(Application application, Storage storage) {
            return createMap(user, getPassword(storage));
        }

        public String getEnvironmentId(Storage storage) {
            if (user != null) return user;
            return super.getEnvironmentId(storage);
        }

        public void saveEnvironment(Storage storage) {
            if (!persistent) return;
            storage.setCustomProperty(PROPERTY_USER, user);
            String keyringId = JmxApplication.createId(null, this, storage);
            char[] pw = getPassword(storage);
            if (pw != null) {
                Keyring.save(keyringId, pw, "VisualVM - JMX password for "+user);    // NOI18N
            } else {
                Keyring.delete(keyringId);
            }
        }
        
        
        String getUsername(Storage storage) { return user; }

        private char[] getPassword(Storage storage) {
            if (hasPassword(storage)) {
                return decodePassword(Arrays.copyOf(pword, pword.length));
            }
            return null;
        }
    
        boolean hasPassword(Storage storage) { return pword != null &&
                                               pword.length > 0; }

        public boolean isPersistent(Storage storage) { return persistent; }

    }


    /**
     * CredentialsProvider to provide custom settings.
     *
     * @author Jiri Sedlacek
     */
    public static class Persistent extends CredentialsProvider {

        public Map<String, ?> getEnvironment(Application application, Storage storage) {
            String user = storage.getCustomProperty(PROPERTY_USER);
            return createMap(user, getPassword(storage));
        }

        public String getEnvironmentId(Storage storage) {
            if (storage != null) {
                String user = storage.getCustomProperty(PROPERTY_USER);
                if (user != null) return user;
            }
            return super.getEnvironmentId(storage);
        }


        String getUsername(Storage storage) { return storage.getCustomProperty(
                                                     PROPERTY_USER); }

        private char[] getPassword(Storage storage) {
            String keyringId = JmxApplication.createId(null, this, storage);
            char[] pw = Keyring.read(keyringId);
            if (pw != null) {
                return pw;
            }
            // read old settings
            String pword = storage.getCustomProperty(PROPERTY_PWORD);
            if (pword != null && !pword.isEmpty()) {
                // migrate old settings to Keyring
                // Keyring.save(keyringId, decodePassword(pword.toCharArray()), "VisualVM - JMX password for "+getUsername(storage));       // NOI18N
                // storage.clearCustomProperty(PROPERTY_PWORD);
                //return getPassword(storage);
                return decodePassword(pword.toCharArray());
            }
            return null;
        }

        boolean hasPassword(Storage storage) {
            char[] pword = getPassword(storage);
            return pword != null && pword.length>0;
        }

        public boolean isPersistent(Storage storage) {
            return getUsername(storage) != null || hasPassword(storage);
        }

    }


    // --- Private implementation ----------------------------------------------

    // NOTE: clears the pword parameter!
    private static Map<String, ?> createMap(String username, char[] pword) {
        Map<String, Object>  map = new HashMap<>();

        if (username != null && !username.isEmpty()) {
            map.put(JMXConnector.CREDENTIALS, new String[] { username, pword == null ? null : new String(pword) });
        }
        if (pword != null) Arrays.fill(pword, (char)0);

        return map;
    }

    // NOTE: clears the pword parameter!
    private static char[] encodePassword(char[] pword) {
        return pword == null ? null : Utils.encodePassword(pword);
    }

    // NOTE: clears the pword parameter!
    private static char[] decodePassword(char[] pword) {
        return pword == null ? null : Utils.decodePassword(pword);
    }

}

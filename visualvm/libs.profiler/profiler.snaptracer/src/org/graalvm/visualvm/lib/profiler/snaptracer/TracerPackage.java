/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer;

import javax.swing.Icon;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.IdeSnapshot;

/**
 * TracerPackage is a container for a set of TracerProbes distributed as a single
 * package (plugin). The probes in a package are typically designed to monitor
 * the same functional unit on the target - for example disk I/O, network I/O,
 * memory subsystem etc.
 *
 * @author Jiri Sedlacek
 */
public abstract class TracerPackage implements Positionable {

    private final String name;
    private final String description;
    private final Icon icon;
    private final int preferredPosition;


    /**
     * Creates new instance of TracerPackage.
     *
     * @param name name of the package
     * @param description description of the package
     * @param icon icon of the package
     * @param preferredPosition preferred position of the package in UI
     */
    public TracerPackage(String name, String description, Icon icon,
                         int preferredPosition) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.preferredPosition = preferredPosition;
    }


    /**
     * Returns name of the package.
     *
     * @return name of the package
     */
    public final String getName() { return name; }

    /**
     * Returns description of the package.
     *
     * @return description of the package
     */
    public final String getDescription() { return description; }

    /**
     * Returns icon of the package.
     *
     * @return icon of the package
     */
    public final Icon getIcon() { return icon; }

    /**
     * Returns preferred position of the package in UI.
     *
     * @return preferred position of the package in UI
     */
    public final int getPreferredPosition() { return preferredPosition; }


    /**
     * Returns array of TracerProbeDescriptors to present the package probes in UI.
     * Should always return descriptors for all probes provided by the provider
     * for each DataSource. If a probe is not available for the DataSource its
     * descriptor should be disabled - TracerProbeDescriptor.isProbeAvailable()
     * returns false.
     *
     * @return array of TracerProbeDescriptors to present the package probes in UI
     */
    public abstract TracerProbeDescriptor[] getProbeDescriptors();

    /**
     * Returns the probe to be used in Tracer session. The probe to return
     * is defined by its TracerProbeDescriptor created by getProbeDescriptors()
     * method and selected by the user.
     *
     * @param descriptor TracerProbeDescriptor selecting the probe
     * @return the probe to be used in Tracer session
     */
    public abstract TracerProbe getProbe(TracerProbeDescriptor descriptor);

    
    /**
     * Optionally returns PackageStateHandler instance which obtains notifications
     * about the Tracer session status in context of TracerProbes provided by this
     * TracerPackage. Default implementation returns null. You may use StateAware
     * subclass instead of implementing this method to obtain the notifications.
     *
     * @return PackageStateHandler instance which obtains status notifications,
     * default implementation returns null
     */
    public PackageStateHandler getStateHandler() { return null; }


    /**
     * An abstract adapter class for receiving Tracer session state notifications.
     * See PackageStateHandler for details.
     *
     */
    public static abstract class SessionAware extends TracerPackage {

        private PackageStateHandler stateHandler;


        /**
         * Creates new instance of TracerPackage.SessionAware.
         *
         * @param name name of the package
         * @param description description of the package
         * @param icon icon of the package
         * @param preferredPosition preferred position of the package in UI
         */
        public SessionAware(String name, String description,
                          Icon icon, int preferredPosition) {
            super(name, description, icon, preferredPosition);
        }


        /**
         * Returns a predefined PackageStateHandler which obtains notifications
         * about the Tracer session state in context of TracerProbes provided by this
         * TracerPackage.
         *
         * @return predefined PackageStateHandler which obtains status notifications
         */
        public synchronized final PackageStateHandler getStateHandler() {
            if (stateHandler == null) stateHandler = new PackageStateHandler() {
                public void probeAdded(TracerProbe probe, IdeSnapshot snapshot) {
                    SessionAware.this.probeAdded(probe, snapshot);
                }
                public void probeRemoved(TracerProbe probe, IdeSnapshot snapshot) {
                    SessionAware.this.probeRemoved(probe, snapshot);
                }
                public TracerProgressObject sessionInitializing(TracerProbe[] probes,
                    IdeSnapshot snapshot, int refresh) {
                    return SessionAware.this.sessionInitializing(probes, snapshot, refresh);
                }
                public void sessionStarting(TracerProbe[] probes, IdeSnapshot snapshot)
                        throws SessionInitializationException {
                    SessionAware.this.sessionStarting(probes, snapshot);
                }
                public void sessionRunning(TracerProbe[] probes, IdeSnapshot snapshot) {
                    SessionAware.this.sessionRunning(probes, snapshot);
                }
                public void sessionStopping(TracerProbe[] probes, IdeSnapshot snapshot) {
                    SessionAware.this.sessionStopping(probes, snapshot);
                }
                public void sessionFinished(TracerProbe[] probes, IdeSnapshot snapshot) {
                    SessionAware.this.sessionFinished(probes, snapshot);
                }
                public void refreshRateChanged(TracerProbe[] probes, IdeSnapshot snapshot,
                        int refresh) {
                    SessionAware.this.refreshRateChanged(probes, snapshot, refresh);
                }
            };
            return stateHandler;
        }


        /**
         * Invoked when a probe is added into the Timeline view.
         *
         * @param probe added probe
         * @param snapshot profiler snapshot
         */
        protected void probeAdded(TracerProbe probe, IdeSnapshot snapshot) {}

        /**
         * Invoked when a probe is removed from the Timeline view.
         *
         * @param probe removed probe
         * @param snapshot profiler snapshot
         */
        protected void probeRemoved(TracerProbe probe, IdeSnapshot snapshot) {}


        /**
         * Invoked when setting up a new Tracer session. This method allows a
         * Package to notify the user about initialization progress. The actual
         * initialization (and updating the TracerProgressObject) should be
         * performed in the sessionStarting() method. Useful for example for
         * messaging a delay during instrumentation of classes in target application.
         *
         * @param probes probes defined for the Tracer session
         * @param snapshot profiler snapshot
         * @param refresh session refresh rate in milliseconds
         * @return TracerProgressObject to track initialization progress
         */
        protected TracerProgressObject sessionInitializing(TracerProbe[] probes,
                IdeSnapshot snapshot, int refresh) { return null; }

        /**
         * Invoked when starting a new Tracer session. Any package/probes
         * initialization should be performed in this method. If provided by the
         * sessionInitializing method, a TracerProgressObject should be updated to
         * reflect the initialization progress. This method may throw a
         * SessionInitializationException in case of initialization failure. Any
         * packages/probes initialized so far will be correctly finished, however the
         * package throwing the SessionInitializationException is responsible for
         * cleaning up any used resources and restoring its state without any
         * following events.
         *
         * @param probes probes defined for the Tracer session
         * @param snapshot profiler snapshot
         * @throws SessionInitializationException in case of initialization failure
         */
        protected void sessionStarting(TracerProbe[] probes, IdeSnapshot snapshot)
                throws SessionInitializationException {}

        /**
         * Invoked when all packages/probes have been started and the Tracer session
         * is running and collecting data.
         *
         * @param probes probes defined for the Tracer session
         * @param snapshot profiler snapshot
         */
        protected void sessionRunning(TracerProbe[] probes, IdeSnapshot snapshot) {}

        /**
         * Invoked when stopping the Tracer session. Any package/probes cleanup
         * should be performed in this method. Any long-running cleanup code should
         * preferably be invoked in a separate worker thread to allow the Tracer
         * session to finish as fast as possible. Be sure to check/wait for the
         * cleanup thread when starting a new Tracer session in sessionStarting().
         *
         * @param probes probes defined for the Tracer session
         * @param snapshot profiler snapshot
         */
        protected void sessionStopping(TracerProbe[] probes, IdeSnapshot snapshot) {}

        /**
         * Invoked when the Tracer session has finished.
         *
         * @param probes probes defined for the Tracer session
         * @param snapshot profiler snapshot
         */
        protected void sessionFinished(TracerProbe[] probes, IdeSnapshot snapshot) {}

        /**
         * Invoked when refresh rate of the Tracer session has been changed.
         *
         * @param probes probes defined for the Tracer session
         * @param snapshot profiler snapshot
         * @param refresh session refresh rate in milliseconds
         */
        protected void refreshRateChanged(TracerProbe[] probes, IdeSnapshot snapshot,
                int refresh) {}

    }

}

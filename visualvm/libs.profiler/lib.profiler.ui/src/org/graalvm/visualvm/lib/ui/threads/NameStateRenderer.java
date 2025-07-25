/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.threads;

import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class NameStateRenderer extends LabelRenderer {

    public NameStateRenderer() {
        setOpaque(true);
        setMargin(3, 4, 3, 4);
    }

    public void setValue(Object value, int row) {
        if (value == null) {
            setText(""); // NOI18N
            setIcon(null);
        } else {
            ThreadData data = (ThreadData)value;
            setText(data.getName());
            setIcon(getIcon(data.getLastState()));
        }
    }

    private static final int THREAD_ICON_SIZE = 9;
    private static final Map<Byte, Icon> STATE_ICONS_CACHE = new HashMap<>();
    private static Icon getIcon(byte state) {
        Icon icon = STATE_ICONS_CACHE.get(state);

        if (icon == null) {
            icon = new ThreadStateIcon(state, THREAD_ICON_SIZE, THREAD_ICON_SIZE);
            STATE_ICONS_CACHE.put(state, icon);
        }

        return icon;
    }

}

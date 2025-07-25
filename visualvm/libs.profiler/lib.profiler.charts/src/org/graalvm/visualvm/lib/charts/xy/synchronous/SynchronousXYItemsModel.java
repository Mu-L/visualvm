/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.charts.xy.synchronous;

import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.ChartItemChange;
import org.graalvm.visualvm.lib.charts.ItemsModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public class SynchronousXYItemsModel extends ItemsModel.Abstract {

    private final ArrayList<SynchronousXYItem> items = new ArrayList<>();
    private final Timeline timeline;


    // --- Constructor ---------------------------------------------------------

    public SynchronousXYItemsModel(Timeline timeline) {
        this.timeline = timeline;
    }

    public SynchronousXYItemsModel(Timeline timeline, SynchronousXYItem[] items) {
        this(timeline);

        if (items == null)
            throw new IllegalArgumentException("Items cannot be null"); // NOI18N
        if (items.length == 0)
            throw new IllegalArgumentException("Items cannot be empty"); // NOI18N

        addItems(items);
    }


    // --- Public interface ----------------------------------------------------

    public void addItems(SynchronousXYItem[] addedItems) {
        for (SynchronousXYItem addedItem : addedItems) {
            addedItem.setTimeline(timeline);
            items.add(addedItem);
        }

        fireItemsAdded(Arrays.asList(addedItems));

        if (timeline.getTimestampsCount() > 0) valuesAdded();
    }

    public void removeItems(SynchronousXYItem[] removedItems) {
        for (SynchronousXYItem item : removedItems) items.remove(item);
        fireItemsRemoved(Arrays.asList(removedItems));
    }


    public final void valuesAdded() {
        // Update values
        List<ChartItemChange> itemChanges = new ArrayList<>(items.size());
        for (SynchronousXYItem item : items) itemChanges.add(item.valuesChanged());
        fireItemsChanged(itemChanges);

        // Check timestamp
        int valueIndex = timeline.getTimestampsCount() - 1;
        long timestamp = timeline.getTimestamp(valueIndex);
        long previousTimestamp = valueIndex == 0 ? -1 :
                                 timeline.getTimestamp(valueIndex - 1);
        
        if (previousTimestamp != -1 && previousTimestamp >= timestamp)
// See #168544
//            throw new IllegalArgumentException(
//                           "ProfilerXYItemsModel: new timestamp " + timestamp + // NOI18N
//                           " not greater than previous " + previousTimestamp + // NOI18N
//                           ", skipping the values."); // NOI18N
            System.err.println("WARNING [" + SynchronousXYItemsModel.class.getName() + // NOI18N
                               "]: ProfilerXYItemsModel: new timestamp " + // NOI18N
                               timestamp + " not greater than previous " + // NOI18N
                               previousTimestamp + ", skipping the values."); // NOI18N
    }

    public final void valuesReset() {
        // Update values
        List<ChartItemChange> itemChanges = new ArrayList<>(items.size());
        for (SynchronousXYItem item : items) itemChanges.add(item.valuesChanged());
        fireItemsChanged(itemChanges);
    }


    public final Timeline getTimeline() {
        return timeline;
    }


    // --- AbstractItemsModel implementation -----------------------------------

    public final int getItemsCount() { return items.size(); }

    public final SynchronousXYItem getItem(int index) { return items.get(index); }

}

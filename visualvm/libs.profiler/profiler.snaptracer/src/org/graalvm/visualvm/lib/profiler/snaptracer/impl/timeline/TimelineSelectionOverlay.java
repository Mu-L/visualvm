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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.charts.ChartConfigurationListener;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartOverlay;
import org.graalvm.visualvm.lib.charts.ChartSelectionListener;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.XYItem;
import org.graalvm.visualvm.lib.charts.xy.XYItemSelection;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineSelectionOverlay extends ChartOverlay {

    private TimelineChart chart;
    private TimelineSupport support;

    private int selectionExtent;

    private ConfigurationListener configurationListener;
    private SelectionListener selectionListener;
    private TimeSelectionListener timeSelectionListener;
    private final Set<Point> highlightedValues;
    private final Set<Point> selectedValues;

    private Paint sMarkPaint;
    private Paint sOddPerfPaint;
    private Paint sEvenPerfPaint;

    private Paint hMarkPaint;
    private Paint hOddPerfPaint;
    private Paint hEvenPerfPaint;

    private Stroke markStroke;
    private Stroke oddPerfStroke;
    private Stroke evenPerfStroke;


    TimelineSelectionOverlay() {
        configurationListener = new ConfigurationListener();
        selectionListener = new SelectionListener();
        timeSelectionListener = new TimeSelectionListener();
        highlightedValues = new HashSet<>();
        selectedValues = new HashSet<>();
        initDefaultValues();
    }
    

    // --- Internal API --------------------------------------------------------

    final void registerChart(TimelineSupport support) {
        unregisterListener();
        this.support = support;
        this.chart = support.getChart();
        registerListener();
    }

    final void unregisterChart(TimelineSupport support) {
        unregisterListener();
        this.support = null;
        this.chart = null;
    }


    // --- Private implementation ----------------------------------------------

    private void registerListener() {
        if (support == null || chart == null) return;
        chart.addConfigurationListener(configurationListener);
        chart.addRowListener(configurationListener);
        chart.getSelectionModel().addSelectionListener(selectionListener);
        support.addSelectionListener(timeSelectionListener);
    }

    private void unregisterListener() {
        if (support == null || chart == null) return;
        chart.removeConfigurationListener(configurationListener);
        chart.removeRowListener(configurationListener);
        chart.getSelectionModel().removeSelectionListener(selectionListener);
        support.removeSelectionListener(timeSelectionListener);
    }

    private void initDefaultValues() {
        sMarkPaint = new Color(120, 120, 120);
        sOddPerfPaint = new Color(120, 120, 120);
        sEvenPerfPaint = Color.WHITE;

        hMarkPaint = new Color(80, 80, 80);
        hOddPerfPaint = Color.BLACK;
        hEvenPerfPaint = Color.WHITE;

        markStroke = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        oddPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 0);
        evenPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 2);

        selectionExtent = 3;
    }

    private final Set<Integer> paintedLines = new HashSet<>();

    public void paint(Graphics g) {
        if (highlightedValues.isEmpty() && selectedValues.isEmpty()) return;

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHints(chart.getRenderingHints());

        Iterator<Point> it = selectedValues.iterator();
        paintedLines.clear();
        
        int height = getHeight();
        int extentP = 1 - selectionExtent;
        int extentD = selectionExtent * 2 - 1;

        while (it.hasNext()) {
            Point p = it.next();
            int x = p.x;
            int y = p.y;

            if (y == -1) continue;

            if (!paintedLines.contains(x)) {
                g2.setPaint(sEvenPerfPaint);
                g2.setStroke(evenPerfStroke);
                g2.drawLine(x, 0, x, height);
                g2.setPaint(sOddPerfPaint);
                g2.setStroke(oddPerfStroke);
                g2.drawLine(x, 0, x, height);

                g2.setPaint(sMarkPaint);
                g2.setStroke(markStroke);

                paintedLines.add(x);
            }
            
            if (y - selectionExtent >= 0 && y + selectionExtent <= height)
                g2.fillOval(x + extentP, y + extentP, extentD, extentD);
        }

        it = highlightedValues.iterator();
        paintedLines.clear();

        while (it.hasNext()) {
            Point p = it.next();
            int x = p.x;
            int y = p.y;

            if (y == -1) continue;

            if (!paintedLines.contains(x)) {
                g2.setPaint(hEvenPerfPaint);
                g2.setStroke(evenPerfStroke);
                g2.drawLine(x, 0, x, height);
                g2.setPaint(hOddPerfPaint);
                g2.setStroke(oddPerfStroke);
                g2.drawLine(x, 0, x, height);

                g2.setPaint(hMarkPaint);
                g2.setStroke(markStroke);

                paintedLines.add(x);
            }
            
            if (y - selectionExtent >= 0 && y + selectionExtent <= height)
                g2.fillOval(x + extentP, y + extentP, extentD, extentD);
        }

    }

    private void vLineBoundsChanged(Set<Point> oldSelection, Set<Point> newSelection) {
        SortedSet<Integer> selectionBounds = new TreeSet<>();
        for (Point p : oldSelection) selectionBounds.add(p.x);
        int selections = selectionBounds.size();
        if (selections == 1) {
            repaint(selectionBounds.first() - selectionExtent,
                    0, selectionExtent * 2, getHeight());
            selectionBounds.clear();
        }

        for (Point p : newSelection) selectionBounds.add(p.x);
        selections = selectionBounds.size();
        if (selections == 1) {
            repaint(selectionBounds.first() - selectionExtent,
                    0, selectionExtent * 2, getHeight());
        } else if (selections > 1) {
            int firstX = selectionBounds.first() - selectionExtent;
            int lastX  = selectionBounds.last() + selectionExtent;
            repaint(firstX, 0, lastX - firstX, getHeight());
        }
    }

    private List<ItemSelection> getSelections() {
        List<ItemSelection> items = new ArrayList<>();

        Set<Integer> timestamps = support.getSelectedTimestamps();
        SynchronousXYItemsModel model = (SynchronousXYItemsModel)chart.getItemsModel();
        int itemsCount = model.getItemsCount();
        for (int itemIndex = 0; itemIndex < itemsCount; itemIndex++)
            for (int timestamp : timestamps)
                items.add(new XYItemSelection.Default(model.getItem(itemIndex),
                          timestamp, XYItemSelection.DISTANCE_UNKNOWN));

        return items;
    }

    private static void updateValues(Set<Point> values,
                                     List<ItemSelection> selectedItems,
                                     TimelineChart chart) {
        values.clear();
        for (ItemSelection sel : selectedItems) {
            XYItemSelection xySel = (XYItemSelection)sel;
            XYItem item = xySel.getItem();
            TimelineXYPainter painter = (TimelineXYPainter)chart.getPaintersModel().getPainter(item);
            ChartContext context = chart.getChartContext(item);
            long xValue = item.getXValue(xySel.getValueIndex());
            long yValue = item.getYValue(xySel.getValueIndex());
            int xPos = Utils.checkedInt(Math.ceil(context.getViewX(xValue)));
            int yPos = Utils.checkedInt(Math.ceil(painter.getItemView(yValue, item, context)));
            if (xPos >= 0 && xPos <= chart.getWidth()) values.add(new Point(xPos, yPos));
        }
    }


    private class ConfigurationListener extends ChartConfigurationListener.Adapter
                                        implements TimelineChart.RowListener {
        private final Runnable selectionUpdater = new Runnable() {
            public void run() {
                Set<Point> oldSelectedValues = new HashSet<>(selectedValues);
                updateValues(selectedValues, getSelections(), chart);
                vLineBoundsChanged(oldSelectedValues, selectedValues);
                
                Set<Point> oldValues = new HashSet<>(highlightedValues);
                updateValues(highlightedValues, chart.getSelectionModel().
                             getHighlightedItems(), chart);
                vLineBoundsChanged(oldValues, highlightedValues);
            }
        };
        public void contentsUpdated(long offsetX, long offsetY,
                                    double scaleX, double scaleY,
                                    long lastOffsetX, long lastOffsetY,
                                    double lastScaleX, double lastScaleY,
                                    int shiftX, int shiftY) {
            if (highlightedValues.isEmpty() && !support.isTimestampSelection(true)) return;
            if (lastOffsetX != offsetX || lastOffsetY != offsetY ||
                scaleX != lastScaleX || scaleY != lastScaleY)
                SwingUtilities.invokeLater(selectionUpdater);
        }
        public void rowsAdded(List<TimelineChart.Row> rows) { selectionUpdater.run(); }

        public void rowsRemoved(List<TimelineChart.Row> rows) { selectionUpdater.run(); }

        public void rowsResized(List<TimelineChart.Row> rows) { selectionUpdater.run(); }
    }

    private class SelectionListener implements ChartSelectionListener {

        public void selectionModeChanged(int newMode, int oldMode) {}

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {
        }

        public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
        }

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            Set<Point> oldHighlightedValues = new HashSet<>(highlightedValues);
            updateValues(highlightedValues, currentItems, chart);
            vLineBoundsChanged(oldHighlightedValues, highlightedValues);
        }

    }

    private class TimeSelectionListener implements TimelineSupport.SelectionListener {
        
        public void intervalsSelectionChanged() {}

        public void indexSelectionChanged() {}

        public void timeSelectionChanged(boolean timestampsSelected, boolean justHovering) {
            Set<Point> oldSelectedValues = new HashSet<>(selectedValues);
            updateValues(selectedValues, getSelections(), chart);
            vLineBoundsChanged(oldSelectedValues, selectedValues);
        }

    }

}

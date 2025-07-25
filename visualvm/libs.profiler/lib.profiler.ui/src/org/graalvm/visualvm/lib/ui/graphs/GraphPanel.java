/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.graphs;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import org.graalvm.visualvm.lib.charts.ChartSelectionListener;
import org.graalvm.visualvm.lib.charts.ChartSelectionModel;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.PaintersModel;
import org.graalvm.visualvm.lib.charts.axis.TimeAxisUtils;
import org.graalvm.visualvm.lib.charts.xy.XYItemSelection;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYChart;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYTooltipModel;


/**
 * @author Jiri Sedlacek
 */
public abstract class GraphPanel extends JPanel {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.graphs.Bundle"); // NOI18N
    private static final String NO_DATA_TOOLTIP = messages.getString("GraphPanel_NoDataTooltip"); // NOI18N
    private static final String MAX_VALUE_STRING = messages.getString("GraphPanel_MaxValueString"); // NOI18N
    // -----

    protected static final DateFormat DATE_FORMATTER;
    protected static final NumberFormat INT_FORMATTER;
    protected static final NumberFormat PERCENT_FORMATTER;

    protected static final double INITIAL_CHART_SCALEX = 0.02d;

    // 3 minutes to switch from Scale To Fit to Fixed Scale
    protected static final long SMALL_CHART_FIT_TO_WINDOW_PERIOD = 180000;


    static {
        String format = MessageFormat.format(TimeAxisUtils.TIME_DATE_FORMAT,
                TimeAxisUtils.TIME_SEC,
                TimeAxisUtils.DATE_YEAR);
        DATE_FORMATTER = new SimpleDateFormat(format);

        INT_FORMATTER = NumberFormat.getIntegerInstance();
        INT_FORMATTER.setGroupingUsed(true);

        PERCENT_FORMATTER = NumberFormat.getPercentInstance();
        PERCENT_FORMATTER.setMinimumFractionDigits(1);
        PERCENT_FORMATTER.setMaximumIntegerDigits(2);
    }


    // --- Messages support ----------------------------------------------------

    protected String getMaxValueString(String origString) {
        return MessageFormat.format(MAX_VALUE_STRING, origString);
    }


    // --- Chart support -------------------------------------------------------

    protected ProfilerXYChart createChart(SynchronousXYItemsModel itemsModel,
                                          PaintersModel paintersModel,
                                          final boolean smallPanel) {

        ProfilerXYChart chart;

        if (smallPanel) {
            chart = new ProfilerXYChart(itemsModel, paintersModel) {
                public JToolTip createToolTip() {
                    lastTooltip = new SmallTooltip(this);
                    return lastTooltip;
                }
                public Point getToolTipLocation(MouseEvent e) {
                    return getSmallTooltipLocation(e, smallTooltipManager);
                }
            };
            smallTooltipManager = new SmallTooltipManager(chart);
            chart.setToolTipText(NO_DATA_TOOLTIP); // Needed to enable the tooltip
            ToolTipManager.sharedInstance().registerComponent(chart);
        } else {
            chart = new ProfilerXYChart(itemsModel, paintersModel);
        }
        
        chart.addPreDecorator(new XYBackground());
        chart.setFitsWidth(false);

        chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_EACH_NEAREST);
        return chart;

    }

    public abstract Action[] getActions();
    
    public abstract void cleanup();


    // --- General tooltip support ---------------------------------------------

    private ProfilerXYTooltipModel tooltipModel;

    protected ProfilerXYTooltipModel getTooltipModel() {
        if (tooltipModel == null) tooltipModel = createTooltipModel();
        return tooltipModel;
    }

    protected abstract ProfilerXYTooltipModel createTooltipModel();


    // --- Small tooltip support -----------------------------------------------

    private SmallTooltip lastTooltip;
    private SmallTooltipManager smallTooltipManager;


    private static Point getSmallTooltipLocation(MouseEvent e, SmallTooltipManager tooltip) {
        Point p = e.getPoint();
        tooltip.setMousePosition(p);
        p.y += 25;
        return p;
    }

    private class SmallTooltip extends JToolTip {
        SmallTooltip(ProfilerXYChart chart) {
            super();
            setComponent(chart);

            addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        if (isShowing()) smallTooltipManager.enableSelection();
                        else smallTooltipManager.disableSelection();
                    }
                }
            });
        }
    }

    private class SmallTooltipManager implements ChartSelectionListener {

        private boolean timerRunning = false;
        private int mouseX;
        private int mouseY;
        private ProfilerXYChart chart;

        SmallTooltipManager(ProfilerXYChart chart) {
            this.chart = chart;
            chart.getSelectionModel().addSelectionListener(this);
        }

        public void setMousePosition(Point p) {
            this.mouseX = p.x;
            this.mouseY = p.y;
        }

        private void enableSelection() {
            chart.getSelectionModel().setHoverMode(ChartSelectionModel.
                                                   HOVER_EACH_NEAREST);
        }

        private void disableSelection() {
            // Another disableSelection() in progress?
            if (timerRunning) return;
            timerRunning = true;

            // Tooltip is hidden when its location changes, let's wait for a while
            Timer timer = new Timer(50, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!isTooltipShowing()) {
                        chart.getSelectionModel().
                                   setHoverMode(ChartSelectionModel.HOVER_NONE);
                        chart.setToolTipText(NO_DATA_TOOLTIP);
                    }
                    timerRunning = false;
                }
            });
            timer.setRepeats(false);
            timer.start();
        }

        private boolean isTooltipShowing() {
            return lastTooltip != null && lastTooltip.isShowing();
        }

        private void updateTooltipText(List<ItemSelection> selectedItems) {
            if (!isTooltipShowing()) return;

            if (selectedItems.isEmpty()) {
                chart.setToolTipText(NO_DATA_TOOLTIP);
            } else {
                chart.setToolTipText(getTooltipText(selectedItems));
            }

            // A MouseEvent needs to be passed to the ToolTipManager to
            // immediately update the displayed tooltip
            MouseEvent e = new MouseEvent(chart, 0, 0, 0, mouseX, mouseY, 0, false);
            ToolTipManager.sharedInstance().mouseMoved(e);
        }

        private String getTooltipText(List<ItemSelection> selectedItems) {
            ProfilerXYTooltipModel model = getTooltipModel();

            int rowsCount = model.getRowsCount();
            if (selectedItems.size() != rowsCount)
                 return "Rows and selected items don't match"; // NOI18N

            StringBuilder builder = new StringBuilder();
            builder.append("<html>"); // NOI18N


            XYItemSelection selection = (XYItemSelection)selectedItems.get(0);
            long timestamp = selection.getItem().getXValue(selection.getValueIndex());
            builder.append("&nbsp;&nbsp;"); // NOI18N
            builder.append(model.getTimeValue(timestamp));
            builder.append("&nbsp;&nbsp;"); // NOI18N
            builder.append("<hr>"); // NOI18N

            for (int i = 0; i < rowsCount; i++) {
                builder.append("<b>&nbsp;&nbsp;"); // NOI18N
                builder.append(model.getRowName(i));
                builder.append(":</b>&nbsp;&nbsp;"); // NOI18N
                XYItemSelection sel = (XYItemSelection)selectedItems.get(i);
                long itemValue = sel.getItem().getYValue(sel.getValueIndex());
                builder.append(model.getRowValue(i, itemValue));
                builder.append("&nbsp;"); // NOI18N
                builder.append(model.getRowUnits(i));
                builder.append("&nbsp;&nbsp;<br>"); // NOI18N
            }

            int extraRowsCount = model.getExtraRowsCount();
            if (extraRowsCount > 0) builder.append("<hr>"); // NOI18N
            for (int i = 0; i < extraRowsCount; i++) {
                builder.append("<b>&nbsp;&nbsp;"); // NOI18N
                builder.append(model.getExtraRowName(i));
                builder.append(":</b>&nbsp;&nbsp;"); // NOI18N
                builder.append(model.getExtraRowValue(i));
                builder.append("&nbsp;"); // NOI18N
                builder.append(model.getExtraRowUnits(i));
                builder.append("&nbsp;&nbsp;<br>"); // NOI18N
            }

            builder.append("</html>"); // NOI18N
            return builder.toString();
        }


        public void selectionModeChanged(int newMode, int oldMode) {}

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {}

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
                                            List<ItemSelection> addedItems,
                                            List<ItemSelection> removedItems) {

            updateTooltipText(currentItems);
        }

        public void selectedItemsChanged(List<ItemSelection> currentItems,
                                         List<ItemSelection> addedItems,
                                         List<ItemSelection> removedItems) {}

    }
    
}

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

package org.graalvm.visualvm.lib.ui.swing.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * Base class for custom Table/Tree/List renderers optimized for performance.
 * To create a label-based renderer, use LabelRenderer instead.
 *
 * @author Jiri Sedlacek
 */
public class BaseRenderer extends JComponent implements ProfilerRenderer {

    // --- Constructor ---------------------------------------------------------

    public BaseRenderer() {
        setOpaque(false);
    }

    // --- Renderer ------------------------------------------------------------

    private int alignment;

    public void setValue(Object value, int row) {}

    public void setHorizontalAlignment(int alignment) {
        this.alignment = alignment;
    }

    public int getHorizontalAlignment() {
        return alignment;
    }

    public JComponent getComponent() {
        return this;
    }

    // --- Tools ---------------------------------------------------------------

    private Point sharedPoint;
    private Dimension sharedDimension;
    private Rectangle sharedRectangle;

    protected final Point sharedPoint(int x, int y) {
        if (sharedPoint == null) sharedPoint = new Point();
        sharedPoint.x = x;
        sharedPoint.y = y;
        return sharedPoint;
    }

    protected final Point sharedPoint(Point point) {
        return sharedPoint(point.x, point.y);
    }

    protected final Dimension sharedDimension(int width, int height) {
        if (sharedDimension == null) sharedDimension = new Dimension();
        sharedDimension.width = width;
        sharedDimension.height = height;
        return sharedDimension;
    }
    
    protected final Dimension sharedDimension(Dimension dimension) {
        return sharedDimension(dimension.width, dimension.height);
    }
    
    protected final Rectangle sharedRectangle(int x, int y, int width, int height) {
        if (sharedRectangle == null) sharedRectangle = new Rectangle();
        sharedRectangle.x = x;
        sharedRectangle.y = y;
        sharedRectangle.width = width;
        sharedRectangle.height = height;
        return sharedRectangle;
    }
    
    protected final Rectangle sharedRectangle(Rectangle rectangle) {
        return sharedRectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }
    
    // --- Geometry ------------------------------------------------------------
    
    protected final Point location = new Point();
    protected final Dimension size = new Dimension();
    
    public void move(int x, int y) {
        location.x = x;
        location.y = y;
    }
    
    public Point getLocation() {
        return sharedPoint(location);
    }
    
    public int getX() {
        return location.x;
    }
    
    public int getY() {
        return location.y;
    }
    
    public void setSize(int w, int h) {
        size.width = w;
        size.height = h;
    }
    
    public Dimension getSize() {
        return sharedDimension(size);
    }
    
    public int getWidth() {
        return size.width;
    }
    
    public int getHeight() {
        return size.height;
    }
    
    public Rectangle getBounds() {
        return sharedRectangle(location.x, location.y, size.width, size.height);
    }
    
    public void reshape(int x, int y, int w, int h) {
        // ignore x, y: used only for move(x, y)
//        location.x = x;
//        location.y = y;
        size.width = w;
        size.height = h;
    }
    
    // --- Margins / Borders ---------------------------------------------------
    
    private final Insets insets = new Insets(0, 0, 0, 0);
    
    public Insets getInsets() {
        return insets;
    }

    public Insets getInsets(Insets insets) {
        return this.insets;
    }
    
    // --- Appearance ----------------------------------------------------------
    
    private boolean visible = true;
    private boolean enabled = true;
    private Color foreground;
    private Color background;
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    public Color getForeground() {
        return foreground;
    }
    
    public void setBackground(Color background) {
        this.background = background;
    }

    public Color getBackground() {
        return background;
    }
    
    // --- Painting / Layout ---------------------------------------------------
    
    public void paint(Graphics g) {
        if (isOpaque()) {
            g.setColor(background);
            g.fillRect(location.x, location.y, size.width, size.height);
        }
    }
    
    public void validate() {}

    public void revalidate() {}

    public void repaint(long tm, int x, int y, int width, int height) {}

    public void repaint(Rectangle r) {}

    public void repaint() {}
    
    // --- Events --------------------------------------------------------------

    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}

    public void firePropertyChange(String propertyName, char oldValue, char newValue) {}

    public void firePropertyChange(String propertyName, short oldValue, short newValue) {}

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {}

    public void firePropertyChange(String propertyName, long oldValue, long newValue) {}

    public void firePropertyChange(String propertyName, float oldValue, float newValue) {}

    public void firePropertyChange(String propertyName, double oldValue, double newValue) {}

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
    
    // --- Accessibility -------------------------------------------------------
    
    private AccessibleContext accessibleContext;
    
    protected AccessibleContext createAccessibleContext() {
        return new JComponent.AccessibleJComponent() {
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.LABEL;
            }
            public String getAccessibleName() {
                return BaseRenderer.this.toString();
            }
        };
    }
    
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = createAccessibleContext();
        return accessibleContext;
    }
    
}

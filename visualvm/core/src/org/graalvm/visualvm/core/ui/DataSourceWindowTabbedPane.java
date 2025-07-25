/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.uisupport.UISupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.graalvm.visualvm.uisupport.ProfilerTabbedPane;
import org.openide.util.NbBundle;

/**
 * TabbedPane container allowing to control if tabs can be closed or not
 *
 * @author Jiri Sedlacek
 *
 */
abstract class DataSourceWindowTabbedPane extends JPanel {

  private final ProfilerTabbedPane tabpane;
  
  // --- Workaround to use the correct Close button on Windows 10 --------------
  
//    static {
//        if (isWindows10() && isWindowsXPLaF()) {
//            UIManager.put( "nb.close.tab.icon.enabled.name", "org/openide/awt/resources/win8_bigclose_enabled.png"); // NOI18N
//            UIManager.put( "nb.close.tab.icon.pressed.name", "org/openide/awt/resources/win8_bigclose_pressed.png"); // NOI18N
//            UIManager.put( "nb.close.tab.icon.rollover.name", "org/openide/awt/resources/win8_bigclose_rollover.png"); // NOI18N
//        }
//    }

//    private static boolean isWindows10() {
//        String osName = System.getProperty ("os.name"); // NOI18N
//        return osName.indexOf("Windows 10") >= 0 // NOI18N
//            || (osName.equals( "Windows NT (unknown)" ) && "10.0".equals( System.getProperty("os.version") )); // NOI18N
//    }

//    private static boolean isWindowsXPLaF() {
//        Boolean isXP = (Boolean)Toolkit.getDefaultToolkit().
//                        getDesktopProperty("win.xpstyle.themeActive"); // NOI18N
//        return isWindowsLaF() && (isXP == null ? false : isXP.booleanValue());
//    }
    
//    private static boolean isWindowsLaF () {
//        String lfID = UIManager.getLookAndFeel().getID();
//        return lfID.endsWith("Windows"); // NOI18N
//    }
    
  // ---------------------------------------------------------------------------
  
  
  DataSourceWindowTabbedPane() {
    super(new BorderLayout());
    
    tabpane = new ProfilerTabbedPane() {
        @Override
        protected void closeTab(Component component) {
            closeView((ViewContainer)component);
        }
    };
//    tabpane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
    
    // GH-52 - true would break Tab navigation
    tabpane.setFocusCycleRoot(false);
      
    // Clear default border for fill up the entire DataSourceWindow
    tabpane.setOpaque(false);
    
    if (UIManager.getLookAndFeel().getID().equals("Aqua")) { // NOI18N
        tabpane.setBorder(BorderFactory.createEmptyBorder(0, -11, -13, -10));
    } else {
        tabpane.setBorder(BorderFactory.createEmptyBorder());
        Insets i = UIManager.getInsets("TabbedPane.contentBorderInsets"); // NOI18N
        if (i != null) tabpane.setBorder(BorderFactory.createEmptyBorder(0, -i.left, -i.bottom, -i.right));
    }
    
    add(tabpane, BorderLayout.CENTER);
  }


  public final boolean requestFocusInWindow() {
      Component sel = tabpane.getSelectedComponent();
      if (sel != null) return sel.requestFocusInWindow();
      else return super.requestFocusInWindow();
  }
  
  public void addView(DataSource dataSource, DataSourceView view) {
      ViewContainer container = new ViewContainer(new DataSourceCaption(view.getDataSource()), view);
      tabpane.addTab(view.getName(), new ImageIcon(view.getImage()), container, null, view.isClosable());
  }
  
  public void insertView(DataSource dataSource, DataSourceView view, int index) {
      ViewContainer container = new ViewContainer(new DataSourceCaption(view.getDataSource()), view);
      tabpane.insertTab(view.getName(), new ImageIcon(view.getImage()), container, null, view.isClosable(), index);
  }
  
  public void removeView(int index) {
      ViewContainer container = (ViewContainer)tabpane.getComponentAt(index);
      tabpane.removeTabAt(index);
      container.getCaption().finish();
  }
  
  void clearView(int index) {
      ViewContainer container = (ViewContainer)tabpane.getComponentAt(index);
      container.removeAll();
      if (container.caption != null) container.caption.finish();
      container.setReloading();
      container.doLayout();
      container.repaint();
  }
  
  ViewContainer getContainer(DataSourceView view) {
      String name = view.getName();
//      int position = view.getPreferredPosition();

      for (int i = 0; i < tabpane.getTabCount(); i++)
        if (tabpane.getTitleAt(i).equals(name))
            return (ViewContainer)tabpane.getComponentAt(i);
      
      return null;
  }
  
  public DataSourceView getView(ViewContainer container) {
      return container.getView();
  }
  
  public int indexOfView(final DataSourceView view) {
      if (!view.hasComponent()) return -1;
      final int[] index = new int[1];
      index[0] = -1;
      UISupport.runInEventDispatchThreadAndWait(new Runnable() {
          public void run() {
              for (int i = 0; i < tabpane.getTabCount(); i++)
                  if (((ViewContainer)tabpane.getComponentAt(i)).getViewComponent() == view.getView()) index[0] = i;
          }
      });
      return index[0];
  }
  
  public List<DataSourceView> getViews() {
      List<DataSourceView> views = new ArrayList<>();
      
      for (int i = 0; i < tabpane.getTabCount(); i++) {
          ViewContainer container = (ViewContainer)tabpane.getComponentAt(i);
          views.add(container.getView());
      }
      
      return views;
  }
  
  public void setViewIndex(int index) {
    tabpane.setSelectedIndex(index);
  }
  
  public void setViewBackground(int index, Color background) {
      tabpane.setBackgroundAt(index, background);
  }
  
  
  protected abstract void closeView(DataSourceWindowTabbedPane.ViewContainer view);
  
  
  static class ViewContainer extends JPanel {
      
      private DataSourceCaption caption;
      private DataSourceView view;
      private DataViewComponent viewComponent;
      
      ViewContainer(DataSourceCaption caption, DataSourceView view) {
          Color backgroundColor = UISupport.getDefaultBackground();
          
          setLayout(new BorderLayout());
          setBorder(BorderFactory.createMatteBorder(0, 5, 5, 5, backgroundColor));
          setBackground(backgroundColor);
          setFocusable(false);

          setView(view);
          setCaption(caption);
      }

      public final boolean requestFocusInWindow() {
        if (getComponentCount() > 0) return getComponent(0).requestFocusInWindow();
        else return super.requestFocusInWindow();
      }
      
      final void setCaption(DataSourceCaption caption) {
          this.caption = caption;
          if (caption != null) {
              caption.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
              add(caption, BorderLayout.NORTH);
          }
      }
      
      public DataSourceCaption getCaption() { return caption; }
      
      final void setView(DataSourceView view) {
          this.view = view;
          this.viewComponent = view.getView();
          add(viewComponent, BorderLayout.CENTER);
          setName(view.getName());
      }
      
      public DataSourceView getView() { return view; }
      
      public DataViewComponent getViewComponent() { return viewComponent; }
      
      final void setReloading() {
        JLabel l = new JLabel(NbBundle.getMessage(DataSourceWindowTabbedPane.class, "DataSourceCaption_MSG_Reloading"), JLabel.CENTER); // NOI18N
        l.setEnabled(false);        
        add(l, BorderLayout.CENTER);
      }
  }
  
}

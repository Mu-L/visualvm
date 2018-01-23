/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.truffle;

import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.MultiRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import com.sun.tools.visualvm.heapviewer.java.StackFrameNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class TruffleStackFrameNode extends StackFrameNode {
    
    public TruffleStackFrameNode(String name, HeapViewerNode[] children) {
        super(name, children);
    }
    
    
    // NOTE: temporary solution, should probably be implemented for each Truffle language separately
    static class Renderer extends MultiRenderer implements HeapViewerRenderer {
    
        private final LabelRenderer atRenderer;
        private final NormalBoldGrayRenderer frameRenderer;
        private final ProfilerRenderer[] renderers;
        
        private String name1;
        private String name2;
        private String detail;


        public Renderer() {
            atRenderer = new LabelRenderer();
            atRenderer.setText("at");
            atRenderer.setMargin(3, 3, 3, 0);
            frameRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    if (value == null) {
                        setNormalValue("");
                        setBoldValue("");
                        setGrayValue("");
                    } else {
                        setNormalValue(((Object[])value)[0].toString());
                        setBoldValue(((Object[])value)[1].toString());
                        setGrayValue(((Object[])value)[2].toString());
                    }
                }
            };
            renderers = new ProfilerRenderer[] { atRenderer, frameRenderer };
        }


        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }


        public void setValue(Object value, int row) {
            if (value == null) {
                // no value - fallback to <unknown>
                name1 = "";
                name2 = "<unknown>";
                detail = "";
            } else {
                String val = value.toString();
                
                int idx = val.lastIndexOf(' ');
                if (idx != -1) { // multiple strings
                    detail = val.substring(idx);
                    if (detail.startsWith(" (")) {
                        val = val.substring(0, idx); // detail contains source:line
                    } else {
                        detail = ""; // no detail available
                    }
                    
                    idx = val.startsWith("<") ? -1 : val.lastIndexOf(' ');
                    if (idx != -1) { // multiple strings - last bold
                        name2 = val.substring(idx + 1);
                        name1 = val.substring(0, idx + 1);
                    } else { // single string or meta value - all bold
                        name1 = "";
                        name2 = val;
                    }
                    
                    idx = name2.lastIndexOf('.');
                    if (idx != -1) { // class.method detected in last string - only method bold
                        if (!name1.isEmpty()) name1 += " ";
                        name1 = name1 + name2.substring(0, idx + 1);
                        name2 = name2.substring(idx + 1);
                    }
                } else { // single string - all bold
                    name1 = "";
                    name2 = val;
                    detail = "";
                }
            }
            
            frameRenderer.setValue(new Object[] { name1, name2, detail }, row);
        }

        public String getShortName() {
            return "at " + name2 + " " + detail;
        }

    }
    
}

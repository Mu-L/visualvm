/*
 * Copyright (c) 1997, 2009, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Data.java
 *
 * Created on June 14, 2005, 12:37 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package simple;


/**
 *
 * @author ehucka
 */
public class Data {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private boolean available = false;
    private int contents;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized int get(int who) {
        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        available = false;
        notifyAll();

        return contents;
    }

    public synchronized void put(int who, int value) {
        while (available == true) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        contents = value;
        available = true;
        notifyAll();
    }
}

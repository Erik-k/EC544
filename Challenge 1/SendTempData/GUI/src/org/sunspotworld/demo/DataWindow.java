/*
 * DataWindow.java
 *
 * Copyright (c) 2008 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.sunspotworld.demo;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;

import java.text.DateFormat;
import java.util.Date;

/**
 * Create a new window to graph the sensor readings in.
 *
 * @author Ron Goldman
 */
public class DataWindow extends JFrame {
//I changed the MAX_SAMPLES to 180, which is 3 hours in this case, but maybe too small
    private static final int MAX_SAMPLES = 180;
    private int index = 0;
    private long[] time = new long[MAX_SAMPLES];
    private int[] val = new int[MAX_SAMPLES];
    DateFormat fmt = DateFormat.getDateTimeInstance();

    /** Creates new form DataWindow */
    public DataWindow() {
        initComponents();
    }

    public DataWindow(String ieee) {
        initComponents();
        setTitle(ieee);
    }
    
    public void addData(long t, int v) {
        time[index] = t;
        val[index++] = v;
        dataTextArea.append(fmt.format(new Date(t)) + "    value = " + v + "\n");
        dataTextArea.setCaretPosition(dataTextArea.getText().length());
        repaint();
    }
    
    // Graph the sensor values in the dataPanel JPanel
    public void paint(Graphics g) {
        super.paint(g);
        int left = dataPanel.getX() + 10;       // get size of pane
        int top = dataPanel.getY() + 30;
        int right = left + dataPanel.getWidth() - 20;
        int bottom = top + dataPanel.getHeight() - 20;
        
        int y0 = bottom - 20;                   // leave some room for margins
        int yn = top;
        int x0 = left + 33;
        int xn = right;
        double vscale = (yn - y0) / 140.0;      // Fahrenheit temp range from 0 to 140
        double tscale = 1.0 / 60*1000.0;           // 1 pixel = 1 min
        
        // draw X axis = time
        g.setColor(Color.BLACK);
        g.drawLine(x0, yn, x0, y0);
        g.drawLine(x0, y0, xn, y0);
        int tickInt = 60 / 2;
        for (int xt = x0 + tickInt; xt < xn; xt += tickInt) {   // tick every 1 minute
            g.drawLine(xt, y0 + 5, xt, y0 - 5);
            int min = (xt - x0) / (60 / 2);
            g.drawString(Integer.toString(min), xt - (min < 10 ? 3 : 7) , y0 + 20);
        }
        
        // draw Y axis = sensor reading
        g.setColor(Color.BLUE);
        for (int vt = 140; vt > 0; vt -= 20) {         // tick every 20
            int v = y0 + (int)(vt * vscale);
            g.drawLine(x0 - 5, v, x0 + 5, v);
            g.drawString(Integer.toString(vt), x0 - 38 , v + 5);
        }

        // graph sensor values
        int xp = -1;
        int vp = -1;
        for (int i = 0; i < index; i++) {
            int x = x0 + (int)((time[i] - time[0]) * tscale);
            int v = y0 + (int)(val[i] * vscale);
            if (xp > 0) {
                g.drawLine(xp, vp, x, v);
            }
            xp = x;
            vp = v;
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dataPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        dataTextArea = new javax.swing.JTextArea();

        dataPanel.setBackground(new java.awt.Color(255, 255, 255));
        dataPanel.setMinimumSize(new java.awt.Dimension(400, 250));
        dataPanel.setPreferredSize(new java.awt.Dimension(400, 250));
        getContentPane().add(dataPanel, java.awt.BorderLayout.CENTER);

        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setMinimumSize(new java.awt.Dimension(400, 100));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(400, 100));

        dataTextArea.setColumns(20);
        dataTextArea.setEditable(false);
        dataTextArea.setRows(4);
        jScrollPane1.setViewportView(dataTextArea);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel dataPanel;
    private javax.swing.JTextArea dataTextArea;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

}

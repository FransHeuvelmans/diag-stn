/*
 * Copyright 2016 Frans van den Heuvel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package diag.stn.GUI;

import diag.stn.GraphGenSettings;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import javax.swing.*;

import javax.swing.JPanel;

/**
 * GUI Settings panel for generating test cases with a Planlike graph
 * @author Frans van den Heuvel
 */
public class PlanSettingsPanel extends JPanel
{
    private GUIApp caller;
    
    public PlanSettingsPanel(GUIApp father)
    {
        caller = father;
        
        // lines
        JLabel linesLabel = new JLabel("# Lines:");
        add(linesLabel);
        JFormattedTextField linesField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        linesField.setColumns(3);
        linesField.setValue(new Integer(3));
        add(linesField);
        linesLabel.setLabelFor(linesField);
        
        // line lb
        JLabel linelbLabel = new JLabel("Line lb:");
        add(linelbLabel);
        JFormattedTextField linelbField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        linelbField.setColumns(3);
        linelbField.setValue(new Integer(8));
        add(linelbField);
        linelbLabel.setLabelFor(linelbField);
        
        // line ub
        JLabel lineubLabel = new JLabel("Line ub:");
        add(lineubLabel);
        JFormattedTextField lineubField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        lineubField.setColumns(3);
        lineubField.setValue(new Integer(12));
        add(lineubField);
        lineubLabel.setLabelFor(lineubField);
        
        // max line con
        JLabel lineconLabel = new JLabel("Line con:");
        add(lineconLabel);
        JFormattedTextField lineconField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        lineconField.setColumns(2);
        lineconField.setValue(new Integer(2));
        add(lineconField);
        lineconLabel.setLabelFor(lineconField);
        
        // max vert con
        JLabel vertconLabel = new JLabel("Vert con:");
        add(vertconLabel);
        JFormattedTextField vertconField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        vertconField.setColumns(2);
        vertconField.setValue(new Integer(2));
        add(vertconField);
        vertconLabel.setLabelFor(vertconField);
        
        //---
        
        // num obs
        JLabel obsLabel = new JLabel("# Obs:");
        add(obsLabel);
        JFormattedTextField obsField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        obsField.setColumns(2);
        obsField.setValue(new Integer(2));
        add(obsField);
        obsLabel.setLabelFor(obsField);
        
        // observation length
        JLabel obslenLabel = new JLabel("Obs. length:");
        add(obslenLabel);
        JFormattedTextField obslenField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        obslenField.setColumns(2);
        obslenField.setValue(new Integer(6));
        add(obslenField);
        obslenLabel.setLabelFor(obslenField);
        
        JLabel diffLabel = new JLabel("diff %:");
        add(diffLabel);
        JFormattedTextField diffField = new JFormattedTextField(
                NumberFormat.getPercentInstance());
        diffField.setColumns(4);
        diffField.setValue(new Double(0.2)); // !!! Needs to be translated!
        add(diffField);
        diffLabel.setLabelFor(diffField);
        
        // Add T0
        JCheckBox zeroPointBox = new JCheckBox("Add T0");
        add(zeroPointBox);
        
        JButton runPlanButton = new JButton("run Plan-Gen");
        runPlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                GraphGenSettings gs = new GraphGenSettings();
                int lines = ((Number)linesField.getValue()).intValue();
                int linelb = ((Number)linelbField.getValue()).intValue();
                int lineub = ((Number)lineubField.getValue()).intValue();
                int linecon = ((Number)lineconField.getValue()).intValue();
                int vertcon = ((Number)vertconField.getValue()).intValue();
                
                int observations = ((Number)obsField.getValue()).intValue();
                int obsLength = ((Number)obslenField.getValue()).intValue();
                double diffP = ((Number)diffField.getValue()).doubleValue();
                int diff = (int) (diffP * 100.0);
                boolean zeroPoint = zeroPointBox.isSelected();
                gs.planlikeGraph(lines,linelb,lineub,linecon, vertcon,
                        observations,obsLength,diff,zeroPoint);
                caller.runSettings(gs);
            }
        });
        
        add(runPlanButton);
        
        setBorder(BorderFactory.createLineBorder(Color.black));
    }
}

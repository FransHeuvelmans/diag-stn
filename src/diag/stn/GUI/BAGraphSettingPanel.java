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

/**
 * GUI Settings panel for generating BA style test cases
 * @author Frans van den Heuvel
 */
public class BAGraphSettingPanel extends JPanel
{
    private GUIApp caller;
    
    public BAGraphSettingPanel(GUIApp father)
    {
        caller = father;
        
        // size
        JLabel sizeLabel = new JLabel("Size graph:");
        add(sizeLabel);
        JFormattedTextField sizeField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        sizeField.setColumns(4);
        sizeField.setValue(new Integer(20));
        add(sizeField);
        sizeLabel.setLabelFor(sizeField);
        
        // links
        JLabel linksLabel = new JLabel("Links/vert:");
        add(linksLabel);
        JFormattedTextField linksField = new JFormattedTextField(
                NumberFormat.getIntegerInstance());
        linksField.setColumns(2);
        linksField.setValue(new Integer(2));
        add(linksField);
        linksLabel.setLabelFor(linksField);
        
        // only max
        JCheckBox onlyMaxBox = new JCheckBox("Only max con");
        add(onlyMaxBox);
        
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
        
        JButton runBAButton = new JButton("run BA-Gen");
        runBAButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                GraphGenSettings gs = new GraphGenSettings();
                int size = ((Number)sizeField.getValue()).intValue();
                int linksPerStep = ((Number)linksField.getValue()).intValue();
                boolean onlymax = onlyMaxBox.isSelected();
                int observations = ((Number)obsField.getValue()).intValue();
                int obsLength = ((Number)obslenField.getValue()).intValue();
                double diffP = ((Number)diffField.getValue()).doubleValue();
                int diff = (int) (diffP * 100.0);
                boolean zeroPoint = zeroPointBox.isSelected();
                gs.BAGraph(size,linksPerStep,onlymax,observations,obsLength,
                        diff,zeroPoint);
                caller.runSettings(gs);
            }
        });
        
        add(runBAButton);
        
        setBorder(BorderFactory.createLineBorder(Color.black));
    }
}

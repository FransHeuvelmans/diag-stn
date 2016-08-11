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

import diag.stn.*;
import diag.stn.analyze.*;
import diag.stn.STN.*;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.*;

/**
 * Create a simple GUI for the application
 *
 * @author Frans van den Heuvel
 */
public class GUIApp
{

    private JTextArea updateArea;

    /**
     * Create the GUI
     */
    public GUIApp()
    {
        // on a separate thread
        Runnable r = () -> createAndShowGUI();
        SwingUtilities.invokeLater(r);
    }

    private void createAndShowGUI()
    {
        JFrame frame = new JFrame("Diag-STN");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextArea consoleOut = new JTextArea(10, 70);
        JScrollPane consoleScroll = new JScrollPane(consoleOut,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        updateArea = consoleOut;
        frame.add(consoleScroll, BorderLayout.CENTER);
        redirectSystemStreams();    // System.out in TextArea

        JPanel controls = new JPanel(new BorderLayout());
        controls.add(new BAGraphSettingsPanel(this), BorderLayout.NORTH);
        controls.add(new PlanSettingsPanel(this), BorderLayout.CENTER);
        // frameLike Graph middle
        // extra options like input yaml and mayb visualization some day south

        frame.add(controls, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
        
        showTestCase();
    }

    private void updateTextArea(final String text)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                updateArea.append(text);
                updateArea.repaint();
            }
        });
    }

    private void redirectSystemStreams()
    {
        OutputStream out = new OutputStream()
        {
            @Override
            public void write(int b) throws IOException
            {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException
            {
                updateTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException
            {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    /**
     * Let some subpanel run a diagnosis test with some given settings
     *
     * @param gs Graph Settings to test
     */
    public void runSettings(GraphGenSettings gs)
    {
        updateArea.setText(null);
        System.out.println("Running with settings: "+ gs + "\n");
        GraphGenerator gen = new GraphGenerator();
        GraphGenerator.GraphObs strct = null;
        if(gs.type == GraphGenSettings.BAGRAPH)
        {
            strct = gen.generateBAGraph(gs);
            while(!strct.success)
                strct = gen.generateBAGraph(gs);
            
        }
        else if(gs.type == GraphGenSettings.PLANLIKEGRAPH)
        {
            strct = gen.generatePlanlikeGraph(gs);
            while(!strct.success)
                strct = gen.generatePlanlikeGraph(gs);
        }
        else
        {
            System.err.println("Wrong settings type!");
            return;
        }
        
        Analyst al = new Analyst(strct.graph);
        for(Observation ob : strct.observations)
        {
            al.addObservation(ob);
        }
        
        al.generatePaths();
        al.printPaths();
        al.propagateWeights();
        al.printWeights();
        Diagnosis[] diag = al.generateDiagnosis();
        al.printDiagnosis();
        CorrectCheck.printErrorsIntroduced(strct);
    }
    
    private void showTestCase()
    {
        System.out.println(" This is a test case showing MAC diagnosis of STN\n"
                + "(from Maximum Confirmation Diagnosis by Roos, 2010)\n");
        int ids = 0;
        
        // Explain the STN
        Graph graph = new Graph();
        Vertex a = new Vertex(ids++, "0");
        Vertex b = new Vertex(ids++, "1");
        Vertex c = new Vertex(ids++, "2");
        Vertex d = new Vertex(ids++, "3");
        Vertex e = new Vertex(ids++, "4");
        Vertex f = new Vertex(ids++, "5");
        Vertex g = new Vertex(ids++, "6");
        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);
        graph.addVertex(e);
        graph.addVertex(f);
        graph.addVertex(g);
        graph.addEdge(a, b, 10, 15);
        graph.addEdge(b, c, 14, 23);
        graph.addEdge(c, d, 6, 12);
        graph.addEdge(c, e, 13, 999);
        graph.addEdge(d, f, 25, 33);
        graph.addEdge(e, g, 10, 15);
        // t0 to t5 == [ 55 - 83]
        // t0 to t6 == [ 47 - 1052]
        
        Observation ob1 = new Observation(a,f,(55+30),(83+17));
        Observation ob2 = new Observation(a,g,(47+19),(47+33));
        
        // analysis part
        Analyst analyst = new Analyst(graph);
        analyst.addObservation(ob1);
        analyst.addObservation(ob2);
        
        analyst.generatePaths();
        
        analyst.printPaths();
        
        analyst.propagateWeights();
        
        analyst.printWeights(ob1);
        
        analyst.printWeights(ob2);
        
        analyst.generateDiagnosis();
        
        analyst.printDiagnosis();
    }
}

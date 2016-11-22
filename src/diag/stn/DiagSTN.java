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
package diag.stn;

import diag.stn.GUI.GUIApp;
import diag.stn.GraphGenerator.GraphObs;
import diag.stn.analyze.*;
import diag.stn.STN.*;

// IO imports
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.*;

/**
 * Entry point for program. Handles basic input/output, commandline paremeters
 * and starts the different parts of the program.
 * @author Frans van den Heuvel
 */
public class DiagSTN
{
    public static final boolean PRINTACC = true;
    public static final boolean PRINTWARNING = false;
    public static final boolean IGNOREINCONSIST = true;
    public static final boolean PATHPRINT = true;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if(args.length > 0)
        {
            if(args[0].equalsIgnoreCase("-gui"))
            {
                GUIApp gui = new GUIApp();
            }
            else
                readAndProcess(args[0]);
                // else presume input yaml file
        }
        else
        {
//            String out = "";

             testCase1();
//             testCase2();
//             testCase3();
//             testInitExt();
//             readAndProcess("/home/frans/Code/diagSTN/diag-stn/test/Data/partConsistent.yml");
//             out = "" + runRandomGen();
//             runSimpleRandomGen();
//             runBenchmark();
//            runSpdBenchmark();

//            boolean ans;
//            do
//            {
//               ans = runRandomGen();
//            }while(ans);
//
//            System.out.println("Right answer found: " + out + "\n");
        }
        
    }
    
    public static void readAndProcess(String file)
    {
        try
        {
            InputStream input = new FileInputStream(new File(file));
            Yaml yaml = new Yaml();
            Map<String, Object> fileMap = (Map<String, Object>) yaml.load(input);
            
            Graph graph = new Graph();
            
            List<Object> vertices = (List) fileMap.get("vertices");
            for(Object x: vertices)
            {
                Map<String, Object> vertexMap = (Map) x;
                Vertex v = new Vertex((int)vertexMap.get("id"),(String)vertexMap.get("name"));
                graph.addVertex(v);
            }
            
            List<Object> edges = (List) fileMap.get("edges");
            for(Object y: edges)
            {
                Map<String, Object> edgeMap = (Map) y;
                int startId = (int) edgeMap.get("start");
                int endId = (int) edgeMap.get("end");
                Vertex start = graph.getVertex(startId);
                Vertex end = graph.getVertex(endId);
                graph.addEdge(start, end, (int)edgeMap.get("lb"), (int)edgeMap.get("ub"));
            }
            
            Analyst analyst = new Analyst(graph);
            List<Object> observations = (List) fileMap.get("observations");
            for(Object z: observations)
            {
                Map<String, Object> obsMap = (Map) z;
                int startId = (int) obsMap.get("start");
                int endId = (int) obsMap.get("end");
                Vertex start = graph.getVertex(startId);
                Vertex end = graph.getVertex(endId);
                Observation o = new Observation(start,end,(int) obsMap.get("lb"),(int) obsMap.get("ub"));
                analyst.addObservation(o);
            }
            
            analyst.generatePaths();
            analyst.propagateWeights();
            analyst.generateDiagnosis();
            analyst.printPaths();
            analyst.printWeights();
            analyst.printDiagnosis();
            
        } catch (FileNotFoundException ex)
        {
            System.err.println("File does not exist");
            Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static boolean runRandomGen()
    {
        // for now no input variables (maybe later catch commandline input)
        
        GraphGenerator gen = new GraphGenerator();
        
        GraphObs strct = gen.generateBAGraph(50, 2, false, 2, 11, 20, true);
        //GraphObs strct = gen.generatePlanlikeGraph(7, 35, 45, 2, 2, 5, 9, 20, true);
        while(!strct.success)
        {
            strct = gen.generateBAGraph(50, 2, false, 2, 11, 20, true);
            //strct = gen.generatePlanlikeGraph(7, 35, 45, 2, 2, 5, 9, 20, true);
        }
        
        int fullPredIntSize = CorrectCheck.totalPredictionSize(strct);
        int fullNumEdges = CorrectCheck.numberUniqueEdges(strct);
        
        SOAnalyst al = new SOAnalyst(strct.graph);
        for(Observation ob : strct.observations)
        {
            al.addObservation(ob);
        }
        
        al.generatePaths();
//        al.printPaths();
//        al.propagateWeights();
//        al.printWeights();
        Diagnosis[] diag = al.generateDiagnosis();
//        al.printDiagnosis();
        CorrectCheck.printErrorsIntroduced(strct);
        
        ConAnalyst cal = new ConAnalyst(strct.graph);
        for(Observation ob : strct.observations)
        {
            cal.addObservation(ob);
        }
        cal.generatePaths();
        cal.propagateWeights();
        Diagnosis[] cdiag = cal.generateDiagnosis();
//        cal.printDiagnosis();
        
        System.out.println("result diff: " + (-CorrectCheck.compareDiagnosisSize(diag, cdiag)));
        System.out.println("Full prediction size: " + fullPredIntSize);
        System.out.println("Number of edges observed: " + fullNumEdges);
        
        return CorrectCheck.errorInDiagnoses(strct, diag);
    }
    
    public static void runSimpleRandomGen()
    {
        GraphGenerator gen = new GraphPerfGenerator();
        
//        GraphObs strct = gen.generateBAGraph(300, 3, false, 4, 10, 10, false);
        GraphObs strct = gen.generatePlanlikeGraph(20, 30, 60, 2, 2, 3, 5, 0, false);
        while(!strct.success)
        {
//            strct = gen.generateBAGraph(300, 3, false, 4, 10, 10, false);
            strct = gen.generatePlanlikeGraph(30, 30, 60, 2, 2, 30, 10, 0, false);
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
        al.generateDiagnosis();
        al.printDiagnosis();
    }
    
    public static void runBenchmark()
    {
        GraphGenerator gen = new GraphGenerator();
        GraphObs strct;
        Analyst al;
        boolean SOAnalyst = false;
        int iter = 10000;
        String location = "A-i10k-BAGraph-60-2-f-4-5-20-f.csv";
        // old: A-i5k-BAGraph-60-2-f-2-13-20-f.csv
        // A-i10k-PBGraph-4-8-12-2-2-2-5-20-t.csv
        
        // Write all output to a CSV file and add some explanation
        FileWriter writer = null;
        try
        {
            writer = new FileWriter(location,true);
            if(SOAnalyst)
            {
                writer.append("fullPredIntSize;fullNumEdges;errorFound;duration;"
                        + "diagLines;nor-SO\n");
            }
            else
            {
                writer.append("fullPredIntSize;fullNumEdges;errorFound;duration;"
                        + "diagLines;conDuration;resultDiff\n");
            }
            writer.flush();
        } catch (IOException ex)
        {
            System.err.println("Couldnt open file to write benchresults to");
            Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        long start, end;
        for(int i = 0; i < iter; i++)
        {
            // Generate a new Problem (Pb)
            strct = gen.generateBAGraph(60, 2, false, 4, 5, 20, false);
            //strct = gen.generatePlanlikeGraph(4, 8, 12, 2, 2, 2, 5, 20, true);
            while(!strct.success)
            {
                strct = gen.generateBAGraph(60, 2, false, 4, 5, 20, false);
                //strct = gen.generatePlanlikeGraph(4, 8, 12, 2, 2, 2, 5, 20, true);
            }
            int fullPredIntSize = CorrectCheck.totalPredictionSize(strct);
            int fullNumEdges = CorrectCheck.numberUniqueEdges(strct);
            
            // Check if it is a solvable problem, only when using SOAnalyst
//            if(!solvable(strct))
//                continue;
            
            // Create analyst and time the analysis (and check the output)
            if(!SOAnalyst)
                al = new Analyst(strct.graph);
            else
                al = new SOAnalyst(strct.graph);
            for(Observation ob : strct.observations)
            {
                al.addObservation(ob);
            }
            start = System.nanoTime();
            al.generatePaths();
            if(!SOAnalyst)
                al.propagateWeights();
            Diagnosis[] diag = al.generateDiagnosis();
            end = System.nanoTime();
            boolean errorFound = CorrectCheck.errorInDiagnoses(strct, diag);
            
            // Consistency based diagnosis (+fm) comparison
            int resultDiff = 0;
            long startCon = 0;
            long endCon = 0;
            ConAnalyst cal = new ConAnalyst(strct.graph);
            for(Observation ob : strct.observations)
            {
                cal.addObservation(ob);
            }
            startCon = System.nanoTime();
            cal.generatePaths();
            cal.propagateWeights(); // Is never SO optimized
            Diagnosis[] cdiag = cal.generateDiagnosis();
            endCon = System.nanoTime(); // Both in time
            resultDiff = -CorrectCheck.compareDiagnosisSize(diag, cdiag);
            
            try
            {
                writer.append(fullPredIntSize + ";" + fullNumEdges + ";" +
                        errorFound + ";" + (end - start) + ";" +
                         al.diagSize() + ";" + (endCon - startCon) + ";" +
                        resultDiff + "\n");
                if(i % 100 == 0)
                    writer.flush();
            } catch (Throwable ex)
            {
                System.err.println("Couldnt append line to file");
                Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try
        {
            writer.flush();
            writer.close();
        } catch (IOException ex)
        {
            System.err.println("Couldnt close file to write benchresults to");
            Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static void runSpdBenchmark()
    {
        GraphGenSettings setting = new GraphGenSettings();
        setting.BAGraph(50, 2, false, 2, 5, 20, false);
//        setting.planlikeGraph(4, 8, 12, 2, 2, 2, 5, 20, true);
        runSpdBenchmark(setting,10000,false);
        System.out.println(setting);

    }   
    
    public static void runSpdBenchmark(GraphGenSettings setting, int iter, boolean SOAnalyst)
    {
        GraphPerfGenerator gen = new GraphPerfGenerator();
        GraphObs strct;
        Analyst al;
        // Put settings separate
//        boolean SOAnalyst = false;
//        int iter = 20000;
//        GraphGenSettings setting = new GraphGenSettings();
//        setting.BAGraph(30, 2, false, 2, 5, 20, false);
//        setting.planlikeGraph(4, 8, 12, 2, 2, 2, 5, 20, true);
        
        // Build output string
        String analyst, location;
        if(SOAnalyst)
            analyst = "SOA";
        else
            analyst = "A";
        if(setting.type == GraphGenSettings.BAGRAPH)
            location = String.format(analyst +"-i%dk-BAGraph-%d-%d-%b-%d-%d-%d"
                    + "-%b.csv",(iter / 1000),setting.vertexSize,
                    setting.BALinksPerVertexAddition,setting.onlyMaxAdditions,
                    setting.numObservations,setting.observationLength,
                    setting.difference,setting.timeSyncT0);
        else if (setting.type == GraphGenSettings.PLANLIKEGRAPH)
            location = String.format(analyst +"-i%dk-PBGraph-%d-%d-%d-%d-%d-%d"
                    + "-%d-%d-%b.csv",(iter / 1000),setting.numLines,
                    setting.lineLengthLB,setting.lineLengthUB,
                    setting.maxInterLineConnect,setting.maxLineVertConnect,
                    setting.numObservations,setting.observationLength,
                    setting.difference,setting.timeSyncT0);
        else
        {
            System.err.println("No normal setting used for benchmark");
            location = "unknown.csv";
        }
        // old: A-i5k-BAGraph-60-2-f-2-13-20-f.csv
        // A-i10k-PBGraph-4-8-12-2-2-2-5-20-t.csv
        
        // Write all output to a CSV file and add some explanation
        FileWriter writer = null;
        try
        {
            writer = new FileWriter(location,true);
            writer.append("totalNumEdges;numUniqueEdges;duration\n");
            writer.flush();
        } catch (IOException ex)
        {
            System.err.println("Couldnt open file to write benchresults to");
            Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        long start, end;
        for(int i = 0; i < iter; i++)
        {
            // Generate a new Problem (Pb)
//            strct = gen.generateBAGraph(setting);
//            strct = gen.generatePlanlikeGraph(setting);
            strct = gen.generateProblem(setting);
            while(!strct.success)
            {
//                strct = gen.generateBAGraph(setting);
//                strct = gen.generatePlanlikeGraph(setting);
                strct = gen.generateProblem(setting);
            }
            int totalNumEdges = CorrectCheck.totalNumberEdges(strct);
            int uniqueEdges = CorrectCheck.numberUniqueEdges(strct);
            
            
            if(!SOAnalyst)
                al = new Analyst(strct.graph);
            else
                al = new SOAnalyst(strct.graph);
            for(Observation ob : strct.observations)
            {
                al.addObservation(ob);
            }
            start = System.nanoTime();
            al.generatePaths();
            if(!SOAnalyst)
                al.propagateWeights();
            Diagnosis[] diag = al.generateDiagnosis();
            end = System.nanoTime();
            
            // Consistency based diagnosis (+fm) comparison
            // For now remove Consistency based diagnosis comparison
            
            try
            {
                writer.append(uniqueEdges + ";" + totalNumEdges + ";" + 
                        (end - start) + "\n");
                if(i % 500 == 0)
                {
                    writer.flush();
                    System.out.print(".");
                }
            } catch (Throwable ex)
            {
                System.err.println("Couldnt append line to file");
                Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try
        {
            writer.flush();
            writer.close();
        } catch (IOException ex)
        {
            System.err.println("Couldnt close file to write benchresults to");
            Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private static boolean solvable(GraphObs go)
    {
        Analyst al = new Analyst(go.graph);
        for(Observation ob : go.observations)
        {
            al.addObservation(ob);
        }
        long start = System.nanoTime();
        al.generatePaths();
        al.propagateWeights();
        Diagnosis[] diag = al.generateDiagnosis();
        long finish = System.nanoTime();
        boolean errorFound = CorrectCheck.errorInDiagnoses(go, diag);
        return errorFound;
    }
    
    public static void testCase1()
    {
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
        graph.addEdge(c, e, 13, 9999);
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
    
    public static void testCase2()
    {
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
        
        graph.addEdge(a, b, 5, 10);
        graph.addEdge(b, c, 8, 20);
        graph.addEdge(c, d, 4, 6);
        graph.addEdge(d, e, 55, 70);
        graph.addEdge(e, f, 10, 20);
        graph.addEdge(f, g, 9, 14);
        
        // t0 to t3 == [ 17 - 36]
        // t0 to t6 == [ 91 - 140]
        
        Observation ob1 = new Observation(a,d,17,36);
        Observation ob2 = new Observation(a,g,70,115);
        
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
    
    public static void testCase3()
    {
        int ids = 0;
        
        // Explain the STN
        Graph graph = new Graph();
        Vertex a = new Vertex(ids++, "a");
        Vertex b = new Vertex(ids++, "b");
        Vertex c = new Vertex(ids++, "c");
        Vertex d = new Vertex(ids++, "d");
        Vertex e = new Vertex(ids++, "e");
        Vertex f = new Vertex(ids++, "f");
        Vertex g = new Vertex(ids++, "g");
        Vertex h = new Vertex(ids++, "h");
        Vertex i = new Vertex(ids++, "i");
        Vertex j = new Vertex(ids++, "j");
        Vertex k = new Vertex(ids++, "k");
        Vertex l = new Vertex(ids++, "l");
        
        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);
        graph.addVertex(e);
        graph.addVertex(f);
        graph.addVertex(g);
        graph.addVertex(h);
        graph.addVertex(i);
        graph.addVertex(j);
        graph.addVertex(k);
        graph.addVertex(l);
        
        graph.addEdge(a, b, 3, 6);
        graph.addEdge(b, c, 4, 7);
        graph.addEdge(c, d, 10, 12);
        graph.addEdge(d, e, 5, 9);
        graph.addEdge(e, f, 18, 19);
        graph.addEdge(b, g, 5, 8);
        graph.addEdge(g, h, 8, 11);
        graph.addEdge(h, i, 1, 16);
        graph.addEdge(h, e, 6, 9);
        graph.addEdge(g, j, 7, 22);
        graph.addEdge(j, k, 14, 16);
        graph.addEdge(k, l, 3, 12);
        
        // 3 obs between a-f, a-i and a-l
        Observation ob1 = new Observation(a,f,40,53); // correct obs
        //Observation ob2 = new Observation(a,i,17,41); // correct obs
        Observation ob2 = new Observation(a,i,36,65);
        //Observation ob3 = new Observation(a,l,32,64); // correct obs
        Observation ob3 = new Observation(a,l,28,48);
        // analysis part
        Analyst analyst = new Analyst(graph);
        analyst.addObservation(ob1);
        analyst.addObservation(ob2);
        analyst.addObservation(ob3);
        
        analyst.generatePaths();
        
        analyst.printPaths();
        
        analyst.propagateWeights();
        
        analyst.printWeights(ob1);
        
        analyst.printWeights(ob2);
        
        analyst.printWeights(ob3);
        
        analyst.generateDiagnosis();
        
        analyst.printDiagnosis();
    }

    public static void testInitExt()
    {
        GraphGenerator gen = new GraphGenerator();
        
        //gen.testAncest();
        Graph g = gen.testInit();
        
        Observation ob1 = new Observation(g.getVertex(15),g.getVertex(6),50,100);
        Observation ob2 = new Observation(g.getVertex(7),g.getVertex(6),50,100);
        Observation ob3 = new Observation(g.getVertex(12),g.getVertex(6),50,100);
        
        // analysis part
        Analyst analyst = new Analyst(g);
        analyst.addObservation(ob1);
        analyst.addObservation(ob2);
        analyst.addObservation(ob3);
        
        analyst.generatePaths();
        
        analyst.printPaths();
        
        analyst.propagateWeights();
        
        analyst.printWeights(ob1);
        
        analyst.printWeights(ob2);
        
        analyst.generateDiagnosis();
        
        analyst.printDiagnosis();
    }
    /**
     * What about inf. UB on edges, see example STN in paper, 
     * its ok for diagnosis to say [-inf, 19] because no change would be 
     * proposed
     */
}

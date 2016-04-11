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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if(args.length > 0)
        {
            readAndProcess(args[0]);
            return;
        }
        
        // testCase1();
        // readAndProcess("/home/frans/Code/diagSTN/diag-stn/test/Data/testSerialization.yml");
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
                Observation o = new Observation(start,end,(int) obsMap.get("lb"),(int) obsMap.get("end"));
                analyst.addObservation(o);
            }
            
            analyst.generatePaths();
            analyst.propagateWeights();
            analyst.generateDiagnosis();
            analyst.printDiagnosis();
            
        } catch (FileNotFoundException ex)
        {
            System.err.println("File does not exist");
            Logger.getLogger(DiagSTN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
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
    
    public static void testCase2()
    {
        
    }

    /**
     * What about inf. UB on edges, see example STN in paper, 
     * its ok for diagnosis to say [-inf, 19] because no change would be 
     * proposed
     */
}

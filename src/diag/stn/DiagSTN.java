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

/**
 *
 * @author frans
 */
public class DiagSTN
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        testCase1();
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
    }

    /**
     * What about inf. UB on edges, see example STN in paper, 
     * its ok for diagnosis to say [-inf, 19] because no change would be 
     * proposed
     */
}

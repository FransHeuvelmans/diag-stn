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
import java.util.List;
import java.util.Random;

/**
 * Generates (Random) Graphs according to certain rules (see methods)
 * @author Frans van den Heuvel
 */
public class GraphGenerator
{
    /**
     * Struct like combination of the Graph and its Observations
     */
    public class GraphObs
    {
        Graph graph;
        List<Observation> observations;
    }
    
    
    /**
     * Generates a new Graph (roughly) according to the Barabàsi-Albert model.
     * Applied to a directed Graph (so never more than x outgoing edges but can
     * have many incoming!)
     * @return filled Graph
     */
    public GraphObs generateBAGraph(int size, int linksPerStep, boolean onlymax)
    {
        Random rand = new Random();
        GraphObs grOb = new GraphObs();
        
        /**
         * Maybe use hashmap to store incoming # for each vertex and store those
         * need it somewhere (best in here)
         */
        
        int id = 1;
        Graph gr = new Graph();
        Vertex first = new Vertex(id);
        gr.addVertex(first);
        id++;
        Vertex second = new Vertex(id);
        id++;
        int lb = rand.nextInt(51);
        int ub = lb + rand.nextInt(21);
        gr.addEdge(second, first, lb, ub); 
        // Keep this direction for all the vertices to add (new to old)
        if(size < 3)
        {
            System.err.println("Graph generation is only for larger graphs "
                    + "ie. > 2");
            grOb.graph = gr;
            return grOb;
        }
        if(linksPerStep > size)
        {
            System.err.println("Can't have more edges than vertices for this"
                    + " AB method");
            grOb.graph = gr;
            return grOb;
        }
        for(int i = 2; i < size; i++) // already 2 vertices
        {
            int links = Math.min(i + 1, linksPerStep);
            ABaddVertex(gr, links, onlymax);
        }
        
        grOb.graph = gr;
        
        /**
         * Idea to generate Observations as well and have those added to grOb 
         * probably best to have some of the last vertices (given the ordering 
         * new to old) to some of the first vertices.
         * To test the method better some true observations need to be added 
         * as well. (so PathFinding algo needs to be implemented here as well
         * or used from analyst!). Then by adding some change (and printing that)
         * one or multiple obs get changed and those can be compared to the algo
         * output.
         */
        
        return grOb;
    }
    
    private void ABaddVertex(Graph g, int links, boolean onlymax)
    {
        // create this new vertex and add some edges !
    }
    
}

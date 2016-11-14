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
import diag.stn.analyze.GraphPath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

/**
 *
 * @author Frans van den Heuvel
 */
public class GraphPerfGenerator extends GraphGenerator
{
    /* BA generation variables */
    private int id; // Store an id counter
    private ArrayList<BuildVertex> vertInfo; // Connection degree f/e Vert
    private int nodes, edges; // # nodes & edges for BAaddVert()
    // extra edges counter for use when calculating probability of attachement
    private int numEdges; 
    
    
    public GraphPerfGenerator()
    {
        super();
    }
    
    /**
     * Generate a BA graph with GraphObservation object without a certain error 
     * position. Useful for performance testing larger networks.
     * @return 
     */
    @Override
    public GraphObs generateBAGraph(int size, int linksPerStep, boolean onlymax, 
            int observations, int obsLength, int diff, boolean zeroPoint)
    {
        if(observations < 1)
        {
            System.err.println("# of false observations needs to be positive");
            observations = 1; // can't use a GraphObs with no observations
        }
        GraphObs grOb = new GraphObs();
        vertInfo = new ArrayList();
        
        // First step is to generate the basic graph with correct form but 
        // without initialized bounds (since we might need to add a T0 later)
        id = 0;
        Graph gr = new Graph();
        gr.reverseNegativeEdge(false);
        Vertex first = new Vertex(id);
        gr.addVertex(first);
        id++;
        Vertex second = new Vertex(id);
        gr.addVertex(second);
        id++;
        gr.addEdge(second, first, 0, 0);
        // Keep this direction for all the vertices to add (new to old)
        BuildVertex bv = new BuildVertex();
        bv.vert = first;
        bv.degree = 1;
        vertInfo.add(bv);
        bv = new BuildVertex();
        bv.vert = second;
        bv.degree = 1;
        vertInfo.add(bv);
        numEdges = 1;  
        
        nodes = 2;
        edges = 1;
        
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
            linksPerStep = size;
        }
        for(int i = 2; i < size; i++) // already 2 vertices
        {
            int links = Math.min(i, linksPerStep);
            BAaddVertex(gr, links, onlymax);
        }
        
        // Store settings
        GraphGenSettings gs = new GraphGenSettings();
        gs.BAGraph(size, linksPerStep, onlymax, observations, obsLength, diff, 
                zeroPoint);
        
        /**
         * Because this Class is made to construct Problems simply for
         * performance testing, this part is different from GraphGen.
         * The networks just have some random initialization, and the errors are
         * calculated very simple.
         */
        
        grOb.graph = initializeBounds(gr);
        
        addErrors(grOb, gs);
        
        if(grOb.observations.size() >= gs.numObservations)
            grOb.success = true; // Enough observations so continue
        else
            grOb.success = false;
        
        grOb.settings = gs;
        
        return grOb;
    }
    
    // Same as GraphGen to keep them interchangable
    @Override
    public GraphObs generateBAGraph(GraphGenSettings gs)
    {
        if(gs.type != GraphGenSettings.BAGRAPH)
            System.err.println("Not correct (BA) type of settings");
        return generateBAGraph(gs.vertexSize,gs.BALinksPerVertexAddition,
                gs.onlyMaxAdditions, gs.numObservations, gs.observationLength,
                gs.difference, gs.timeSyncT0);
    }
    
    // Similar to GraphGen functions but made for Performance testing
    private void BAaddVertex(Graph g, int links, boolean onlymax)
    {
        Random rand = new Random();
        // create this new vertex and add some edges !
        Vertex newV = new Vertex(id);
        id++;
        g.addVertex(newV);
        BuildVertex nbv = new BuildVertex();
        nbv.degree = 0;
        nbv.vert = newV;
        
        // Bit of double bookkeeping here but for now..
        HashSet<Vertex> usedToVertices = new HashSet();       
        
        // Decide how many connections to make 
        if(!onlymax)
        {
            int oldlinks = links;
            links = rand.nextInt(oldlinks);
            // We allow 0 links, for the network to get more than 1 end point!
        }
        
        //int ignore = 0;
        int add = 0;
        LinkedList<BuildVertex> vertexDegPlus = new LinkedList();
        while(links>0)  // While there are new edges needed, addmore!
        {
            double prob = 0;    // the odds
            double randNum = rand.nextDouble();
            
            addEdge:
            for(BuildVertex bigv : vertInfo)    // For each of the current Vertex
            {
                // TODO: Possibility to check for paths before adding it
                
                prob += (double) ((double) bigv.degree) / 
                        ((double) (2.0d * numEdges)); // (none to ignore!)
                
                if(randNum <= prob) // add edge time
                {
                    // Cant have multiple edges between 2 verts in STN
                    if(usedToVertices.contains(bigv.vert))
                        break;
                    
                    int lb,ub;
                    
                    g.addEdge(newV, bigv.vert, 0, 0);
                    
                    
                    vertexDegPlus.add(bigv);
                    vertexDegPlus.add(nbv);
                    add++;
                    links--;
                    usedToVertices.add(bigv.vert);
                    
                    edges++;
                    break; // break out of for loop, time for next edge!
                }
            }
        }
        // Increment the number of edges and vertex-degrees after all edges have
        // been added (so the edge addition does not influence itself)
        numEdges += add;
        for(BuildVertex buv : vertexDegPlus)
            buv.degree++; 
        
        // Finally add the new buildVertex to vertexInfo
        vertInfo.add(nbv); // should be @ location "id" ...
        
        nodes++;
    }
    
     /** 
     * The dirty method for adding bounds. Bounds are between [0, 100).
     * <b>WARNING: Creates inconsistent networks !</b>.  
     * @param graphIn
     * @return 
     */
    private Graph initializeBounds(Graph graphIn)
    {
        Random rand = new Random();
        
        DEdge[] allEdges = graphIn.listAllEdges();
        for(DEdge de : allEdges)
        {
            de.setLowerb(0);
            de.setUpperb(rand.nextInt(100));
            // Use bad networks for performance testing ??
        }
        return graphIn;
    }

    /* Adds random errors and tries to use the correct length */
    private void addErrors(GraphObs graphPlusObser, GraphGenSettings graphSettings)
    {
        Random rand = new Random();
        Vertex startSync = null;
        Graph gr = graphPlusObser.graph; // Easy Graph reference
        
        if(graphSettings.timeSyncT0)
        {
            startSync = new Vertex(Integer.MAX_VALUE,"S");
            gr.addVertex(startSync);
            // New Vertex added but no bounds (bec. no edges -> those need init)
        }
        
        int observations = graphSettings.numObservations;
        int obsLength = graphSettings.observationLength;
        
        int trys = 200000; // tries remaining for with obsLength
        // Try to add observations of a certain length but not sure if its
        // possible 
        int currentLen; // length remaining
        ArrayList<Vertex[]> falsieSubs = new ArrayList(); // all Obs pairs
        
        if(graphSettings.timeSyncT0) // going to add an edge lateron
            obsLength--;
        while(observations > 0 && trys > 0)
        {
            currentLen = obsLength;
            
            // Do not care about fault so some random edge is good
            DEdge randomEdge = gr.randomEdge();
            Vertex end = randomEdge.getEnd();
            Vertex start = randomEdge.getStart();
            LinkedList<DEdge> path = new LinkedList();
            path.add(randomEdge);
            currentLen--;
            
            // First search forward for this Observation-path
            while(currentLen > 0)
            {
                LinkedList<Vertex> possibilities = gr.adjacentNodes(end);
                if(possibilities == null || possibilities.size() < 1)
                    break;
                Vertex newEnd = possibilities.get(rand.nextInt(possibilities.size()));
                DEdge de = gr.getDirectEdge(end, newEnd);
                path.add(de);
                end = newEnd;
                currentLen--;
            }
            // Then look for an earlier starting position for the Obs-path
            while(currentLen > 0)
            {
                LinkedList<Vertex> possibilities = gr.incomingNodes(start);
                if(possibilities == null || possibilities.size() < 1)
                    break;
                Vertex newStart = possibilities.get(rand.nextInt(possibilities.size()));
                DEdge de = gr.getDirectEdge(newStart, start);
                path.push(de);
                start = newStart;
                currentLen--;
            }
            
            // Usable Observation pair (right length is all we need here)
            if(currentLen < 1)
            {
                observations--;
                Vertex[] obs = {start, end};
                falsieSubs.add(obs);
            }
            trys--;
        }
        // Should there be more observations ? -> try using a smaller length
        if(observations > 0)
        {
            graphSettings.observationLength--;
            addErrors(graphPlusObser, graphSettings); 
            // Recursive call!, might cause Memory probs. 
            // Tries again with smaller observations
        }
        // Add T0 edges if needed !! & Replace the starting vertices
        if(graphSettings.timeSyncT0)
        {
            for(Vertex[] obs : falsieSubs)
            {
                // See if the edge exists, if not add it!
                if(!gr.directReach(startSync, obs[0]))
                {
                    // Add that edge!
                    gr.addEdge(startSync, obs[0], 0, 0);
                }
                obs[0] = startSync; // and replace the starting Vertex with T0
            }
        }
        
        // Now initialize these Vertex-pairs as true observations (with errors!)
        graphPlusObser.observations = new LinkedList();
        
        for(Vertex[] obs : falsieSubs)
        {
            GraphPath startPath = new GraphPath(obs[0]);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, obs[1], 
            gr);
            int[] combinedBound = combinePaths(boundsFound);
            
            // The error addition
            combinedBound[0] += 500;
            combinedBound[1] += 500;
            
            Observation fullObs = new Observation(obs[0],obs[1],combinedBound[0]
                    , combinedBound[1]);
            graphPlusObser.observations.add(fullObs);
        }
    }
    
    /**
     * Generate a problem using a Plan-like network as basis without a certain 
     * error position. Useful for performance testing larger networks.
     * @return 
     */
    public GraphObs generatePlanlikeGraph(int line, int linelb, int lineub, 
            int maxLineCon, int maxVertCon, int observations, int obsLength, 
            int diff, boolean zeroPoint)
    {
        if(line < 1)
        {
            System.out.println("Cant have negative # paths");
            line = 1;
        }
        if(linelb < 1)
        {
            System.out.println("Cant have negative # nodes");
            linelb = 1;
        }
        if(lineub < linelb)
        {
            System.out.println("Upperbound on # nodes per path cant "
                    + "be less than lowerbound");
            lineub = linelb;
        }
        if(maxLineCon < 0)
        {
            System.out.println("# path connections can't be negative");
            maxLineCon = 0;
        }
        if(maxVertCon < 0)
        {
            System.out.println("# Vertex connections per path can't be negative");
            maxVertCon = 0;
        }
        //init
        Random rand = new Random();
        GraphObs grOb = new GraphObs();
        int orLine = line;
        int orObs = observations;
        
        id = 0;
        numEdges = 0;
        Graph gr = new Graph();        
        LinkedList<Vertex>[] lines = new LinkedList[line];
        
        int randNod = lineub - linelb;
        int lineVertices, ub, lb;
        while(line > 0)
        {
            // add a line
            lineVertices = linelb + rand.nextInt(randNod+1);
            Vertex firstV = new Vertex(id);
            gr.addVertex(firstV);
            id++;
            Vertex prevV = firstV;
            lineVertices--;
            lines[line-1] = new LinkedList(); // so from arrayIndex = line -> 0
            lines[line-1].add(firstV);
            while(lineVertices > 0)
            {
                // add a vertex
                Vertex nextV = new Vertex(id);
                gr.addVertex(nextV);
                id++;
                lines[line-1].add(nextV);
                
                gr.addEdge(nextV, prevV, 0, 0); // WARNING!! from next to prev again!
                
                // onto the next
                prevV = nextV;
                lineVertices--;
            }
            line--;
        }
        // Connect the different plans
        for(int l = 0; l < lines.length; l++)
        {
            int lineConnects = rand.nextInt(maxLineCon) + 1;
            LinkedList<Vertex> conLine = lines[l];
            while(lineConnects > 0)
            {
                int linePick = rand.nextInt(lines.length);
                if(linePick == l)
                    continue;
                int vertConnect = rand.nextInt(maxVertCon) + 1;
                while(vertConnect > 0)
                {
                    int vertStart = rand.nextInt(conLine.size());
                    int usedSpace = conLine.size() - vertStart;
                    int space = lines[linePick].size() - (usedSpace + 1); 
                    // 3 room extra in between
                    if(space < 1)
                        continue;
                    int vertEnd = rand.nextInt(space);
                    
                    lb = 0;
                    ub = rand.nextInt(30);
                    gr.addEdge(conLine.get(vertStart),
                            (lines[linePick].get(vertEnd)), lb, ub);
                    vertConnect--;
                }
                lineConnects--;
            }
        }
        
        // Store settings
        GraphGenSettings gs = new GraphGenSettings();
        gs.planlikeGraph(orLine, linelb, lineub, maxLineCon, maxVertCon, orObs, 
                obsLength, diff, zeroPoint);
        
        // init the bounds randomly
        grOb.graph = initializeBounds(gr);
        
        // Add some bounds to grOb
        addErrors(grOb, gs);
        
        if(grOb.observations.size() >= gs.numObservations)
            grOb.success = true; // Enough observations so continue
        else
            grOb.success = false;
        
        grOb.settings = gs;
        
        return grOb;
    }
}

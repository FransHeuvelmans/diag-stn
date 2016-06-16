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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Generates (Random) Graphs according to certain rules (see methods)
 * @author Frans van den Heuvel
 */
public class GraphGenerator
{
    
    private int id; // stores Vertex id & names
    private ArrayList<BuildVertex> vertInfo; // stores degrees & Vertices baby please
    private int numEdges;
    
    private int nodes, edges;
    
    /**
     * Struct like combination of the Graph and its Observations
     */
    public class GraphObs
    {
        public Graph graph;
        public List<Observation> observations;
    }
    
    private class BuildVertex
    {
        public Vertex vert;
        public int degree;
    }
    
    /**
     * Generates a new Graph (roughly) according to the Barabàsi-Albert model.
     * Applied to a directed Graph (so never more than x outgoing edges but can
     * have many incoming!)
     * @return filled Graph
     */
    public GraphObs generateBAGraph(int size, int linksPerStep, boolean onlymax, int falseObs, int trueObs)
    {
        if(falseObs < 1)
        {
            System.err.println("# of false observations needs to be positive");
            falseObs = 1; // can't use a GraphObs with no observations
        }
        if(trueObs < 0)
        {
            System.err.println("# of true observations needs to be non-negative");
            trueObs = 0;
        }
        
        Random rand = new Random();
        GraphObs grOb = new GraphObs();
        vertInfo = new ArrayList();
        
        /**
         * Maybe use hashmap to store incoming # for each vertex and store those
         * need it somewhere (best in here)
         */
        
        id = 0;
        Graph gr = new Graph();
        gr.reverseNegativeEdge(false);
        Vertex first = new Vertex(id);
        gr.addVertex(first);
        id++;
        Vertex second = new Vertex(id);
        gr.addVertex(second);
        id++;
        int lb = rand.nextInt(20);
        int ub = lb + rand.nextInt(60);
        gr.addEdge(second, first, lb, ub);
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
            System.out.println("Nodes added: " + nodes + " edges added: " + edges);
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
        
        grOb.observations = new LinkedList();
        
        falseObsAdd:
        while(falseObs > 0)
        {
            // Since its new to old try to get a path between 2 by trialNerror
            int half = vertInfo.size() /2;
            BuildVertex fromV = vertInfo.get(half + rand.nextInt(vertInfo.size() - half));
            BuildVertex toV = vertInfo.get(rand.nextInt(half));
            
            for(Observation fObs : grOb.observations)
            {
                if(fObs.startV.equals(fromV.vert))
                {
                    if(fObs.endV.equals(toV.vert))
                        continue falseObsAdd; // yep, its the quickNDirty
                    // Can't have a certain obs already in use
                }
            }
            
            GraphPath startPath = new GraphPath(fromV.vert);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV.vert, gr);
            int[] boufou = combinePaths(boundsFound);
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // TODO Calc proper lb & ub !!!!
                Observation ob = new Observation(fromV.vert, toV.vert, boufou[0], boufou[1]);
                grOb.observations.add(ob);
                falseObs--;
                /*
                TODO: Prob. -> no changes in bounds are added but still there
                is some discrepancy between the propagated values here and the ones
                from Analyst so there is some problem!
                Current version only finds the first path and uses that value
                (which is not the correct value for a consistent path)
                */
//                System.out.println("For Observation " + fromV.vert.getID() +
//                            " to " + toV.vert.getID());
//                System.out.println("Generating LB:" + boundsFound.get(0)[0] + 
//                            " UB:" + boundsFound.get(0)[1]);
            }
        }
        trueObsAdd:
        while(trueObs > 0)
        {
            // Since its new to old try to get a path between 2 by trialNerror
            int half = vertInfo.size() /2;
            BuildVertex fromV = vertInfo.get(half + rand.nextInt(vertInfo.size() - half));
            BuildVertex toV = vertInfo.get(rand.nextInt(half));
            
            
            for(Observation aObs : grOb.observations)
            {
                if(aObs.startV.equals(fromV.vert))
                {
                    if(aObs.endV.equals(toV.vert))
                        continue trueObsAdd; // yep, its the quickNDirty
                    // Can't have a certain (any) obs already in use (be it
                    // either false or true)
                }
            }
            
            GraphPath startPath = new GraphPath(fromV.vert);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV.vert, gr);
            int[] boufou = combinePaths(boundsFound);
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // if it is fully correct all paths should be the same... !!!!
                Observation ob = new Observation(fromV.vert, toV.vert, boufou[0], boufou[1]);
                grOb.observations.add(ob);
                trueObs--;
//                System.out.println("For Observation " + fromV.vert.getID() +
//                            " to " + toV.vert.getID());
//                System.out.println("Generating LB:" + boundsFound.get(0)[0] + 
//                            " UB:" + boundsFound.get(0)[1]);
            }
        }
        
        return grOb;
    }
    
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
        int tries = 300; // 300 random tries to add an edge, if it cant be done it cant be done!
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
                    
                    lb = 0;
                    ub = 20 + rand.nextInt(60);
                    g.addEdge(newV, bigv.vert, lb, ub);
                    
                    
                    vertexDegPlus.add(bigv);
                    vertexDegPlus.add(nbv);
                    add++;
                    links--;
                    usedToVertices.add(bigv.vert);
                    
                    edges++;
                    break; // break out of for loop, time for next edge!
                }
            }
            if(tries < 1)
                break;
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
    
    private ArrayList<int[]> pathCalc(GraphPath g, int lb, int ub, Vertex end, Graph graph)
    {
        int dlb,dub;
        ArrayList<int[]> pathLbUbs = new ArrayList<>();
        // combine generatePaths & propagateWeights
        LinkedHashSet<DEdge> edgeExp = graph.possibleEdges(g.getLastV());
        
         if(edgeExp == null)
            return pathLbUbs; // dead end!
        for(DEdge de : edgeExp)
        {
            if(!g.edgeUsed(de))
            {
                g.addStep(de, de.getEnd());
                dlb = de.getLowerb();
                dub = de.getUpperb();
                lb += dlb;
                ub += dub;
                if(de.getEnd().equals(end))
                {
                    int[] lbub = new int[2];
                    lbub[0] = lb;
                    lbub[1] = ub;
                    pathLbUbs.add(lbub);
                }
                ArrayList<int[]> returnedLbUbs;
                if(!g.edgeUsed(de))  // shouldn't be part of current path(takes)
                {
                    returnedLbUbs = pathCalc(g,lb,ub, end, graph);
                    pathLbUbs.addAll(returnedLbUbs);
                }
                g.removeLast();
            }
        }
        
        return pathLbUbs; 
    }
    
    private int[] combinePaths(ArrayList<int[]> paths)
    {
        // Takes the Intersection
        int[] finalbounds = paths.remove(paths.size()-1); // take last
        for(int[] p: paths)
        {
            if(p[0] > finalbounds[0])
                finalbounds[0] = p[0];
            if(p[1] < finalbounds[1])
                finalbounds[1] = p[1];
        }
        return finalbounds;
    }
    
    /**
     * Create a Graph that resembles multiple plans that have a few connections
     * @param line number of plans coming together
     * @param linelb lowerbound on plansize
     * @param lineub upperbound on plansize
     * @param maxLineCon how much the plans are interconnected 
     * @param maxVertCon how much 2 plans can be interconnected
     * @param falseObs how many false observations are added
     * @param trueObs how many true observations are added
     * @return 
     */
    public GraphObs generatePlanlikeGraph(int line, int linelb, int lineub, 
            int maxLineCon, int maxVertCon, int falseObs, int trueObs)
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
        // no vertInfo needed
        id = 0;
        numEdges = 0;
        Graph gr = new Graph();        
        LinkedList<Vertex>[] lines = new LinkedList[line];
        
        int randNod = linelb - lineub;
        int lineVertices, ub, lb;
        while(line > 0)
        {
            // add a line (adaline!)
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
                
                // add an edge
                lb = 0; // TODO! also the dirty solution!!
                ub = rand.nextInt(120);
                gr.addEdge(nextV, prevV, lb, ub); // WARNING!! from next to prev again!
                
                // onto the next
                prevV = nextV;
                lineVertices--;
            }
            
            // onto the next
            line--;
        }
        for(int l = 0; l < lines.length; l++)
        {
            // Connect dem lines!!
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
                    int space = lines[linePick].size() - vertStart;
                    if(space < 1)
                        continue;
                    int vertEnd = rand.nextInt(space) + vertStart;
                    
                    lb = 0; // TODO! also the dirty solution!!
                    ub = rand.nextInt(30);
                    gr.addEdge(conLine.get(vertStart),
                            (lines[linePick].get(vertEnd)), lb, ub);
                    
                    vertConnect--;
                }
                linePick--;
            }
        }
        
        // time to bruteforce some observations with this Graph...
         grOb.observations = new LinkedList();
        
        falseObsAdd:
        while(falseObs > 0)
        {
            int startLine, endLine, startV, endV;
            startLine = rand.nextInt(lines.length);
            endLine = rand.nextInt(lines.length);
            startV = rand.nextInt(4);
            if(startV >= lines[startLine].size())
                continue falseObsAdd;
            endV = lines[endLine].size() - rand.nextInt(4);
            if(endV < 0)
                continue falseObsAdd;
            
            Vertex fromV, toV;
            fromV = lines[startLine].get(startV);
            toV = lines[endLine].get(endV);
            
            for(Observation fObs : grOb.observations)
            {
                if(fObs.startV.equals(fromV))
                {
                    if(fObs.endV.equals(toV))
                        continue falseObsAdd; // yep, its the quickNDirty
                    // Can't have a certain obs already in use
                }
            }
            
            GraphPath startPath = new GraphPath(fromV);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV, gr);
            int[] boufou = combinePaths(boundsFound);
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // TODO PROBLEM, SEE OTHER GEN !!!!
                Observation ob = new Observation(fromV, toV, boufou[0], boufou[1]);
                grOb.observations.add(ob);
                falseObs--;
            }
        }
        trueObsAdd:
        while(trueObs > 0)
        {
            int someL, startV, endV;
            someL = rand.nextInt(lines.length);
            startV = rand.nextInt(4);
            if(startV >= lines[someL].size())
                continue trueObsAdd;
            endV = lines[someL].size() - rand.nextInt(4);
            if(endV < 0)
                continue trueObsAdd;
            
            Vertex fromV, toV;
            fromV = lines[someL].get(startV);
            toV = lines[someL].get(endV);
            
            for(Observation aObs : grOb.observations)
            {
                if(aObs.startV.equals(fromV))
                {
                    if(aObs.endV.equals(toV))
                        continue trueObsAdd; // yep, its the quickNDirty
                    // Can't have a certain (any) obs already in use (be it
                    // either false or true)
                }
            }
            
            GraphPath startPath = new GraphPath(fromV);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV, gr);
            int[] boufou = combinePaths(boundsFound);
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // if it is fully correct all paths should be the same... !!!!
                Observation ob = new Observation(fromV, toV, boufou[0], boufou[1]);
                grOb.observations.add(ob);
                trueObs--;
            }
        }
        return grOb;
    }
    
    /**
     * Make a problem Single Origin. A vertex is added and [0,0] edges are added
     * to the starting points of the observations.
     * @param grObs Graph + Observations
     * @return the same reference (mutates input!)
     */
    public GraphObs makeSO(GraphObs grObs)
    {
        // Warning, doesn't make a proper copy simply changes the handed object
        // and has been made for simulation perposes only!
        
        Vertex startSync = new Vertex(Integer.MAX_VALUE,"S");
        grObs.graph.addVertex(startSync);
        for(Observation ob : grObs.observations)
        {
            Vertex oldstart = ob.startV;
            grObs.graph.addEdge(startSync, oldstart, 0, 0);
            ob.startV = startSync;
        }
        return grObs;
    }
    
}

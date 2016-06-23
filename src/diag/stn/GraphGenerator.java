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
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
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
    
    private class BuildVertex implements Comparable<BuildVertex>
    {
        public Vertex vert;
        public int degree;

        @Override
        public int compareTo(BuildVertex t)
        {
            return Integer.compare(this.degree, t.degree);
        }
    }
    
    /**
     * Generates a new Graph (roughly) according to the Barabàsi-Albert model.
     * Applied to a directed Graph (so never more than x outgoing edges but can
     * have many incoming!)
     * @return filled Graph
     */
    public GraphObs generateBAGraph(int size, int linksPerStep, boolean onlymax, int falseObs, int trueObs, boolean zeroPoint)
    {
        if(falseObs < 0)
        {
            System.err.println("# of false observations needs to be non-negative");
            falseObs = 0; // can't use a GraphObs with no observations
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
            if(nodes % 10 == 0)
            {
                System.out.println("Nodes added: " + nodes + " edges added: " + edges);
            }
        }
        
        /**
         * First generate a graph, 
         * after which the starting and ending nodes of the observations
         * are needed for adding a possible T0. (and add this possible t0)
         * Only _then_ assign intervals to each of the edges.
         * This needs to be done in a separate step using a linear ordering
         * 
         */
        
        LinkedList<BuildVertex[]> falseO = new LinkedList(); 
        LinkedList<BuildVertex[]> trueO = new LinkedList();
        falseObsAdd:
        while(falseObs > 0)
        {
            // Since its new to old try to get a path between 2 by trialNerror
            int half = vertInfo.size() /2;
            BuildVertex fromV = vertInfo.get(half + rand.nextInt(vertInfo.size() - half));
            BuildVertex toV = vertInfo.get(rand.nextInt(half));
            
            for(BuildVertex fObs[] : falseO)
            {
                if(fObs[0].vert.equals(fromV.vert))
                {
                    if(fObs[1].vert.equals(toV.vert))
                        continue falseObsAdd; // yep, its the quickNDirty
                    // Can't have a certain obs already in use
                }
            }
            
            GraphPath startPath = new GraphPath(fromV.vert);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV.vert, gr);
            // Check if 0,0 bounds are returned indeed!! (Could go wrong)
            if(!boundsFound.isEmpty())
            {
                BuildVertex[] aFObs = new BuildVertex[2];
                aFObs[0] = fromV;
                aFObs[1] = toV;
                falseO.add(aFObs);
                falseObs--;
            }
        }
        
        trueObsAdd:
        while(trueObs > 0)
        {
            // Since its new to old try to get a path between 2 by trialNerror
            int half = vertInfo.size() /2;
            BuildVertex fromV = vertInfo.get(half + rand.nextInt(vertInfo.size() - half));
            BuildVertex toV = vertInfo.get(rand.nextInt(half));
            
            
            for(BuildVertex tObs[] : trueO)
            {
                if(tObs[0].vert.equals(fromV.vert))
                {
                    if(tObs[1].vert.equals(toV.vert))
                        continue trueObsAdd; // yep, its the quickNDirty
                    // Can't have a certain (any) obs already in use (be it
                    // either false or true)
                }
            }
            
            GraphPath startPath = new GraphPath(fromV.vert);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV.vert, gr);
            if(!boundsFound.isEmpty())
            {
                BuildVertex[] aTObs = new BuildVertex[2];
                aTObs[0] = fromV;
                aTObs[1] = toV;
                falseO.add(aTObs);
                trueObs--;
            }
        }
        
        if(zeroPoint)
        {
            Vertex startSync = new Vertex(Integer.MAX_VALUE,"S");
            gr.addVertex(startSync);
            for(BuildVertex[] ob : falseO)
            {
                Vertex oldstart = ob[0].vert;
                gr.addEdge(startSync, oldstart, 0, 0);
                ob[0].vert = startSync;
            }
            for(BuildVertex[] ob : trueO)
            {
                Vertex oldstart = ob[0].vert;
                gr.addEdge(startSync, oldstart, 0, 0);
                ob[0].vert = startSync;
            }
        }
        
        grOb.graph = initializeBounds(gr);
        // WARNING!!! Graph has been build from scratch so old Vertex refs.
        // will not work from this point on!
        
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
        
        for(BuildVertex[] falseObserv : falseO)
        {
            Vertex oStartV = grOb.graph.getVertex(falseObserv[0].vert.getID());
            Vertex oEndV = grOb.graph.getVertex(falseObserv[1].vert.getID());
            GraphPath startPath = new GraphPath(oStartV);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, oEndV, grOb.graph);
            if(boundsFound.size() > 0)
            {
                int[] boufou = combinePaths(boundsFound);
                // TODO: Do something with boufou[x] here!
                boufou[0] += 50;
                boufou[1] += 100;
                Observation ob = new Observation(oStartV, oEndV, boufou[0], boufou[1]);
                grOb.observations.add(ob);
            }
        }
        for(BuildVertex[] trueObserv : trueO)
        {
            Vertex oStartV = grOb.graph.getVertex(trueObserv[0].vert.getID());
            Vertex oEndV = grOb.graph.getVertex(trueObserv[1].vert.getID());
            GraphPath startPath = new GraphPath(oStartV);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, oEndV, grOb.graph);
            if(boundsFound.size() > 0)
            {
                int[] boufou = combinePaths(boundsFound);
                Observation ob = new Observation(oStartV, oEndV, boufou[0], boufou[1]);
                grOb.observations.add(ob);
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
            dlb = 0;
            dub = 0;
            dlb = de.getLowerb() + lb;
            dub = de.getUpperb() + ub;
            if(de.getEnd().equals(end))
            {
                g.addStep(de, de.getEnd());
                int[] lbub = new int[2];
                lbub[0] = dlb;
                lbub[1] = dub;
                pathLbUbs.add(lbub);
            }
            ArrayList<int[]> returnedLbUbs;
            if(!g.edgeUsed(de))  // shouldn't be part of current path(takes)
            {
                g.addStep(de, de.getEnd());
                returnedLbUbs = pathCalc(g,dlb,dub, end, graph);
                pathLbUbs.addAll(returnedLbUbs);
            }
            if(g.stepSize() > 1)
                g.removeLast();
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
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // TODO PROBLEM, SEE OTHER GEN !!!!
                int[] boufou = combinePaths(boundsFound);
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
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // if it is fully correct all paths should be the same... !!!!
                int[] boufou = combinePaths(boundsFound);
                Observation ob = new Observation(fromV, toV, boufou[0], boufou[1]);
                grOb.observations.add(ob);
                trueObs--;
            }
        }
        return grOb;
    }
    
    private Graph initializeSimpleBounds(Graph graphIn)
    {
        Random rand = new Random();
        
        DEdge[] allEdges = graphIn.listAllEdges();
        for(DEdge de : allEdges)
        {
            de.setUpperb(rand.nextInt(100));
        }
        
        return graphIn;
    }
    
    private Graph initializeBounds(Graph graphIn)
    {
        Random ranGen = new Random();
        
        Graph improvedGraph = new Graph();
        LinkedList<DEdge> edgeToAdd = new LinkedList(); //rebuild before adding!
        LinkedList<Vertex> vertexToAdd;
        // copy of VertexToAdd without the remove and with the new Vertices!
        
        while(graphIn.vSize() > 0)
        {
            vertexToAdd = new LinkedList(); // Always reset! should be none left
            
            Vertex[] verticesIn = graphIn.listAllVertices();
            ArrayList<BuildVertex> vertexList = new ArrayList();
            for(Vertex v :  verticesIn)
            {
                BuildVertex bv = new BuildVertex();
                bv.vert = v;
                bv.degree = graphIn.inDegree(v);
                vertexList.add(bv);
            }
            Collections.sort(vertexList);
            int lowestDegree = vertexList.get(0).degree;
            for(BuildVertex target : vertexList)
            {
                if(target.degree == lowestDegree)
                {   // These must be cut and put on the addition Queue
                    
                    // first remove the edges
                    LinkedHashSet<DEdge> edgesOut = graphIn.possibleEdges(target.vert);
                    if((edgesOut != null) && (edgesOut.size() > 0))
                    {
                        DEdge[] toRem = edgesOut.toArray(new DEdge[edgesOut.size()]);
                        for(DEdge de: toRem)
                        {
                            edgeToAdd.add(de);
                            graphIn.removeEdge(de);
                        }
                    }
                    
                    // then remove the vertex
                    boolean rem = graphIn.removeVertex(target.vert);
                    if(!rem)
                        System.out.println("Target vert not found in graph");
                                
                    vertexToAdd.add(target.vert);
                }
            }
            
            // Now add new Vertices to the output Graph
            for(Vertex addV : vertexToAdd)
            {
                Vertex imprV = new Vertex(addV.getID(), addV.getName());
                improvedGraph.addVertex(imprV);
                
                // System.out.println("Adding vert: " + imprV.getName());
                // After adding a vertex see which edges need to go to thatthere
                // vertex!
                ArrayList<DEdge> toThisNewV = new ArrayList();
                for(DEdge addE : edgeToAdd)
                {
                    if(addE.getEnd().equals(addV))
                        toThisNewV.add(addE);
                }
                //Vertex[] fromsToNewV = new Vertex[toThisNewV.size()];
                ArrayList<Vertex> tempFTNV = new ArrayList();
                for(int i = 0; i < toThisNewV.size() ; i++)
                {
                    Vertex old = toThisNewV.get(i).getStart();
                    Vertex newv = improvedGraph.getVertex(old.getID());
                    if(newv != null)
                        tempFTNV.add(newv);
                    // fromsToNewV[i] = improvedGraph.getVertex(old.getID());
                }
                Vertex[] fromsToNewV = tempFTNV.toArray(new Vertex[tempFTNV.size()]);
                ArrayList<int[]> bounds = commonAncest(fromsToNewV, improvedGraph);
                
                // Add the edges according to bounds as well
                for(int j = 0; j < fromsToNewV.length; j++)
                {
                    int[] bound = bounds.get(j);
                    if((bound[0] == 0) && (bound[1] == 0))
                    {
                        // can be any bound
                        int lb = ranGen.nextInt(40);
                        int ub = (lb + ranGen.nextInt(20));
                        improvedGraph.addEdge(fromsToNewV[j], imprV, lb, ub);
                        // System.out.print("Adding edge: " + fromsToNewV[j].getName()
                        // + " -[" + lb + "," + ub + "]-> " + imprV.getName() + "\n");
                    }
                    else
                    {
                        if((bound[0] >= 0 && bound[1] >= 0)&&(bound[0] <= bound[1]))
                        {
                            improvedGraph.addEdge(fromsToNewV[j], imprV, bound[0], bound[1]);
                            // System.out.print("Adding edge: " + fromsToNewV[j].getName()
                            // + " -b[" + bound[0] + "," + bound[1] + "]-> " + imprV.getName() + "\n");
                        }
                        else
                        {
                            // System.out.print("Skipping edge: " + fromsToNewV[j].getName()
                            // + " -b[" + bound[0] + "," + bound[1] + "]-> " + imprV.getName() + "\n");
                        }
                    }
                }
            }
            /**
             * Vertex & edges are removed and added now (or in the case of edges,
             * placed on the stack)
             */
        }
        return improvedGraph;
    }
    
    /** 
     * Checks if multiple from-Vertices to a certain new Vertex have
     * a common ancestor and if so will return the needed edge weights
     * (make sure that both paths have the same 
     * @param froms
     * @return 
     */
    private ArrayList<int[]> commonAncest(Vertex[] froms, Graph g)
    {
        ArrayList<int[]> boundsPerFrom = new ArrayList();
        for(Vertex someV : froms)
        {
            int[] someAr = new int[2];
            someAr[0] = 0;
            someAr[1] = 0;
            boundsPerFrom.add(someAr);
        }
        
        Vertex[][] ancstrz = new Vertex[froms.length][]; // all ancestors per Vertex
        LinkedList<CommonStruct> allCommon = new LinkedList(); //
        
        for(int k = 0; k < froms.length; k++)
            ancstrz[k] = ancest(froms[k], g).toArray(new Vertex[0]); 
            /**
             * At the moment ancest has only the guarantee that in a path
             * the deepest/farthest Vertex is first but when branching paths
             * this does not have to be the case (This could possibly lead 
             * to a problem -> see 22 June notes)
             */
            
        for(int i = 0; i < froms.length; i++)
        {
            for(int j = i + 1; j < froms.length; j++)
            {
                // for each pair check for common ancestor
                for(Vertex vert: ancstrz[i])
                {
                    for(Vertex pert: ancstrz[j])
                    {
                        if(vert.equals(pert))
                        {
                            // yes common ancest
                            // ONLY ADD IF IT WASNT ADDED YET!!!
                            
                            // TODO:Check if the order from Vertex[0] is correct 
                            // ie. is multiple ancestor, does it pick the latest ?
                            boolean addA = true;
                            boolean addB = true;
                            for(CommonStruct cs: allCommon)
                            {
                                if(froms[i].equals(cs.fromV))
                                {
                                    if(cs.common.equals(vert))
                                    {
                                        addA = false;
                                        break;
                                    }
                                }
                            }
                            if(addA)
                            {
                                CommonStruct strA = new CommonStruct();
                                strA.common = vert;
                                strA.fromV = froms[i];
                                allCommon.add(strA);
                            }
                            for(CommonStruct cs: allCommon)
                            {
                                if(froms[j].equals(cs.fromV))
                                {
                                    if(cs.common.equals(vert))
                                    {
                                        addB = false;
                                        break;
                                    }
                                }
                            }
                            if(addB)
                            {
                                CommonStruct strB = new CommonStruct();
                                strB.common = vert;
                                strB.fromV = froms[j];
                                allCommon.add(strB);
                            }
                        }
                    }
                }
            }
        }
        if(allCommon.size() < 1)
            return boundsPerFrom;
        
        // First create groups with common ancestors
        LinkedList<CommonGroup> groupsWComAnc = new LinkedList();
        while(allCommon.size() > 1)
        {
            CommonGroup newGroup = new CommonGroup();
            newGroup.bpf = new ArrayList();
            for(Vertex vf : froms)
            {
                int[] foo = new int[2];
                foo[0] = 0;
                foo[1] = 0;
                newGroup.bpf.add(foo);
            }
            newGroup.csWComAnc = new LinkedList();
            newGroup.csWComAnc.add(allCommon.getFirst());
            for(CommonStruct cs : allCommon)
            {
                if(cs.common.equals(newGroup.csWComAnc.get(0).common))
                {
                    newGroup.csWComAnc.add(cs);
                }
            }
            // now delete them
            for(CommonStruct delcs : newGroup.csWComAnc)
            {
                for(CommonStruct posdel : allCommon)
                {
                    if(delcs.equals(posdel))
                    {
                        allCommon.remove(delcs);
                        break;
                    }
                }
            }
            // and add the new group to all groups
            groupsWComAnc.add(newGroup);
        }
        
        // now for each group populate its BoundsPerFrom
        for(CommonGroup cg : groupsWComAnc)
        {
            int highestlb = Integer.MIN_VALUE;
            int highestub = Integer.MIN_VALUE;
            for(CommonStruct cas : cg.csWComAnc)
            {
                int[] boufou = {0,0};
                GraphPath startPath = new GraphPath(cas.common);
                if(cas.common.equals(cas.fromV)) 
                {   
                    // This fromV is common with another fromV ie. is the ancest
                    // of another node
                    cas.lb = 0;
                    cas.ub = 0;
                }
                else
                {
                    ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, cas.fromV, g);
                    boufou = combinePaths(boundsFound); // Should be ok since it has been found
                    cas.lb = boufou[0];
                    cas.ub = boufou[1];
                }
                
                if(cas.lb > highestlb)
                    highestlb = cas.lb;
                if(cas.ub > highestub)
                    highestub = cas.ub;
                if(highestub < highestlb)
                    highestub = highestlb;
            }
            // now set the outputs for the edges as per notes highestlb + 10, highestub + 20
            for(CommonStruct caso : cg.csWComAnc)
            {
                caso.edgeLb = (highestlb + 10) - caso.lb;
                caso.edgeUb = (highestub + 20) - caso.ub;
                
                // Finally put them in the right position in the output array
                for(int p = 0; p < froms.length; p++)
                {
                    if(froms[p].equals(caso.fromV))
                    {
                        int[] bounds = cg.bpf.get(p);
                        bounds[0] = caso.edgeLb;
                        bounds[1] = caso.edgeUb;
                    }
                }
            }
        }
        // finally combine the boundsPerFrom from different ancestor groups to
        // a single output array 
        for(int l = 0; l < boundsPerFrom.size(); l++)
        {
            int[] tempFinal;
            ArrayList<int[]> toCombine = new ArrayList();
            for(CommonGroup cg : groupsWComAnc)
            {
                if(cg.bpf.get(l)[0] > 0)
                    toCombine.add(cg.bpf.get(l));
            }
            if(toCombine.size() > 1)
            {
                // Takes the Intersection
                int[] finalbounds = toCombine.remove(toCombine.size()-1); // take last
                for(int[] p: toCombine)
                {
                    if(p[0] > finalbounds[0])
                        finalbounds[0] = p[0];
                    if(p[1] < finalbounds[1])
                        finalbounds[1] = p[1];
                }
                if(finalbounds[0] < finalbounds[1])
                {
                    // lucky! they can be combined
                    tempFinal = boundsPerFrom.get(l);
                    tempFinal[0] = finalbounds[0];
                    tempFinal[1] = finalbounds[1];
                }
                else
                {
                    // do NOT add this edge
                    tempFinal = boundsPerFrom.get(l);
                    tempFinal[0] = -10;
                    tempFinal[1] = -15;
                }
            }
            else if(toCombine.size() == 1)
            {
                tempFinal = boundsPerFrom.get(l);
                tempFinal[0] = toCombine.get(0)[0];
                tempFinal[1] = toCombine.get(0)[1];
            }
            // else no change, simply {0,0} will do!
        }
        
        return boundsPerFrom;
    }
    
    /**
     * Returns a list of ancestor Vertices of a vertex in a given Graph.
     * In a path, the deepest ancestor is first.
     * @param vx
     * @param graaf
     * @return ArrayList with Vertex objects.
     */
    private ArrayList<Vertex> ancest(Vertex vx, Graph graaf)
    {
        ArrayList<Vertex> ancesti = new ArrayList();
        LinkedList<Vertex> directAncest = graaf.incomingNodes(vx);
        for(Vertex inc : directAncest) 
        {   
            // als incoming empty is returned hij ook empty set
            ancesti.addAll(ancest(inc, graaf));
        }
        ancesti.add(vx);
        return ancesti;
    }
    
    private class CommonStruct
    {
        public Vertex common;
        public Vertex fromV;
        public int lb;
        public int ub;
        public int edgeLb;
        public int edgeUb;
    }
    
    private class CommonGroup
    {
        private LinkedList<CommonStruct> csWComAnc;
        ArrayList<int[]> bpf;
    }
    
    public void testAncest()
    {
        Graph test = new Graph();
        Vertex a = new Vertex(1, "a");
        Vertex b = new Vertex(2, "b");
        Vertex c = new Vertex(3, "c");
        Vertex d = new Vertex(4, "d");
        Vertex e = new Vertex(5, "e");
        Vertex f = new Vertex(6, "f");
        Vertex g = new Vertex(7, "g");
        Vertex h = new Vertex(8, "h");
        Vertex i = new Vertex(9, "i");
        Vertex j = new Vertex(10, "j");
        Vertex k = new Vertex(11, "k");
        Vertex l = new Vertex(12, "l");
        Vertex m = new Vertex(13, "m");
        Vertex n = new Vertex(14, "n");
        Vertex o = new Vertex(15,"o");
        test.addVertex(a);
        test.addVertex(b);
        test.addVertex(c);
        test.addVertex(d);
        test.addVertex(e);
        test.addVertex(f);
        test.addVertex(g);
        test.addVertex(h);
        test.addVertex(i);
        test.addVertex(j);
        test.addVertex(k);
        test.addVertex(l);
        test.addVertex(m);
        test.addVertex(n);
        test.addVertex(o);
        test.addEdge(a, b, 10, 20);
        test.addEdge(b, c, 10, 20);
        test.addEdge(a, d, 5, 10);
        test.addEdge(d, e, 5, 10);
        test.addEdge(g, j, 1, 2);
        test.addEdge(j, k, 1, 2);
        test.addEdge(g, h, 5, 6);
        test.addEdge(h, i, 5, 6);
        test.addEdge(l, m, 100, 200);
        test.addEdge(m, n, 100, 200);
        test.addEdge(o,a, 500,510); // added this line to check mutliple ancestors
        
        Vertex[] fromVertices = {e, c, i, k, n};
        ArrayList<int[]> out = commonAncest(fromVertices, test);
        System.out.print("Vertex e should have output 20 - 40, has :");
        System.out.print(out.get(0)[0] + " - " + out.get(0)[1] + "\n");
        System.out.print("Vertex c should have output 10 - 20, has :");
        System.out.print(out.get(1)[0] + " - " + out.get(1)[1] + "\n");
        System.out.print("Vertex i should have output 10 - 20, has :");
        System.out.print(out.get(2)[0] + " - " + out.get(2)[1] + "\n");
        System.out.print("Vertex k should have output 18 - 28, has :");
        System.out.print(out.get(3)[0] + " - " + out.get(3)[1] + "\n");
        System.out.print("Vertex k should have output 0 - 0, has :");
        System.out.print(out.get(4)[0] + " - " + out.get(4)[1] + "\n");
    }
    
    public Graph testInit()
    {
        Graph test = new Graph();
        Vertex a = new Vertex(1, "a");
        Vertex b = new Vertex(2, "b");
        Vertex c = new Vertex(3, "c");
        Vertex d = new Vertex(4, "d");
        Vertex e = new Vertex(5, "e");
        Vertex f = new Vertex(6, "f");
        Vertex g = new Vertex(7, "g");
        Vertex h = new Vertex(8, "h");
        Vertex i = new Vertex(9, "i");
        Vertex j = new Vertex(10, "j");
        Vertex k = new Vertex(11, "k");
        Vertex l = new Vertex(12, "l");
        Vertex m = new Vertex(13, "m");
        Vertex n = new Vertex(14, "n");
        Vertex o = new Vertex(15,"o");
        test.addVertex(a);
        test.addVertex(b);
        test.addVertex(c);
        test.addVertex(d);
        test.addVertex(e);
        test.addVertex(f);
        test.addVertex(g);
        test.addVertex(h);
        test.addVertex(i);
        test.addVertex(j);
        test.addVertex(k);
        test.addVertex(l);
        test.addVertex(m);
        test.addVertex(n);
        test.addVertex(o);
        test.addEdge(a, b, 0, 0);
        test.addEdge(b, c, 0, 0);
        test.addEdge(a, d, 0, 0);
        test.addEdge(d, e, 0, 0);
        test.addEdge(g, j, 0, 0);
        test.addEdge(j, k, 0, 0);
        test.addEdge(g, h, 0, 0);
        test.addEdge(h, i, 0, 0);
        test.addEdge(l, m, 0, 0);
        test.addEdge(m, n, 0, 0);
        test.addEdge(o,a, 0,0); 
        test.addEdge(e, f, 0, 0);
        test.addEdge(c, f, 0, 0);
        test.addEdge(i, f, 0, 0);
        test.addEdge(k, f, 0, 0);
        test.addEdge(n, f, 0, 0);
        
        Graph outp = initializeBounds(test);
        return outp;
    }
    
}

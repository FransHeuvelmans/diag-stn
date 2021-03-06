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
     * Struct like combination of the Graph, its Observations and attached 
     * errors. This is used to check if diagnosis actually finds the introduced 
     * faults
     */
    public class GraphObs
    {
        public Graph graph;
        public List<Observation> observations;
        public List<DEdge> errorEdges; // per obs
        public List<Integer> errorDiffs; // per edge
        
        public GraphGenSettings settings; // Settings used to generate Problem
        public boolean success;
    }
    
    /**
     * Struct used to attach a degree to a vertex (and to be able to compare
     * build-vertices)
     */
    class BuildVertex implements Comparable<BuildVertex>
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
     * @param size Number of vertices
     * @param linksPerStep when adding a vertex how many edges need to be added
     * @param onlymax Always try to add max edges or a random amount
     * @param observations # false obs
     * @param obsLength Length of the observations added
     * @param diff Percentage in int (ie. 50% = 50) that will be added (or
     * subtracted when negative) of the observation (path) prediction
     * @param zeroPoint Add a T0
     * @return filled Graph
     */
    public GraphObs generateBAGraph(int size, int linksPerStep, boolean onlymax, 
            int observations, int obsLength, int diff, boolean zeroPoint)
    {
        if(observations < 1)
        {
            System.err.println("# of false observations needs to be positive");
            observations = 1; // can't use a GraphObs with no observations
        }
        
        Random rand = new Random();
        GraphObs grOb = new GraphObs();
        vertInfo = new ArrayList();
        
        int oriObservations = observations;
        
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
            /*if(nodes % 10 == 0)
            {
                System.out.println("Nodes added: " + nodes + " edges added: " + edges);
            }*/
        }
        
        // Store settings
        GraphGenSettings gs = new GraphGenSettings();
        gs.BAGraph(size, linksPerStep, onlymax, oriObservations, obsLength, diff, 
                zeroPoint);
        
        // Then the errors are generated (again without bounds) randomly and
        // if a T0 is needed, then this is added
        FalsieGroup fgroup = generateErrors(gr, gs);
                
        // Check if there are errors to diagnose (check because of bruteforce
        // random add)
        if(fgroup.intendedEs.size() < 1)
        {
            /* If it wasnt possible to add any obs then maybe it's time to try 
             * adding smaller length observations */
            return generateBAGraph(size,linksPerStep,onlymax,oriObservations,
                    --obsLength,diff,zeroPoint);
        }
        
        // Only then with the final Graph can the edges be initialized
        // WARNING: Edges might dissapear if they cant be added during 
        // initialization which could in turn break obs so it needs to be tested
        grOb.graph = initializeBounds(gr);
        
        // WARNING!!! Graph has been build from scratch so old Vertex refs.
        // will not work from this point on!
        
        // Using the general position of the generated (Falsie) errors, and an
        // initialized graph; the obs can also be initialized. Needs quite a few
        // checks to see if the original path is still there and the bounds of
        // the errors are consistent
        boolean suc = initializeErrors(grOb, fgroup, gs);
        
        grOb.success = suc;
        grOb.settings = gs;
        // TODO: Make sure this is not some nested function because it saves al
        if(suc)
        {
            boolean chck = checkConsist(grOb);
            grOb.success = chck;
        }
        if(grOb.observations.size() < oriObservations)
            grOb.success = false;
        return grOb;
    }
    
    /**
     * Generate a graph according to Barabasi–Albert model. Wrapper method
     * for using Settings objects
     * @param gs GraphGenSettings object containing all the settings normally
     * used with generateBAGraph()
     * @return Object with the Graph and the observations
     */
    public GraphObs generateBAGraph(GraphGenSettings gs)
    {
        if(gs.type != GraphGenSettings.BAGRAPH)
            System.err.println("Not correct (BA) type of settings");
        return generateBAGraph(gs.vertexSize,gs.BALinksPerVertexAddition,
                gs.onlyMaxAdditions, gs.numObservations, gs.observationLength,
                gs.difference, gs.timeSyncT0);
    }
    
    /**
     * Add a vertex according to Barabasi–Albert model
     * @param g Graph object to which the vertex needs to be added
     * @param links Max of edges to connect the new vertex with
     * @param onlymax Must find max edges or between (1,Max)
     */
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
     * Returns a list of predicted bounds on a path. Uses simple DFS.
     * @param g Traversed path (Start with only starting vert)
     * @param lb Lower bound on time already traversed
     * @param ub Upper bound on time already traversed
     * @param end End vertex
     * @param graph Base graph which is traversed
     * @return list with int[2] = {lower bound, upper bound}
     */
    public static ArrayList<int[]> pathCalc(GraphPath g, int lb, int ub, Vertex end, 
            Graph graph)
    {
        //g.smallPrint();
        int dlb,dub;
        ArrayList<int[]> pathLbUbs = new ArrayList<>();
        // combine generatePaths & propagateWeights
        LinkedHashSet<DEdge> edgeExp = graph.possibleEdges(g.getLastV());
        
        if(edgeExp == null || (edgeExp.size() < 1))
            return pathLbUbs; // dead end!
        for(DEdge de : edgeExp)
        {
            dlb = de.getLowerb() + lb;
            dub = de.getUpperb() + ub;
            if(de.getEnd().equals(end))
            {
                g.addStep(de, de.getEnd());
                int[] lbub = new int[2];
                lbub[0] = dlb;
                lbub[1] = dub;
                pathLbUbs.add(lbub);
                g.removeLast();
            }
            ArrayList<int[]> returnedLbUbs;
            if(!g.edgeUsed(de))  // shouldn't be part of current path(takes)
            {
                g.addStep(de, de.getEnd());
                returnedLbUbs = pathCalc(g,dlb,dub, end, graph);
                pathLbUbs.addAll(returnedLbUbs);
                g.removeLast();
            }
        }
        
        return pathLbUbs; 
    }
    
    /**
     * Similar to pathCalc but returns all the paths for some pair of vertices 
     * instead of the bounds
     * @param g Traveled path (start with path containing only starting pos)
     * @param end End vertex
     * @param graph Graph object which is being traversed 
     * @return List with all the paths as GraphPath objects
     */
    public static ArrayList<GraphPath> obsPaths(GraphPath g, Vertex end, Graph graph)
    {
        int dlb,dub;
        ArrayList<GraphPath> paths = new ArrayList<>();
        // combine generatePaths & propagateWeights
        LinkedHashSet<DEdge> edgeExp = graph.possibleEdges(g.getLastV());
        
         if(edgeExp == null || (edgeExp.size() < 1))
            return paths; // dead end!
        for(DEdge de : edgeExp)
        {
            if(de.getEnd().equals(end))
            {
                g.addStep(de, de.getEnd());
                paths.add(g.copy());
                g.removeLast();
            }
            ArrayList<GraphPath> returnedPaths;
            if(!g.edgeUsed(de))  // shouldn't be part of current path(takes)
            {
                g.addStep(de, de.getEnd());
                returnedPaths = obsPaths(g, end, graph);
                paths.addAll(returnedPaths);
                g.removeLast();
            }
        }
        
        return paths;
    }
    
    /**
     * Quick method for propagating the weights along a graph path
     * @param gp GraphPath object with the path that needs propagation.
     * @return int array with [0] lower bound & [1] upper bound
     */
    public int[] propPath(GraphPath gp)
    {
        int lb = 0, ub = 0;
        for(int i=1; i < gp.stepSize(); i++)
        {
            DEdge de = gp.getStepE(i);
            lb += de.getLowerb();
            ub += de.getUpperb();
        }
        int[] ans = {lb, ub};
        return ans;
    }
    
    /**
     * Method for taking the intersection of all the bounds on a path
     */
    static int[] combinePaths(ArrayList<int[]> paths)
    {
        if(paths.size() < 1)
            System.err.println(" Cant combine empty arraylist!");
        
        // Takes the Intersection
        int[] finalbounds = paths.remove(paths.size()-1); // take last
        for(int[] p: paths)
        {
            if(p[0] > finalbounds[0])
                finalbounds[0] = p[0];
            if(p[1] < finalbounds[1])
                finalbounds[1] = p[1];
        }
        if((finalbounds[0] > finalbounds[1]) && DiagSTN.PRINTWARNING)
            System.out.println("No intersection found for combine paths");
        return finalbounds;
    }
    
    /**
     * Create a Graph that resembles multiple plans that have a few connections
     * @param line number of plans coming together
     * @param linelb lowerbound on plansize
     * @param lineub upperbound on plansize
     * @param maxLineCon how much the plans are interconnected 
     * @param maxVertCon how much 2 plans can be interconnected
     * @param observations # false observations 
     * @param obsLength Length of the observations added (1 path with edge size)
     * @param diff Percentage in int (ie. 50% = 50) that will be added (or
     * subtracted when negative) of the observation (path) prediction
     * @param zeroPoint Add a time synchronization point  
     * @return Object with all problem info
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
        // no vertInfo needed
        id = 0;
        numEdges = 0;
        Graph gr = new Graph();        
        LinkedList<Vertex>[] lines = new LinkedList[line];
        
        int randNod = lineub - linelb;
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
                
                gr.addEdge(nextV, prevV, 0, 0); // WARNING!! from next to prev again!
                
                // onto the next
                prevV = nextV;
                lineVertices--;
            }
            /*System.out.print("line " + (line-1) + ":");
            for(Vertex vprint : lines[line-1])
            {
                //System.out.print(vprint.getName() + "<-");
            }
            //System.out.print("\n");*/
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
                //System.out.print("Connecting :" + l + "," + linePick +":");
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
                    
                    lb = 0; // Must be set properly after creation!
                    ub = rand.nextInt(30);
                    gr.addEdge(conLine.get(vertStart),
                            (lines[linePick].get(vertEnd)), lb, ub);
                    //System.out.print(conLine.get(vertStart).getName() + "," + lines[linePick].get(vertEnd).getName());
                    //System.out.print(" & ");
                    vertConnect--;
                }
                //System.out.print("\n");
                lineConnects--;
            }
        }
        
        // Store settings
        GraphGenSettings gs = new GraphGenSettings();
        gs.planlikeGraph(orLine, linelb, lineub, maxLineCon, maxVertCon, orObs, 
                obsLength, diff, zeroPoint);
        
        // Then the errors are generated (again without bounds) randomly and
        // if a T0 is needed, then this is added
        FalsieGroup fgroup = generateErrors(gr, gs);
                
        // Check if there are errors to diagnose (check because of bruteforce
        // random add)
        if(fgroup.intendedEs.size() < 1)
        {
            /* If it wasnt possible to add any obs then maybe it's time to try 
             * adding smaller length observations */
            gs.observationLength--;
            return generateBAGraph(gs);
        }
        
        // init the graph with the final Vertices
        grOb.graph = initializeBounds(gr);
        
        // WARNING!!! Graph has been build from scratch so old Vertex refs.
        // will not work from this point on!
        
        // Using the general position of the generated (Falsie) errors, and an
        // initialized graph; the obs can also be initialized. Needs quite a few
        // checks to see if the original path is still there and the bounds of
        // the errors are consistent
        boolean suc = initializeErrors(grOb, fgroup, gs);
        
        grOb.success = suc;
        grOb.settings = gs;
        // TODO: Make sure this is not some nested function because it saves al
        if(suc)
        {
            boolean chck = checkConsist(grOb);
            grOb.success = chck;
        }
        if(grOb.observations.size() < orObs)
            grOb.success = false;
        
        /**
         * Much of this code is for now the same as BA-Gen code. Its separate 
         * because of previous versions. As TODO: make it neatly 1 method with 
         * multiple network generation sub methods to generate the gr Graph obj 
         */
        
        return grOb;
    }
    
    /**
     * Create a Graph that resembles multiple plans that have a few connections.
     * Wrapper method to use Setttings objects.
     * @param gs GraphGenSettingsobject containing all the settings
     * @return Problem with a graph and some observations
     */
    public GraphObs generatePlanlikeGraph(GraphGenSettings gs)
    {
        if(gs.type != GraphGenSettings.PLANLIKEGRAPH)
            System.err.println("Not correct (planlike) type of settings");
        return generatePlanlikeGraph(gs.numLines, gs.lineLengthLB, gs.lineLengthUB,
                gs.maxInterLineConnect, gs.maxLineVertConnect, 
                gs.numObservations, gs.observationLength, gs.difference, 
                gs.timeSyncT0);
    }
    
    // --- A whole bunch of Graph Initialization from this point ---
    
    /** 
     * Quick and dirty method to add bounds between 0 and max 100. For proper
     * testing used normal initializeBounds
     * @param graphIn
     * @return 
     */
    private Graph initializeSimpleBounds(Graph graphIn)
    {
        Random rand = new Random();
        
        DEdge[] allEdges = graphIn.listAllEdges();
        for(DEdge de : allEdges)
        {
            de.setLowerb(0);
            de.setUpperb(rand.nextInt(100));
        }
        
        return graphIn;
    }
    
    /**
     * Takes a graph with structure (edges) and sets the bounds for these edges
     * randomly. It will keep the network consistent but in some cases when no
     * consistent network is possible, an edge is omitted.
     * @param graphIn
     * @return Initialized Graph object
     */
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
                
                //Create array with the the vertices where the new vertex will
                //connect (from) - with the new references (so in the new graph)
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
                    if(improvedGraph.directReach(fromsToNewV[j], imprV))
                    {
                        // double edge ?? shouldnt be possible!
                        continue;
                    }
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
            int numComGroups = 0;
            for(CommonGroup cg : groupsWComAnc)
            {
                if(cg.bpf.get(l)[0] > 0)
                {
                    toCombine.add(cg.bpf.get(l));
                    numComGroups++;
                }
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
    
    /**
     * Returns a list of all descending vertices of some vertex in a given
     * Graph
     * @param vx
     * @param graaf
     * @return ArrayList with Vertex objects
     */
    private ArrayList<Vertex> descendant(Vertex vx, Graph graaf)
    {
        ArrayList<Vertex> descent = new ArrayList();
        LinkedList<Vertex> directDescent = graaf.adjacentNodes(vx);
        for(Vertex inc : directDescent)
        {
            descent.addAll(descendant(inc, graaf));
        }
        descent.add(vx);
        return descent;
    }
    
    /**
     * A struct to store info about a fromV 
     */
    private class CommonStruct
    {
        public Vertex common;
        public Vertex fromV;
        public int lb;
        public int ub;
        public int edgeLb;
        public int edgeUb;
    }
    
    /**
     * Struct that groups together multiple commonStruct for those with common 
     * ancestor (and shows the bounds that need to be taken into consideration 
     * in order to create the new edges to the new vertex)
     */
    private class CommonGroup
    {
        private LinkedList<CommonStruct> csWComAnc;
        ArrayList<int[]> bpf;
    }
    
    /**
     * Generates the errors for an uninitialized graph and adds a zero time 
     * point if it is needed. These errors are not initialized because the graph
     * might not be.
     * @param gr Graph object (not initialized)
     * @param gs Settings for Graph Generation
     * @return 
     */
    private FalsieGroup generateErrors(Graph gr, GraphGenSettings gs)
    {
        Random rand = new Random();
        Vertex startSync = null;
        if(gs.timeSyncT0)
        {
            startSync = new Vertex(Integer.MAX_VALUE,"S");
            gr.addVertex(startSync);
        }
        
        LinkedList<Falsie> falseIntentions = new LinkedList();
        LinkedList<DEdge> doNotUse = new LinkedList();
        // Do not use edges which are path of the obs-path of another fault
        // (so some obs will not have 2 faults on 1 path!)
        LinkedList<DEdge> faultyEdges = new LinkedList();
        // These are the precise faults
        
        int observations = gs.numObservations;
        int obsLength = gs.observationLength;
        
        int trys = 200000;
        // Try to add observations of a certain length but not sure if its
        // possible 
        int currentLen;
        
        addObservations:
        while(observations > 0 && trys > 0)
        {
            boolean zeroEdgeAdded = false; 
            DEdge syncEdge = null;
            // did this obs add a zeroPoint edge ? If so and the obs can't be
            // created then the edge needs to be removed!
            
            currentLen = obsLength;
            DEdge misbehave = gr.randomEdge();
            if(doNotUse.contains(misbehave))
                continue;
            Vertex end = misbehave.getEnd();
            Vertex start = misbehave.getStart();
            LinkedList<DEdge> path = new LinkedList();
            path.add(misbehave);
            currentLen--;
            // First try to add edges to the end of the obs path
            while(currentLen > 0)
            {
                LinkedList<Vertex> possibilities = gr.adjacentNodes(end);
                if(possibilities == null || possibilities.size() < 1)
                    break;
                Vertex newEnd = possibilities.get(rand.nextInt(possibilities.size()));
                DEdge de = gr.getDirectEdge(end, newEnd);
                if(faultyEdges.contains(de))
                {
                    HashSet<DEdge> possibleEdges = gr.possibleEdges(end);
                    for(DEdge eddie : possibleEdges)
                    {
                        if(faultyEdges.contains(eddie))
                            possibilities.remove(eddie.getEnd());
                    }
                    if(possibilities.size() < 1)
                        break;
                    else
                        continue; // try another path!
                }
                path.add(de);
                end = newEnd;
                currentLen--;
            }
            // tried as much as possible, time to add to the front
            while(currentLen > 0)
            {
                LinkedList<Vertex> possibilities = gr.incomingNodes(start);
                if(possibilities == null || possibilities.size() < 1)
                    break;
                
                Vertex newStart = possibilities.get(rand.nextInt(possibilities.size()));
                DEdge de = gr.getDirectEdge(newStart, start);
                if(faultyEdges.contains(de))
                {
                    HashSet<DEdge> possibleEdges = gr.incomingEdges(start);
                    for(DEdge eddie : possibleEdges)
                    {
                        if(faultyEdges.contains(eddie))
                            possibilities.remove(eddie.getStart()); 
                    }
                    if(possibilities.size() < 1)
                        break;
                    else
                        continue; // try another path!
                }
                path.push(de);
                start = newStart;
                currentLen--;
            }
            // If there is a zeroPoint, this needs to be added
            if(gs.timeSyncT0 && !start.equals(startSync))
            {
                // a small check if the first vertex is already the timeSync!
                
                // Remember to remove the edge if it was added this iteration
                syncEdge = null;
                // check if edge already exists
                if(!gr.directReach(startSync, start))
                {
                    // Add that edge!
                    gr.addEdge(startSync, start, 0, 0);
                    syncEdge = gr.getDirectEdge(startSync, start);
                    zeroEdgeAdded = true;
                }
                else
                {
                    // Check if the edge can be used (only a problem if it already
                    // existed
                    syncEdge = gr.getDirectEdge(startSync, start);
                    if(faultyEdges.contains(syncEdge))
                    {
                        trys--;
                        continue addObservations;
                    }
                }
                // lets add it to the path
                path.push(syncEdge);
                start = startSync;
            }
            // now lets see if we even have a proper obs
            if(currentLen < 1)
            {
                // First check if any of the paths for this observation will have 
                // more than 1 fault
                GraphPath st = new GraphPath(start);
                ArrayList<GraphPath> paths = obsPaths(st, end, gr);
                for(GraphPath gp : paths)
                {
                    int fault = 0;
                    boolean hasEdgMisbehave = false;
                    DEdge[] edges = gp.toEdges();
                    for(DEdge de : edges)
                    {
                        if(de.equals(misbehave))
                        {
                            fault++;
                            hasEdgMisbehave = true;
                        }
                        for(DEdge flsi : faultyEdges)
                        {
                            if(de.equals(flsi))
                                fault++;
                        }
                    }
                    if(fault > 1)
                    {
                        if(zeroEdgeAdded)
                            gr.removeEdge(syncEdge);
                        trys--;
                        continue addObservations;
                        // Dont add some observation if there are multiple faults
                        // on one of its paths!
                    }
                    else if(hasEdgMisbehave && (edges.length != path.size()))
                    {
                        if(zeroEdgeAdded)
                            gr.removeEdge(syncEdge);
                        // if it uses the faulty edge but has a different length
                        // then strong probability answer is inconsistent
                        trys--;
                        continue addObservations;
                    }
                }
                // best if we check for the other Falsies if they dont have double
                // faults as well
                for(Falsie gdFalse: falseIntentions)
                {
                    GraphPath flStart = new GraphPath(gdFalse.falSE[0]);
                    ArrayList<GraphPath> flPaths = obsPaths(flStart, gdFalse.falSE[1], gr);
                    for(GraphPath gp : flPaths)
                    {
                        int fault = 0;
                        DEdge[] edges = gp.toEdges();
                        for(DEdge de : edges)
                        {
                            if(de.equals(misbehave))
                            {
                                fault++;
                            }
                            for(DEdge flsi : faultyEdges)
                            {
                                if(de.equals(flsi))
                                    fault++;
                            }
                        }
                        if(fault > 1)
                        {
                            if(zeroEdgeAdded)
                                gr.removeEdge(syncEdge);
                            trys--;
                            continue addObservations;
                            // The addition of the extra misbehave edge made 
                            // an old Falsie path have 2+ errors so skip
                        }
                    }
                    
                    // and check if they have similar start and end points
                    // (I want different Obs!)
                    if(gdFalse.falSE[0].equals(start) &&
                            gdFalse.falSE[1].equals(end))
                    {
                        if(zeroEdgeAdded)
                            gr.removeEdge(syncEdge);
                        trys--;
                        continue addObservations;
                    }
                }
                
                
                // its good observation!
                Falsie f = new Falsie();
                Vertex[] newObs = new Vertex[2];
                newObs[0] = start;
                newObs[1] = end;
                f.falSE = newObs;
                f.falseE = misbehave;
                f.falsePath = path.toArray(new DEdge[path.size()]);
                f.otherEs = new ArrayList();
                falseIntentions.add(f);
                // Always add the edge -> means obs and edge are connected
                
                // Any edge used on this obs-path may not be used in some other
                // obs-path to make sure no inconsistent observations are 
                // generated (or atleast no obs-path with 2 faults!)
                for(DEdge de : f.falsePath)
                {
                    if(!doNotUse.contains(de))
                        doNotUse.add(de);   // also adds misbehave!
                }
                faultyEdges.add(misbehave);
                    
                
                // Assume that each other path of the observation can have an 
                // error as well
                for(GraphPath gp : paths)
                {
                    DEdge[] pathEdges = gp.toEdges();
                    boolean hasError = false;
                    for(DEdge pathEdg : pathEdges)
                    {
                        // See if this particular path already has an error
                        if(faultyEdges.contains(pathEdg))
                        {
                            hasError = true;
                            break;
                        }
                    }
                    // if there is not already some error in the path, some
                    // edge must be used as possible faulty edge (in case the
                    // path ends up with the wrong prediction)
                    if(!hasError)
                    {
                        for(int iter = pathEdges.length - 1; iter >= 0; iter--)
                        {
                            if(!doNotUse.contains(pathEdges[iter]))
                            {
                                // We appoint this as a possible faulty edge
                                faultyEdges.add(pathEdges[iter]);
                                f.otherEs.add(pathEdges[iter]);
                                hasError = true;
                                break;  // Only need to add 1 error / path
                            }
                        }
                    }
                    // if it still has no error than for 1 of the (other) paths
                    // of this obs it was not possible to use some error!
                    if(!hasError)
                    {
                        if(DiagSTN.PRINTWARNING)
                            System.out.println("Skipping obs because of "
                                    + "impossible side-path");
                        // try to revert the damage done (isnt possible for the
                        // doNotUse register)
                        falseIntentions.remove(f);
                        faultyEdges.remove(misbehave);
                        trys--;
                        if(zeroEdgeAdded)
                            gr.removeEdge(syncEdge);
                        continue addObservations;
                    }
                    
                    // finally add all edges to doNotUse (ie. dont touch these!)
                    for(DEdge pathEdg : pathEdges)
                    {
                        if(!doNotUse.contains(pathEdg))
                            doNotUse.add(pathEdg);
                    }
                }
                
                trys--;
                observations--;
                if(DiagSTN.PRINTWARNING)
                {
                    System.out.print("Adding obs: " + start.getID() + "," + 
                            end.getID());
                    System.out.print(" faulty edge: " + 
                            misbehave.getStart().getID() + "," + 
                            misbehave.getEnd().getID() + "\n");
                }
            }
            else
                trys--;
        }
        FalsieGroup FIGroup = new FalsieGroup();
        FIGroup.intendedEs = falseIntentions;
        FIGroup.allErrors = faultyEdges;
        return FIGroup;
    }
    
    /**
     * This is a struct for a _possible_ false observation that needs to be 
     * added after the network is initialized (so more of a intended obs)
     */
    private class Falsie
    {
        public Vertex[] falSE;    // start and end vertex
        public DEdge falseE;       // edg where an error could be introduced 
        public DEdge[] falsePath;  // full (single) path the error lays on
        boolean addError = true;
        
        // A list of possible other errors on other
        // paths to make sure that generated problems 
        // can be solved!
        public ArrayList<DEdge> otherEs; 
                               
        
        // Internal stuff for errorInit.
        public ArrayList<GraphPath> allPaths;
        public ArrayList<int[]> allBounds;
        public int finalChange;
        public int[] correctObs; // correct observation be4 changing the Graph!
    }
    
    /**
     * This combines all possible false observations with a list of all errors 
     * that are used to create a valid group of false observations (includes
     * errors that are not explicitly sought for by diagnosis but needed to construct
     * a valid diagnosis)
     */
    private class FalsieGroup
    {
        LinkedList<Falsie> intendedEs;
        LinkedList<DEdge> allErrors;
        
        // internal stuff for errorInitZP
        LinkedList<int[]> errorBounds; // what could a possible answer for an error be
        // these are the actual errors ie. pred + err = observ.
        
        public void printErrors()
        {
            if(allErrors == null)
                return;
            System.out.print(" used errors: ");
            for(DEdge edge : allErrors)
            {
                System.out.print(edge.getStart().getID() + "," + 
                        edge.getEnd().getID() + " ^ ");
            }
        }
    }
    
    /**
     * Given some freshly initialized Graph and some intended errors, add these
     * errors without making the graph or the errors inconsistent!
     * @param gO GraphObs object containing the problem.
     * @param fgroup All the possible locations the new errors should be placed.
     * @param settings Settings used for the problem generation.
     */
    private boolean initializeErrors(GraphObs gO, FalsieGroup fgroup, 
            GraphGenSettings settings)
    {
        if(settings.timeSyncT0)
        {
            return initializeZPErrors(gO, fgroup, settings);
        }
        
        LinkedList<Falsie> intendedEs = fgroup.intendedEs;
        
        gO.observations = new LinkedList();
        gO.errorEdges = new LinkedList();
        gO.errorDiffs = new LinkedList();

        Graph trueGraph = gO.graph.copy();   // not sure if needed but lets keep it rdy
        LinkedList<Falsie> addedErrors = new LinkedList();
        
        for(Falsie falseO : intendedEs)
        {
            Vertex oldStart, start, oldEnd, end;
            oldStart = falseO.falSE[0];
            oldEnd = falseO.falSE[1];
            start = gO.graph.getVertex(oldStart.getID());
            end = gO.graph.getVertex(oldEnd.getID());
            falseO.falSE[0] = start; // lets correct these values immediately
            falseO.falSE[1] = end;  // from the old graph reference to the new graph
            GraphPath startPath = new GraphPath(start);
            ArrayList<GraphPath> pathsFound = obsPaths(startPath, end, 
                    gO.graph);
            if(pathsFound.size() > 0)
            {
                // see if the original obs-path is still there 
                // and put it in a new GraphPath
                GraphPath falsePathNewGraph = new GraphPath(start);
                for(DEdge obsPathEdge : falseO.falsePath)
                {
                    int pathEdgFro = obsPathEdge.getStart().getID();
                    int pathEdgTo = obsPathEdge.getEnd().getID();
                    boolean stillh = gO.graph.directReach(
                            gO.graph.getVertex(pathEdgFro),
                            gO.graph.getVertex(pathEdgTo));
                    if(!stillh)
                    {
                        if(DiagSTN.PRINTWARNING)
                            System.out.println("Couldnt add f edge: " +
                                    pathEdgFro + " , " + pathEdgTo +
                                    " error path not found");
                        return false;
                    }
                    else
                    {
                        DEdge pathEdgeNewGraph = gO.graph.getDirectEdge(
                                gO.graph.getVertex(pathEdgFro), 
                                gO.graph.getVertex(pathEdgTo));
                        falsePathNewGraph.addStep(pathEdgeNewGraph,
                                gO.graph.getVertex(pathEdgTo));
                    }
                }
                falseO.falsePath = falsePathNewGraph.toEdges();
                // Check if the old edge is still there (and save a reference)
                int[] correctObservation = propPath(falsePathNewGraph);
                int falsEStart = falseO.falseE.getStart().getID();
                int falsEND = falseO.falseE.getEnd().getID();
                DEdge realFalseEdge = gO.graph.getDirectEdge(
                        gO.graph.getVertex(falsEStart), 
                        gO.graph.getVertex(falsEND));
                falseO.falseE = realFalseEdge; // translate to new Graph 
                
                
                boolean changeSet = false;  // Do we have to take some preset error
                int maxChange = 0;
                int[] changeLimits = new int[2];
                changeLimits[0] = Integer.MIN_VALUE;
                changeLimits[1] = Integer.MAX_VALUE;
                
                /* See if the error edge we want to use is part of any of the paths
                 * of already added observations! (if so we want to make sure the
                 * error will keep the other obs consistent!)
                 *  And See if some of the already added errors are part of one of the
                 * paths connecting the new obs ?? (its shouldnt be on the path!)
                
                These last 2 things should be combined */
                if(!addedErrors.isEmpty())
                {
                    // Need to check for each added error
                    for(Falsie addedErr : addedErrors)
                    {
                        ArrayList<Vertex> ancestAE = ancest(addedErr.falseE.getStart(),gO.graph);
                        ArrayList<Vertex> ancestCurE = ancest(realFalseEdge.getStart(),gO.graph);
                        LinkedList<Vertex> commonAncest = new LinkedList();
                        for(Vertex anAE : ancestAE)
                        {
                            for(Vertex anCurE : ancestCurE)
                            {
                                if(anAE.equals(anCurE) && !commonAncest.contains(anAE))
                                    commonAncest.add(anAE);
                            }
                        }
                        ArrayList<Vertex> descentAE = descendant(addedErr.falseE.getEnd(),gO.graph);
                        ArrayList<Vertex> descentCurE = descendant(realFalseEdge.getEnd(),gO.graph);
                        LinkedList<Vertex> commonDescent = new LinkedList();
                        for(Vertex deAE : descentAE)
                        {
                            for(Vertex deCurE : descentCurE)
                            {
                                if(deAE.equals(deCurE) && !commonDescent.contains(deAE))
                                    commonDescent.add(deAE);
                            }
                        }
                        // now we need to look for each pair what the limits are
                        // to the error what we want to intoduce
                        if(!commonDescent.isEmpty() && !commonAncest.isEmpty())
                        {
                            for(Vertex comAnc : commonAncest)
                            {
                                for(Vertex comDesc : commonDescent)
                                {
                                    int[] changelimit = new int[2];
                                    GraphPath p = new GraphPath(comAnc);
                                    ArrayList<int[]> allBounds = pathCalc(p, 0, 0, comDesc, gO.graph);
                                    int[] combined = combinePaths(allBounds);
                                    
                                    int[] preComb = new int[2];
                                    if(comAnc.equals(realFalseEdge.getStart()))
                                    {
                                        preComb[0] = 0;
                                        preComb[1] = 0;
                                    }
                                    else
                                    {
                                        GraphPath p2 = new GraphPath(comAnc);
                                        ArrayList<int[]> preBounds = pathCalc(p2, 0, 0, realFalseEdge.getStart(), gO.graph);
                                        preComb = combinePaths(preBounds);
                                    }
                                    
                                    int[] postComb = new int[2];
                                    if(comDesc.equals(realFalseEdge.getEnd()))
                                    {
                                        postComb[0] = 0;
                                        postComb[1] = 0;
                                    }
                                    else
                                    {
                                        GraphPath p3 = new GraphPath(realFalseEdge.getEnd());
                                        ArrayList<int[]> postBounds = pathCalc(p3, 0, 0, comDesc, gO.graph);
                                        postComb = combinePaths(postBounds);
                                    }
                                    
                                    int[] curInterval = new int[2];
                                    curInterval[0] = preComb[0] + postComb[0] + realFalseEdge.getLowerb();
                                    curInterval[1] = preComb[1] + postComb[1] + realFalseEdge.getUpperb();
                                    
                                    // to stay consistent it can move at least 
                                    changelimit[0] = combined[0] - curInterval[1];
                                    changelimit[1] = combined[1] - curInterval[0];
                                    
                                    // intersection with previously found values;
                                    if(changelimit[0] > changeLimits[0])
                                        changeLimits[0] = changelimit[0];
                                    if(changelimit[1] < changeLimits[1])
                                        changeLimits[1] = changelimit[1];
                                    
                                    changeSet = true;
                                }
                            }
                        }
                    }
                }
                
                
                ArrayList<int[]> boundsFound = new ArrayList();
                falseO.allPaths = pathsFound;
                for(GraphPath gp : pathsFound)
                {
                    int[] bounds = propPath(gp);
                    boundsFound.add(bounds);
                }
                // Warning: uses different type of boundsFound method! (needs testing)
                falseO.allBounds = boundsFound; 
                int[] boufou = combinePaths(boundsFound);
                int dff;
                if(!changeSet)
                {
                    // we can think of some new error as long as it doesnt make the system
                    // inconsistent
                    maxChange = boufou[1] - boufou[0];
                    double transf = ((double) settings.difference) / 100.0;
                    int lbc = (int) (boufou[0] * transf);
                    dff = Math.min(maxChange, lbc);
                    if(realFalseEdge.getLowerb() == 0) 
                    {
                        // if the edge cant be moved lower, we move it the other way
                        // ie. observation is earlier than predicted .. (not common but ok)
                        dff = -dff;
                    }
                    falseO.finalChange = dff;
                    int newlb = realFalseEdge.getLowerb() - dff; // pred = obs - error
                    int newub = realFalseEdge.getUpperb() - dff;
                    if(newlb < 0)
                    {
                        newlb = 0;
                        dff = realFalseEdge.getLowerb();
                        falseO.finalChange = dff;
                        newub = realFalseEdge.getUpperb() - dff;
                    }
                    realFalseEdge.setLowerb(newlb);  // Set the wrong/false values
                    realFalseEdge.setUpperb(newub);
                    
                    /**
                     * This shouldnt be tested for just the observation Vertex pair
                     * but for all ancestors and descendants to make sure the system
                     * is still consistent for some added error!
                     */
                }
                else
                {
                    Random rand = new Random();
                    // A test to compare the wiggle room of the edge vs
                    // the changeLimits wiggle room!
                    if(changeLimits[0] < 0)
                    {   // 
                        if((realFalseEdge.getLowerb() + changeLimits[0]) < 0)
                        {
                            // else make sure the wiggle room down is adjusted to the 
                            // space the edge has
                            changeLimits[0] = - realFalseEdge.getLowerb();
                        }
                    }
                    // Need to add an error according to the change set.
                    if(changeLimits[0] > changeLimits[1] || (changeLimits[0] == 0 && changeLimits[1] == 0))
                    {
                        // it was impossible to add some error in that position
                        // so error is skipped
                        continue;
                    }
                    else
                    {
                        int width = changeLimits[1] - changeLimits[0];
                        int randVal = changeLimits[0] + rand.nextInt(width + 1); // inclusive so +1
                        if(randVal == 0)
                            if(changeLimits[1] == 0)
                                randVal--;
                            else
                                randVal++; // cant be both 0!
                        dff = -randVal; // change is opposite of error!
                        
                        falseO.finalChange = dff;
                        int newlb = realFalseEdge.getLowerb() - dff; // pred = obs - error
                        int newub = realFalseEdge.getUpperb() - dff;
                        if(newlb < 0)
                        {
                            newlb = 0;
                            dff = realFalseEdge.getLowerb();
                            falseO.finalChange = dff;
                            newub = realFalseEdge.getUpperb() - dff;
                        }
                        realFalseEdge.setLowerb(newlb);  // Set the wrong/false values
                        realFalseEdge.setUpperb(newub);
                    }
                }
                
                // the correct observation is used!
                Observation ob = new Observation(start, end, correctObservation[0], correctObservation[1]);
                gO.observations.add(ob);
                gO.errorEdges.add(realFalseEdge);
                gO.errorDiffs.add(falseO.finalChange); 
                // this is the expected error (= obs - pred)
                
                addedErrors.add(falseO);
            }
            else
            {
                // observationPath is no more!
                if(DiagSTN.PRINTWARNING)
                    System.out.println("Couldnt add false obs: " + start.getID()
                            + " , " + end.getID() + " no path found");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Given some freshly initialized Graph and some intended errors, add these
     * errors without making the graph or the errors inconsistent! 
     * special ZeroPoint version
     * @param gO GraphObs object containing the problem.
     * @param fgroup All the possible locations the new errors should be placed.
     * @param settings Settings used for the problem generation.
     */
    private boolean initializeZPErrors(GraphObs gO, FalsieGroup fgroup, 
            GraphGenSettings settings)
    {
        // TODO; for now special version to make sure no regressions happen!
        LinkedList<Falsie> intendedEs = fgroup.intendedEs;
        
        // needed for last special ZP test
        fgroup.errorBounds = new LinkedList();
        for(DEdge smtng : fgroup.allErrors)
        {
            int[] tmp = new int[2];
            tmp[0] = Integer.MIN_VALUE;
            tmp[1] = Integer.MAX_VALUE;
            fgroup.errorBounds.add(tmp);
        }
        
        gO.observations = new LinkedList();
        gO.errorEdges = new LinkedList();
        gO.errorDiffs = new LinkedList();

        Graph trueGraph = gO.graph.copy();   // not sure if needed but lets keep it rdy
        LinkedList<Falsie> addedErrors = new LinkedList();
        
        for(Falsie falseO : intendedEs)
        {
            Vertex oldStart, start, oldEnd, end;
            oldStart = falseO.falSE[0];
            oldEnd = falseO.falSE[1];
            start = gO.graph.getVertex(oldStart.getID());
            end = gO.graph.getVertex(oldEnd.getID());
            falseO.falSE[0] = start; // lets correct these values immediately
            falseO.falSE[1] = end;  // from the old graph reference to the new graph
            GraphPath startPath = new GraphPath(start);
            ArrayList<GraphPath> pathsFound = obsPaths(startPath, end, 
                    gO.graph);
            
            // No paths already means that its not going to work!
            if(pathsFound.size() < 1)
            {
                // observationPath is no more!
                if(DiagSTN.PRINTWARNING)
                    System.out.println("Couldnt add false obs: " + start.getID()
                            + " , " + end.getID() + " no path found");
                return false;
            }
            
            // see if the original obs-path is still there 
            // and put it in a new GraphPath
            GraphPath falsePathNewGraph = new GraphPath(start);
            for(DEdge obsPathEdge : falseO.falsePath)
            {
                int pathEdgFro = obsPathEdge.getStart().getID();
                int pathEdgTo = obsPathEdge.getEnd().getID();
                boolean stillh = gO.graph.directReach(
                        gO.graph.getVertex(pathEdgFro),
                        gO.graph.getVertex(pathEdgTo));
                if(!stillh)
                {
                    if(DiagSTN.PRINTWARNING)
                        System.out.println("Couldnt add f edge: " +
                                pathEdgFro + " , " + pathEdgTo +
                                " error path not found");
                    return false;
                }
                else
                {
                    DEdge pathEdgeNewGraph = gO.graph.getDirectEdge(
                            gO.graph.getVertex(pathEdgFro), 
                            gO.graph.getVertex(pathEdgTo));
                    falsePathNewGraph.addStep(pathEdgeNewGraph,
                            gO.graph.getVertex(pathEdgTo));
                }
            }
            falseO.falsePath = falsePathNewGraph.toEdges();
            // Check if the old edge is still there (and save a reference)
            int[] correctObservation = propPath(falsePathNewGraph);
            falseO.correctObs = correctObservation;
            int falsEStart = falseO.falseE.getStart().getID();
            int falsEND = falseO.falseE.getEnd().getID();
            DEdge realFalseEdge = gO.graph.getDirectEdge(
                    gO.graph.getVertex(falsEStart), 
                    gO.graph.getVertex(falsEND));
            falseO.falseE = realFalseEdge; // translate to new Graph 


            boolean changeSet = false;  // Do we have to take some preset error
            int maxChange = 0;
            int[] changeLimits = new int[2];
            changeLimits[0] = Integer.MIN_VALUE;
            changeLimits[1] = Integer.MAX_VALUE;

            // test if there are 2 errors on a path // ZP EXTRA
            for(GraphPath gp : pathsFound)
            {
                int fault = 0;
                boolean hasEdgMisbehave = false;
                DEdge[] edges = gp.toEdges();
                for(DEdge de : edges)
                {
                    if(de.equals(falseO.falseE))
                    {
                        fault++;
                        hasEdgMisbehave = true;
                    }
                    for(Falsie flsi : addedErrors)
                    {
                        if(de.equals(flsi.falseE))
                            fault++;
                    }
                }
                if(fault > 1)
                {
                    // For now lets just return false (best if we leave 1 error
                    // out and just add the observation)
                    if(DiagSTN.PRINTWARNING)
                        System.out.println("Double error found, skipped obs-add");
                    return false;
                }
                else if(hasEdgMisbehave && (edges.length != falseO.falsePath.length))
                {
                    // if it uses the faulty edge but has a different length
                    // then strong probability answer is inconsistent
                    if(DiagSTN.PRINTWARNING)
                        System.out.println("error with wrong path length found"
                                + ", skipped obs-add");
                    return false;
                }
            }

            /* See if the error edge we want to use is part of any of the paths
             * of already added observations! (if so we want to make sure the
             * error will keep the other obs consistent!)
             *  And See if some of the already added errors are part of one of the
             * paths connecting the new obs ?? (its shouldnt be on the path!)

            These last 2 things should be combined */
            if(!addedErrors.isEmpty())
            {
                // Need to check for each added error
                for(Falsie addedErr : addedErrors)
                {
                    ArrayList<Vertex> ancestAE = ancest(addedErr.falseE.getStart(),gO.graph);
                    ArrayList<Vertex> ancestCurE = ancest(realFalseEdge.getStart(),gO.graph);
                    LinkedList<Vertex> commonAncest = new LinkedList();
                    for(Vertex anAE : ancestAE)
                    {
                        for(Vertex anCurE : ancestCurE)
                        {
                            if(anAE.equals(anCurE) && !commonAncest.contains(anAE))
                                commonAncest.add(anAE);
                        }
                    }
                    ArrayList<Vertex> descentAE = descendant(addedErr.falseE.getEnd(),gO.graph);
                    ArrayList<Vertex> descentCurE = descendant(realFalseEdge.getEnd(),gO.graph);
                    LinkedList<Vertex> commonDescent = new LinkedList();
                    for(Vertex deAE : descentAE)
                    {
                        for(Vertex deCurE : descentCurE)
                        {
                            if(deAE.equals(deCurE) && !commonDescent.contains(deAE))
                                commonDescent.add(deAE);
                        }
                    }
                    // now we need to look for each pair what the limits are
                    // to the error what we want to intoduce
                    if(!commonDescent.isEmpty() && !commonAncest.isEmpty())
                    {
                        for(Vertex comAnc : commonAncest)
                        {
                            for(Vertex comDesc : commonDescent)
                            {
                                int[] changelimit = new int[2];
                                GraphPath p = new GraphPath(comAnc);
                                ArrayList<int[]> allBounds = pathCalc(p, 0, 0, comDesc, gO.graph);
                                int[] combined = combinePaths(allBounds);

                                int[] preComb = new int[2];
                                if(comAnc.equals(realFalseEdge.getStart()))
                                {
                                    preComb[0] = 0;
                                    preComb[1] = 0;
                                }
                                else
                                {
                                    GraphPath p2 = new GraphPath(comAnc);
                                    ArrayList<int[]> preBounds = pathCalc(p2, 0, 0, realFalseEdge.getStart(), gO.graph);
                                    preComb = combinePaths(preBounds);
                                }

                                int[] postComb = new int[2];
                                if(comDesc.equals(realFalseEdge.getEnd()))
                                {
                                    postComb[0] = 0;
                                    postComb[1] = 0;
                                }
                                else
                                {
                                    GraphPath p3 = new GraphPath(realFalseEdge.getEnd());
                                    ArrayList<int[]> postBounds = pathCalc(p3, 0, 0, comDesc, gO.graph);
                                    postComb = combinePaths(postBounds);
                                }

                                int[] curInterval = new int[2];
                                curInterval[0] = preComb[0] + postComb[0] + realFalseEdge.getLowerb();
                                curInterval[1] = preComb[1] + postComb[1] + realFalseEdge.getUpperb();

                                // to stay consistent it can move at least 
                                changelimit[0] = combined[0] - curInterval[1];
                                changelimit[1] = combined[1] - curInterval[0];

                                // intersection with previously found values;
                                if(changelimit[0] > changeLimits[0])
                                    changeLimits[0] = changelimit[0];
                                if(changelimit[1] < changeLimits[1])
                                    changeLimits[1] = changelimit[1];

                                changeSet = true;
                            }
                        }
                    }
                }
            }


            ArrayList<int[]> boundsFound = new ArrayList();
            falseO.allPaths = pathsFound;
            for(GraphPath gp : pathsFound)
            {
                int[] bounds = propPath(gp);
                boundsFound.add(bounds);
            }
            // Warning: uses different type of boundsFound method!
            falseO.allBounds = boundsFound; 
            int[] boufou = combinePaths(boundsFound);
            int dff;
            if(!changeSet)
            {
                // we can think of some new error as long as it doesnt make the system
                // inconsistent
                maxChange = boufou[1] - boufou[0];
                double transf = ((double) settings.difference) / 100.0;
                int lbc = (int) (boufou[0] * transf);
                dff = Math.min(maxChange, lbc);
                if(realFalseEdge.getLowerb() == 0) 
                {
                    // if the edge cant be moved lower, we move it the other way
                    // ie. observation is earlier than predicted .. (not common but ok)
                    dff = -dff;
                }
                falseO.finalChange = dff;
                int newlb = realFalseEdge.getLowerb() - dff; // pred = obs - error
                int newub = realFalseEdge.getUpperb() - dff;
                if(newlb < 0)
                {
                    newlb = 0;
                    dff = realFalseEdge.getLowerb();
                    falseO.finalChange = dff;
                    newub = realFalseEdge.getUpperb() - dff;
                }
                realFalseEdge.setLowerb(newlb);  // Set the wrong/false values
                realFalseEdge.setUpperb(newub);

                /**
                 * This shouldnt be tested for just the observation Vertex pair
                 * but for all ancestors and descendants to make sure the system
                 * is still consistent for some added error!
                 */
            }
            else
            {
                Random rand = new Random();
                // A test to compare the wiggle room of the edge vs
                // the changeLimits wiggle room!
                if(changeLimits[0] < 0)
                {   // 
                    if((realFalseEdge.getLowerb() + changeLimits[0]) < 0)
                    {
                        // else make sure the wiggle room down is adjusted to the 
                        // space the edge has
                        changeLimits[0] = - realFalseEdge.getLowerb();
                    }
                }
                // Need to add an error according to the change set.
                if(changeLimits[0] > changeLimits[1] || (changeLimits[0] == 0 && changeLimits[1] == 0))
                {
                    // it was impossible to add some error in that position
                    // so couldnt add all obs (lets make it false)
                    return false;
                }
                else
                {
                    int width = changeLimits[1] - changeLimits[0];
                    int randVal = changeLimits[0] + rand.nextInt(width + 1); // inclusive so +1
                    if(randVal == 0)
                        if(changeLimits[1] == 0)
                            randVal--;
                        else
                            randVal++; // cant be both 0!
                    dff = -randVal; // change is opposite of error!

                    falseO.finalChange = dff;
                    int newlb = realFalseEdge.getLowerb() - dff; // pred = obs - error
                    int newub = realFalseEdge.getUpperb() - dff;
                    if(newlb < 0)
                    {
                        newlb = 0;
                        dff = realFalseEdge.getLowerb();
                        falseO.finalChange = dff;
                        newub = realFalseEdge.getUpperb() - dff;
                    }
                    realFalseEdge.setLowerb(newlb);  // Set the wrong/false values
                    realFalseEdge.setUpperb(newub);
                }
            }
            
            // A test to see if all the other paths can be explained by some of
            // the errors added // ZP Extra!
            
            // first enter the just chosen new change in falsO.falsE
            boolean realFalseFound = false;
            int[] dffIntroduced = new int[2];
            dffIntroduced[0] = dff;
            dffIntroduced[1] = dff;
            for(int it = 0; it < fgroup.allErrors.size(); it++)
            {
                if(realFalseEdge.sameIds(fgroup.allErrors.get(it)))
                {
                    int[] rfEBounds = fgroup.errorBounds.get(it);
                    rfEBounds = dffIntroduced;
                }
            }
            
            // part 2 outside falsie loop because all errors need to be
            // linitialized (TODO: during calculation check if some error
            // is part of another obs path and adjust things accordingly)
            
            // the correct observation is used!
            Observation ob = new Observation(start, end, correctObservation[0], correctObservation[1]);
            gO.observations.add(ob);
            gO.errorEdges.add(realFalseEdge);
            gO.errorDiffs.add(falseO.finalChange); 
            // this is the expected error (= obs - pred)

            addedErrors.add(falseO);
        }
        
        // If some error explains 2 different observations, can it explain
        // them both properly. Also test if there is some error possible for
        // some given edge
        for(Falsie falsO : intendedEs)
        {
            // see if every path of every Obs can be explained by one of the errors!
            for(GraphPath somePath :  falsO.allPaths)
            {
                int errorPos = -1;
                int numErrors = 0; // 2nd extra check if paths have 2 errors
                DEdge[] edges = somePath.toEdges();
                for(DEdge dedg : edges)
                {
                    for(int jter = 0; jter < fgroup.allErrors.size(); jter++)
                    {
                        if(dedg.sameIds(fgroup.allErrors.get(jter)))
                        {
                            errorPos = jter;
                            numErrors++;
                        }
                    }
                }
                if(numErrors != 1)  // should simply have a single error on each path!
                    return false;
                
                /** Now test if it is possible to change this edge @ jter 
                 *  and explain the strange output obs. (ie. is the resulting
                 *  system consistent).
                 */
                int[] pathValues = propPath(somePath);
                int[] obsValues = {falsO.correctObs[0], falsO.correctObs[1]};
                int lbDifference = obsValues[0] - pathValues[0];
                int ubDifference = obsValues[1] - pathValues[1];
                // diff = -error so lets check 
                
                // pathValue + dff = obsValue
                
                int[] errorSaved = fgroup.errorBounds.get(errorPos);
                // if error is set then the error must at least explain the obs
                if(errorSaved[0] != Integer.MIN_VALUE || errorSaved[1] != Integer.MAX_VALUE)
                {
                    if(pathValues[0] + errorSaved[0] > obsValues[1])
                        return false;   // problem!
                    if(pathValues[1] + errorSaved[1] < obsValues[0])
                        return false;
                    // here on the change is valid!
                }
                else    // Set error for other paths
                {
                    errorSaved[0] = obsValues[0] - pathValues[0];
                    errorSaved[1] = obsValues[1] - pathValues[1];
                }
                
                // Could also check if error can be made @ the given position 
                // (check if the edge wont become negative but for now this is
                // not a problem since edges _can_ be negative) *with special
                // flags*
            }
        }
        return true;
    }
    
    /**
     * Runs a test to see if Ancestry is correctly detected.
     */
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
    
    /**
     * Initializes a test graph. Used for testing outside GraphGenerator.
     * @return An initialized graph
     */
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
          
    // --- end Graph Initialization
    
    /**
     * Adds true observations to a diagnosis problem. Note that only 1 path is
     * used for the prediction value (ie. longer paths will add more false 
     * observation paths than true ones)
     * @param go GraphObs object with all the ingredients for a Diagnosis problem
     * @param num Number of true observations
     * @param size path-length of the observations added
     */
    public void addTrueObs(GraphObs go, int num, int size)
    {
        LinkedList<DEdge[]> trueO = new LinkedList();
        LinkedList<DEdge> doNotUse = new LinkedList();
        
        for(DEdge falseEdge: go.errorEdges)
            doNotUse.add(falseEdge);
        
        int trys = 100000; 
        // Trys to add observations of a certain length but not sure if its
        // possible 
        int currentLen;
        
        Graph graph = go.graph;
        Random rand = new Random();
        
        while(num > 0 && trys > 0)
        {
            currentLen = size;
            DEdge correctEdge = graph.randomEdge();
            if(doNotUse.contains(correctEdge))
                continue;
            Vertex end = correctEdge.getEnd();
            Vertex start = correctEdge.getStart();
            LinkedList<DEdge> path = new LinkedList();
            path.add(correctEdge);
            currentLen--;
            // First try to add edges to the end of the obs path
            while(currentLen > 0)
            {
                LinkedList<Vertex> possibilities = graph.adjacentNodes(end);
                if(possibilities == null || possibilities.size() < 1)
                    break;
                Vertex newEnd = possibilities.get(rand.nextInt(possibilities.size()));
                DEdge de = graph.getDirectEdge(end, newEnd);
                if(doNotUse.contains(de))
                {
                    if(possibilities.size() < 2)
                        break; // can only take already malfunctioning edge
                    else
                        continue; // try another path!
                }
                path.add(de);
                end = newEnd;
                currentLen--;
            }
            // tried as much as possible, time to add to the front
            while(currentLen > 0)
            {
                LinkedList<Vertex> possibilities = graph.incomingNodes(start);
                if(possibilities == null || possibilities.size() < 1)
                    break;
                
                Vertex newStart = possibilities.get(rand.nextInt(possibilities.size()));
                DEdge de = graph.getDirectEdge(newStart, start);
                if(doNotUse.contains(de))
                {
                    if(possibilities.size() < 2)
                        break; // can only take already malfunctioning edge
                    else
                        continue; // try another path!
                }
                path.push(de);
                start = newStart;
                currentLen--;
            }
            if(currentLen < 1)
            {
                // its good observation!
                Vertex[] newObs = new Vertex[2];
                newObs[0] = start;
                newObs[1] = end;
                trueO.add(path.toArray(new DEdge[path.size()]));
                
                for(DEdge de : path)
                {
                    if(!doNotUse.contains(de))
                        doNotUse.add(de);
                }
                DEdge[] pathE = path.toArray(new DEdge[path.size()]);
                trys--;
                num--;
            }
            else
                trys--;
        }
        
        // Now add all found true observations to the graphObs (with the correct
        // times!)
        for(DEdge[] truePath : trueO)
        {
            int lb = 0;
            int ub = 0;
            for(DEdge de : truePath)
            {
                lb += de.getLowerb();
                ub += de.getUpperb();
            }
            Vertex start = truePath[0].getStart();
            Vertex end = truePath[truePath.length - 1].getEnd();
            
            Observation newTrueObs = new Observation(start, end, lb, ub);
            go.observations.add(newTrueObs);
        }
    }
    
    /**
     * Tests the consistency for each of the observations.
     * @param go GraphObs which has all the information for a Problem
     * @return False is 2 or more paths on some observation are inconsistent
     * (do not overlap)
     */
    public boolean checkConsist(GraphObs go)
    {
        List<Observation> obs = go.observations;
        for(Observation ob : obs)
        {
            Vertex start = ob.startV;
            GraphPath g = new GraphPath(start);
            ArrayList<int[]> bounds = pathCalc(g, 0, 0, ob.endV, go.graph);
            int[] combined = combinePaths(bounds);
            if(combined[0] > combined[1])
                return false;
        }
        return true;
    }
}

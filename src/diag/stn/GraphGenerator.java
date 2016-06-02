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
    
    /**
     * Struct like combination of the Graph and its Observations
     */
    public class GraphObs
    {
        public Graph graph;
        public List<Observation> observations;
    }
    
    public class BuildVertex
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
        Vertex first = new Vertex(id);
        gr.addVertex(first);
        id++;
        Vertex second = new Vertex(id);
        id++;
        int lb = rand.nextInt(51);
        int ub = lb + rand.nextInt(21);
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
            int links = Math.min(i + 1, linksPerStep);
            BAaddVertex(gr, links, onlymax);
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
        
        while(falseObs > 0)
        {
            // Since its new to old try to get a path between 2 by trialNerror
            int half = vertInfo.size() /2;
            BuildVertex fromV = vertInfo.get(half + rand.nextInt(vertInfo.size() - half));
            BuildVertex toV = vertInfo.get(rand.nextInt(half));
            GraphPath startPath = new GraphPath(fromV.vert);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV.vert, gr);
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // TODO Calc proper lb & ub !!!!
                Observation ob = new Observation(fromV.vert, toV.vert, boundsFound.get(0)[0], boundsFound.get(0)[1]);
                grOb.observations.add(ob);
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
            
            
            for(Observation fObs : grOb.observations)
            {
                if(fObs.startV.equals(fromV.vert))
                {
                    if(fObs.endV.equals(toV.vert))
                        continue trueObsAdd; // yep, its the quickNDirty
                }
            }
            
            GraphPath startPath = new GraphPath(fromV.vert);
            ArrayList<int[]> boundsFound = pathCalc(startPath, 0, 0, toV.vert, gr);
            if(!boundsFound.isEmpty())
            { // there is actually a path!
                // if it is fully correct all paths should be the same... !!!!
                Observation ob = new Observation(fromV.vert, toV.vert, boundsFound.get(0)[0], boundsFound.get(0)[1]);
                grOb.observations.add(ob);
                trueObs--;
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
        vertInfo.add(nbv); // should be @ location "id" ...
        
        // Decide how many connections to make 
        if(!onlymax)
        {
            int oldlinks = links;
            links = rand.nextInt(oldlinks);
            // We allow 0 links, for the network to get more than 1 end point!
        }
        
        int ignore = 0;
        int add = 0;
        while(links>0)  // While there are new edges needed, addmore!
        {
            double prob = 0;    // the odds
            double randNum = rand.nextDouble();
            
            for(BuildVertex bigv : vertInfo)    // For each of the current Vertex
            {
                // TODO: Possibility to check for paths before adding it
                
                prob += (double) ((double) bigv.degree) / 
                        ((double) (2.0d * numEdges) - ignore);
                
                if(randNum <= prob) // add edge time
                {
                    // Here be the check 4 the edge properties (if needed)
                    GraphPath gp = new GraphPath(newV);
                    ArrayList<int[]> oldlimits = pathCalc(gp,0,0,bigv.vert,g);
                    if(oldlimits.isEmpty())
                    {
                        int lb = rand.nextInt(51);
                        int ub = lb + rand.nextInt(21);
                        g.addEdge(newV, bigv.vert, lb, ub);
                    }
                    else    // TODO: combine the limits to a single limit!
                    {
                        int[] lbub = oldlimits.get(0);
                        g.addEdge(newV, bigv.vert, lbub[0], lbub[1]);
                    }
                    // find the proper values for these plox!
                    ignore += bigv.degree;
                    bigv.degree++;
                    nbv.degree++;
                    add++;
                    
                    break; // break out to while loop (next edge if there are)
                }
            }
        }
        numEdges += add;
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
    
    public GraphObs generatePlanlikeGraph(int linelb, int lineub, int maxlinecon, int maxcon)
    {
        // See idea 23 May
        
        // init: calc the Random.nextint and the fixed addition
        
        // Calc a full line and add it to the graph
        
        // if there are more lines add it to between 1 and maxlinecon other lines
        // for each connection, connect with between 1 and maxcon vertices of the other
        // line
        
        // finally add (small) observations (maybe within lines) that are correct
        
        // add big obs that are incorrect -> diagnosis.
        return null;
    }
    
}

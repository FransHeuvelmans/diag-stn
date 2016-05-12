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
package diag.stn.analyze;

import diag.stn.DiagSTN;
import diag.stn.STN.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * The analyst checks a model and diagnoses the possible problems in the model.
 * The model is represented by a directed graph
 * @author Frans van den Heuvel
 */
public class Analyst
{
    private Graph graph;
    private ArrayList<Observation> observations;
    private Map<Vertex, Integer> fixedTimes; // add a 0/time point to any vertex
    private Map<Observation, LinkedHashSet<GraphPath>> obsPaths;
    private ArrayList<Diagnosis> diagnosisList;
    
    private Map<GraphPath, int[]> diffStore; // Need to store changes before applying to the path
    
    
    // Each observation might have multiple paths connected
    
    /**
     * Creates a new analyst for a given Graph.
     * @param g preferably instantiated Graph object that needs to be analyzed
     */
    public Analyst(Graph g)
    {
        observations = new ArrayList<>();
        fixedTimes = new HashMap<>();
        obsPaths = new HashMap<>(); 
        diffStore = new HashMap<>(); 
        diagnosisList = new ArrayList<>();
        graph = g;
    }
    
    /**
     * Adds an observation that needs to be tested and is possibly used to create
     * a diagnosis.
     * @param ob instantiated Observation object.
     */
    public void addObservation(Observation ob)
    {
        observations.add(ob);
    }
    
    /**
     * Adds a fixed time to the starting point, if the model should not start at 0
     * @param v Vertex that is the starting point
     * @param i integer with time/cost @ this starting point
     */
    public void addFixedTime(Vertex v, int i)
    {
        if(fixedTimes.containsKey(v))
        {
            System.err.println("Tried to add a fixed time point to a vertex that has one");
            return;
        }
        fixedTimes.put(v, i);
    }
    
    /**
     * Generates all paths for each observation and stores them.
     */
    public void generatePaths()
    {
        // given the observations, what paths must be checked ?
        // puts them in a map ! -> see simplePaths
        
        for(Observation ob : observations)
        {
            GraphPath g = new GraphPath(ob.startV);
            simplePaths(g, ob);
        }
    }
    
    /**
     * For each stored path the weights are propagated along the path and the 
     * results are checked and discrepancies are stored. (needs paths generated
     * by generatePaths() in order to work)
     */
    public void propagateWeights()
    {
        // For now first use the stored paths and calculate ALL paths
        int lb, ub, deltalb, deltaub, changelb, changeub, sizeObs, sizePred;
        int[] chng;
        Integer strtVal;
        
        for(Observation o: observations)
        {
            sizeObs = o.endUb - o.endLb;
            LinkedHashSet<GraphPath> paths = obsPaths.get(o);
            int[] checkBounds = null;
            ArrayList<int[]> pathBounds = new ArrayList();
            if(paths != null)   // only if there are paths
            {
                for(GraphPath p : paths)  // lets do each path separate for now
                {
                    // First see if there is some fixedTime for the starting vertex
                    strtVal = fixedTimes.get(o.startV); // lets hope that o.startV == p.getStepV(0)
                    if(strtVal != null)
                    {
                        lb = (int) strtVal;
                        ub = (int) strtVal;
                    }
                    else    // 0 point!
                    {
                        lb = 0;
                        ub = 0;
                    }
                    for(int i=1; i < p.stepSize(); i++)
                    {
                        DEdge de = p.getStepE(i);
                        lb += de.getLowerb();
                        ub += de.getUpperb();
                    }
                    int[] lbub = new int[2];
                    lbub[0] = lb;
                    lbub[1] = ub;
                    pathBounds.add(lbub); // first just store the raw data
                    
                    // TODO: Remove should start here
                    deltalb = o.endLb - lb; // lets keep this order and use no abs!
                    deltaub = o.endUb - ub;
                    
                    sizePred = ub - lb;
                    if(sizeObs < sizePred) // A check to add some info to the observation
                        o.moreAccurate = true;
                    else
                        o.moreAccurate = false;
                    
                    changelb = Math.min(deltalb, deltaub);
                    changeub = Math.max(deltalb, deltaub);
                    chng = new int[2]; //test & check code
                    chng[0] = changelb;
                    chng[1] = changeub;
                    diffStore.put(p, chng);// First store each of the paths
                    
                    // START REMOVE HERE 
                    if(checkBounds != null)
                    {
                        if((checkBounds[0] != chng[0])||checkBounds[1] != chng[1])
                            System.err.println("Inconsistent path found!");
                    }
                    else
                    {
                        checkBounds = new int[2];
                        checkBounds[0] = chng[0];
                        checkBounds[1] = chng[1];
                    }
                    
                    
                    if((chng[0] == 0)&&(chng[1] == 0))
                        o.fixneeded = false; //quickNdirty
                    else
                        o.fixneeded = true;
                    
                    for(int j=1; j < p.stepSize(); j++)
                    {
                        DEdge de = p.getStepE(j);
                        de.addPossibleChange(changelb, changeub); 
                        // store the possible changes
                    }
                    // END HERE (make sure that function has been put somewhere else)
                }
                // Refine/combine all the Bounds to a single bound
                int[] finalBounds = pathBounds.get(0).clone(); // should be a path!
                int[] union = finalBounds.clone();
                int[] intersect = finalBounds.clone();
                for(int i = 1; i < pathBounds.size(); i++)
                {
                    int[] addBound = pathBounds.get(i).clone();
                    if((addBound[0] > intersect[1]) || (addBound[1] < intersect[0]))
                    {
                        System.err.println("Inconsistent path found!");
                        // Abort ??
                    }
                    int unionlb = union[0] < addBound[0] ? union[0] : addBound[0];
                    int unionub = union[1] > addBound[1] ? union[1] : addBound[1];
                    union[0] = unionlb;
                    union[1] = unionub;
                    int intsctlb = intersect[0] > addBound[0] ? intersect[0] : addBound[0];
                    int intsctub = intersect[1] < addBound[1] ? intersect[1] : addBound[1];
                    intersect[0] = intsctlb;
                    intersect[1] = intsctub;
                }
                
                int sizeUnion = union[1] - union[0];
                if(sizeObs > sizeUnion)
                {
                    // Fits in the whole obs
                    finalBounds[0] = o.endLb - union[0];
                    finalBounds[1] = o.endUb - union[1];
                    // similar to the old delta-lb/ub
                }
                else
                {
                    // Need to fit intersection in there!
                    int lbChange, lbIntChange, lbUnChange;
                    lbIntChange = o.endLb - intersect[0];
                    lbUnChange = o.endUb - union[1]; 
                    // union should explain whole obs (since union > obs)
                    
                    // TODO: ubChange should be the _abs_(smallest) change of the 2
                    // Riight ??? (test this first in notes!)
                    
                    int ubChange, ubIntChange, ubUnChange;
                    // TODO same for ub!!
                    
                    finalBounds[0] = lbChange;
                    finalBounds[1] = ubChange;
                }
                
                // TODO: add Math.min / Math.max stuff here (???)
                
                // This actually means if some movement is possible
                // or atleast should be suggested
                if((finalBounds[0] == 0)&&(finalBounds[1] == 0))
                        o.fixneeded = false; //quickNdirty
                else 
                        o.fixneeded = true;
                
                /*
                 * Now finally add the possible changes to all the edges.
                 * Can only be done after we know what changes will be applied
                 * to the other paths (of this observation) 
                 */
                for(GraphPath p : paths)
                {
                    for(int j=1; j < p.stepSize(); j++)
                    {
                        DEdge de = p.getStepE(j);
                        de.addPossibleChange(finalBounds[0], finalBounds[1]); 
                        // store the possible changes
                    }
                }
            }
        }
    }
    
    /**
     * Get a list of all possible full diagnosis for this model. Needs propagateWeights
     * to be done before diagnosis can start.
     * @return A list of possible Diagnosis for the model
     */
    public Diagnosis[] generateDiagnosis()
    {
        LinkedList<GraphPath> needDiag = new LinkedList<>();
        for(Observation ob: observations)
        {
            // if observation is wrong....
            // ie if difference is 0 ????
            if(ob.fixneeded)
            {
                LinkedHashSet<GraphPath> pathSet = obsPaths.get(ob);
                for(GraphPath p : pathSet)
                    needDiag.add(p);    // faster way to copy?
            }
        }
        /**
         * CHECK: if no fix is needed the values shouldnt be allowed to change!!
         * Is it caught with the [0,0] bounds added to the edges of that observ.?
         */
        generateDiagnosis(new Diagnosis(), needDiag, new ArrayList<>());
        return diagnosisList.toArray(new Diagnosis[diagnosisList.size()]);
    }
    
    private void generateDiagnosis(Diagnosis diagOriginal, LinkedList<GraphPath> wronglyPredicted, ArrayList<GraphPath> testedPaths)
    {
        if(wronglyPredicted.isEmpty())
            return;
        GraphPath path = wronglyPredicted.pop();    
        testedPaths.add(path);
        if(!diagOriginal.edgeUsed(path)) // if path is not solved try to solve it
        {   
            // ie. if in the current diagnosis an edge has already been solved on the current path
            for(int i = 1; i < path.stepSize(); i++)
            { 
                DEdge edge = path.getStepE(i);  // for EACH edge on this path!
                Diagnosis diag = diagOriginal.copy(); // new Diagnosis

                //check if edge is part of any of the testedPaths, if so 
                // it can't be used
                boolean alreadyUsed = false;
                if(!testedPaths.isEmpty())
                {
                    for(int j = 0; j < testedPaths.size()-1;j++) // shouldnt check the last added Path (ie. the current Path!!)
                    {
                        GraphPath p = testedPaths.get(j);
                        if(p.edgeUsed(edge))
                            alreadyUsed = true; // can't use this edge in current diagnosis 
                    }                           // already part of solved path
                }
                if(alreadyUsed)
                {
                    continue;
                }
                // if combine possible
                boolean combine = true;
                ArrayList<int[]> changes = edge.getPossibleChanges();
                int[] finalchng = changes.remove(changes.size()-1); // lets start with the last added bounds
                for(int[] bounds : changes)
                {
                    if((bounds[0] > finalchng[1])||(bounds[1] < finalchng[0]))
                    {
                        combine = false;
                        break;
                    }
                    else
                    {
                        if(bounds[0] > finalchng[0])
                            finalchng[0] = bounds[0];
                        if(bounds[1] < finalchng[1])
                            finalchng[1] = bounds[1];
                    }
                }
                if(combine)
                {
                    diag.addPartial(edge, finalchng[0], finalchng[1]);
                    if(wronglyPredicted.isEmpty())
                        diagnosisList.add(diag);
                    else
                    {
                        LinkedList<GraphPath> newWP = new LinkedList<>();
                        for(GraphPath gp: wronglyPredicted)
                            newWP.add(gp);
                        ArrayList<GraphPath> newTP = new ArrayList<>();
                        for(GraphPath g : testedPaths)
                            newTP.add(g);
                        generateDiagnosis(diag, newWP, newTP);
                    }
                }
            }
        }
        else // apparently path was already solved
        {   // continue as if it was solved
            if(wronglyPredicted.isEmpty())
                        diagnosisList.add(diagOriginal.copy());
            else
            {
                LinkedList<GraphPath> newWP = new LinkedList<>();
                for(GraphPath gp: wronglyPredicted)
                    newWP.add(gp);
                ArrayList<GraphPath> newTP = new ArrayList<>();
                for(GraphPath g : testedPaths)
                    newTP.add(g);
                generateDiagnosis(diagOriginal.copy(), newWP, newTP);
            }
        }
    }
    
    /**
     * Simple system out print of all paths generated
     */
    public void printPaths()
    {
        System.out.println("=== Path overview ===");
        
        for(Observation o: observations)
        {
            System.out.print("Observation: " + o.startV.getName() + " to "
                        + o.endV.getName()+ "\n\n");
            if(obsPaths.containsKey(o))
            {
                LinkedHashSet<GraphPath> paths = obsPaths.get(o);
                if(!paths.isEmpty())
                {
                    for(GraphPath p : paths)
                    {
                        p.simplePrint();
                    }
                }
            }
        }
    }
    
    /**
     * Prints the discrepancies (if any) for each of the paths for a given observation
     * @param o Observation object used by the analyst
     */
    public void printWeights(Observation o)
    {
        System.out.println("=== Observation per path differences ===");
        if(obsPaths.containsKey(o))
        {
            LinkedHashSet<GraphPath> paths = obsPaths.get(o);
            for(GraphPath p: paths)
            {
                System.out.print("Path: " + p.getStepV(0).getName() + " to "
                        + p.getLastV().getName() + "\n\n");
                if(diffStore.containsKey(p))
                {
                    int[] bounds = diffStore.get(p);
                    System.out.println("Diff lb:" + bounds[0] + " ub:" + bounds[1]);
                }
            }
            if(DiagSTN.PRINTACC)
            {
                if(o.moreAccurate)
                    System.out.println("Observation is more accurate than prediction");
                else
                    System.out.println("Observation is less accurate than prediction");
            }
        }
    }
    
    /**
     * Prints all the different diagnosis on system out
     */
    public void printDiagnosis()
    {
        int iter = 1;
        System.out.println("=== Diagnosis overview ===");
        for(Diagnosis d : diagnosisList)
        {
            System.out.println("Diagnosis: " + iter);
            d.printDiagnosis();
            iter++;
        }
    }
    
    private void simplePaths(GraphPath graphPath, Observation obs)
    {
        // need to create new set for each new observation!
        LinkedHashSet<DEdge> edgeExp = graph.possibleEdges(graphPath.getLastV());
        if(edgeExp == null)
            return; // dead end!
        for(DEdge de : edgeExp)
        {
            if(de.getEnd().equals(obs.endV)) // end of edge equals end of observation ie. path is correct!
            {
                graphPath.addStep(de, de.getEnd());
                // SAVE
                LinkedHashSet<GraphPath> paths = obsPaths.get(obs.startV);
                if(paths == null) 
                {
                    paths = new LinkedHashSet();
                    obsPaths.put(obs, paths);
                }
                paths.add(graphPath.copy()); // save A COPY!
                
                graphPath.removeLast();
            }
            else if(!graphPath.edgeUsed(de)) // Edge has not been used yet
            {
                graphPath.addStep(de, de.getEnd());
                simplePaths(graphPath, obs); // use it!
                graphPath.removeLast();
            }
        }
    }
    
    /**
     * Either point has a fixed time or assume first Vertex is 0
     */
}

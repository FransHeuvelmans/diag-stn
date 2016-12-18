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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
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
    protected Graph graph;
    protected ArrayList<Observation> observations;
    protected Map<Vertex, Integer> fixedTimes; // add a 0/time point to any vertex
    protected Map<Observation, LinkedHashSet<GraphPath>> obsPaths;
    protected ArrayList<Diagnosis> diagnosisList;
    
    protected Map<GraphPath, int[]> diffStore; // Need to store changes before applying to the path
    
    protected LinkedHashSet<Observation> inconsistent;
    protected Map<GraphPath, int[]> predictions; // Just used for consistencyBasedDiag
    
    
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
        
        predictions = new HashMap();
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
        int lb, ub, deltalb, deltaub, sizeObs, sizePred;
        Integer strtVal;
        
        for(Observation o: observations)
        {
            sizeObs = o.endUb - o.endLb;
            LinkedHashSet<GraphPath> obPaths = obsPaths.get(o);
            GraphPath[] paths = obPaths.toArray(new GraphPath[obPaths.size()]);
            int[][] pathBounds = new int[paths.length][]; // for each of the paths
            int[] sizes = new int[paths.length];
            boolean[] smallerThanObs = new boolean[paths.length];
            // Quick store 4each: are the preds smalrthanobs
            boolean[] consistencyHazard = new boolean[paths.length];
            // Multi-path problem, extra output for user
            
            o.moreAccurate = false;
            o.fixneeded = false;
            
            if(paths != null)   // only if there are paths
            {
                int[] intersect = new int[2];
                intersect[0] = Integer.MIN_VALUE;
                intersect[1] = Integer.MAX_VALUE;
                for(int j = 0; j < paths.length; j++)  // lets do each path separate for now
                {
                    consistencyHazard[j] = false; // init!
                    
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
                    for(int i=1; i < paths[j].stepSize(); i++)
                    {
                        DEdge de = paths[j].getStepE(i);
                        lb += de.getLowerb();
                        ub += de.getUpperb();
                    }
                    int[] lbub = new int[2];
                    lbub[0] = lb;
                    lbub[1] = ub;
                    pathBounds[j] = lbub; // first just store the raw data
                    predictions.put(paths[j], lbub);
                    sizes[j] = ub - lb;
                    if(sizes[j] < sizeObs)
                        smallerThanObs[j] = true;
                    else
                    {
                        smallerThanObs[j] = false;
                        o.moreAccurate = true;
                    }
                    
                    intersect[0] = intersect[0] > lb ? intersect[0] : lb;
                    intersect[1] = intersect[1] < ub ? intersect[1] : ub;
                    if(((lb > intersect[1]) || (ub < intersect[0])) && !DiagSTN.IGNOREINCONSIST)
                    {
                        if(DiagSTN.PRINTWARNING)
                            System.out.println("Inconsistent path found!");
                        if(inconsistent == null)
                            inconsistent = new LinkedHashSet();
                        if(!inconsistent.contains(o))
                            inconsistent.add(o);
                    }
                }
                
                // comboLoop: if possible combo of predictions can become 
                // inconsistent with a certain change (ie. combined size < obs)
                for(int k = 0; k < paths.length; k++)
                {
                    for(int l = k + 1; l < paths.length; l++)
                    {
                        if(smallerThanObs[k] && smallerThanObs[l])
                        {
                            if((sizes[k] + sizes[l]) < sizeObs)
                            {
                                consistencyHazard[k] = true;
                                consistencyHazard[l] = true;
                            }
                        }
                    }
                }
                
                //redone loop
                int[] change = new int[2];
                for(int m = 0; m < paths.length; m++)
                {
                    deltalb = o.endLb - pathBounds[m][0];
                    deltaub = o.endUb - pathBounds[m][1];
                    
                    change[0] = Math.min(deltalb, deltaub);
                    change[1] = Math.max(deltalb, deltaub);
                    
                    if(change[0] != 0 || change[1] != 0)
                        o.fixneeded = true;
                    
                    diffStore.put(paths[m], change.clone());
//                    System.out.println("For Observation " + o.startV.getID() +
//                            " to " + o.endV.getID());
//                    System.out.println("Storing obsLB:" + o.endLb + 
//                            " pathLB:" + pathBounds[m][0] +
//                            " obsUB:" + o.endUb +
//                            " pathUB:" + pathBounds[m][1]);
                    for(int n=1; n < paths[m].stepSize(); n++)
                    {
                        DEdge de = paths[m].getStepE(n);
                        de.addPossibleChange(change[0], change[1]); 
                        // store the possible changes
                        if(consistencyHazard[m])
                            de.setPossibleConProblem(true);
                    }
                }
            }
            else
            {
                System.out.println("Observation with no paths tried propagation");
                // No paths in the LinkedHashSet paths that is!
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
        // First repair possible inconsistent observations
        if(!DiagSTN.IGNOREINCONSIST && inconsistent != null) 
        {
            if(DiagSTN.PRINTWARNING)
                System.out.println("Found inconsistent paths, will proceed with"
                        + " repair suggestions");
            Iterator<Observation> incIter = inconsistent.iterator();
            while(incIter.hasNext())
            {
                Observation incObs = incIter.next();
                LinkedHashSet<GraphPath> pathSet = obsPaths.get(incObs);
                for(GraphPath p : pathSet)
                    needDiag.add(p);
                // TODO: fix some observation here!!!!
                
            }
            
            // for now lets not do both -> fix inconsistencies before
            // trying to fix a normal network (see example) because inconsistency
            // might point to a faulty model (use ignoreInconsistency)
        }
        else
        {
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
        }
        /**
         * CHECK: if no fix is needed the values shouldnt be allowed to change!!
         * Is it caught with the [0,0] bounds added to the edges of that observ.?
         */
        generateDiagnosis(new Diagnosis(), needDiag, new ArrayList<>());
        return diagnosisList.toArray(new Diagnosis[diagnosisList.size()]);
    }
    
    /* Underlying generateDiagnosis method used to recursively traverse the tree
     * of problem edges */ 
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
     * Generate all consistency based diagnoses (without fault model)
     * @return ConDiagnosis object array 
     */
    public ConDiagnosis[] generateConDiagnosis()
    {
        LinkedList<GraphPath> fDiagnoses = new LinkedList();
        LinkedList<GraphPath> tDiagnoses = new LinkedList();
        for(Observation ob : observations)
        {
            // for each obs -> for each path on that obs
            // see if obs falls outside of pred
            for(GraphPath obPath : obsPaths.get(ob))
            {
                int[] pred = predictions.get(obPath);
                int obslb = ob.endLb;
                int obsub = ob.endUb;
                if(obslb < pred[1] && obsub > pred[0])
                    tDiagnoses.add(obPath);
                else
                    fDiagnoses.add(obPath);
            }
        }
        System.out.println("False diagnoses: " + fDiagnoses.size());
        ConDiagnosis empty = new ConDiagnosis();
        for(GraphPath truePath : tDiagnoses)
        {
            for(int i = 1; i < truePath.stepSize(); i++)
                empty.addCorrectEdge(truePath.getStepE(i));
        }
        LinkedList<ConDiagnosis> conDiagnoses = generateConDiagnosis(empty,
                fDiagnoses, new LinkedList());
        // then loop trough all possibilities!
        // How ? -> zelfde manier als normale generateDiag...
        
        return conDiagnoses.toArray(new ConDiagnosis[conDiagnoses.size()]);
    }
    
    /* Underlying generateConDiagnosis method used to recursively traverse the 
    tree of problem edges for a consistency based diagnosis */ 
    private LinkedList<ConDiagnosis> generateConDiagnosis(ConDiagnosis conDiagOri, 
            LinkedList<GraphPath> faultyPaths , LinkedList<GraphPath> testedPaths)
    {
        LinkedList<ConDiagnosis> allFullDiag = new LinkedList();
        if(faultyPaths.isEmpty())
            return allFullDiag;
        GraphPath path = faultyPaths.pop();    
        testedPaths.add(path); // lost of paths that are already solved!
        
        if(!conDiagOri.pathSolved(path)) // if path is not solved try to solve it
        {
            // ie. if in the current diagnosis an edge has already been solved on the current path
            for(int i = 1; i < path.stepSize(); i++)
            {
                DEdge edge = path.getStepE(i);  // for EACH edge on this path!
                ConDiagnosis condiag = conDiagOri.copy(); // new Diagnosis

                //check if edge is part of any of the testedPaths, if so 
                // it can't be used
                boolean alreadySolved = false;
                if(!testedPaths.isEmpty())
                {
                    for(int j = 0; j < testedPaths.size()-1;j++) // shouldnt check the last added Path (ie. the current Path!!)
                    {
                        GraphPath p = testedPaths.get(j);
                        if(p.edgeUsed(edge))
                            alreadySolved = true; // can't use this edge in current diagnosis 
                    }                           // already part of solved path
                }
                if(alreadySolved)
                    continue;
                if(condiag.edgeCorrect(edge) || condiag.edgeSolved(edge))
                    continue;
                
                condiag.addFaultyEdge(edge);
                if(faultyPaths.isEmpty())
                    allFullDiag.add(condiag);
                else
                {
                    LinkedList<GraphPath> newWP = new LinkedList<>();
                    for(GraphPath gp: faultyPaths)
                        newWP.add(gp);
                    LinkedList<GraphPath> newTP = new LinkedList<>();
                    for(GraphPath g : testedPaths)
                        newTP.add(g);
                    LinkedList<ConDiagnosis> out = generateConDiagnosis(condiag, newWP, newTP);
                    for(ConDiagnosis cd : out)
                        allFullDiag.add(cd);
                }
            }
        }
        else // apparently path was already solved
        {   // continue as if it was solved
            if(faultyPaths.isEmpty())
                allFullDiag.add(conDiagOri.copy());
            else
            {
                LinkedList<GraphPath> newWP = new LinkedList<>();
                for(GraphPath gp: faultyPaths)
                    newWP.add(gp);
                LinkedList<GraphPath> newTP = new LinkedList<>();
                for(GraphPath g : testedPaths)
                    newTP.add(g);
                LinkedList<ConDiagnosis> out = generateConDiagnosis(conDiagOri.copy(), newWP, newTP);
                for(ConDiagnosis cd : out)
                    allFullDiag.add(cd);
            }
        }
        return allFullDiag;
    }
    
    /**
     * Simple System.out print of all paths generated.
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
                p.simplePrint();
                if(diffStore.containsKey(p))
                {
                    int[] bounds = diffStore.get(p);
                    System.out.println("Change between lb:" + bounds[0] + " ub:" + bounds[1]);
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
     *  Print weights for all observations.
     */
    public void printWeights()
    {
        for(Observation o: observations)
        {
            if(o.fixneeded)
                printWeights(o);
            // might need further testing for valid observations
        }
    }
    
    /**
     * Prints all the different diagnosis on System.out.
     */
    public void printDiagnosis()
    {
        Collections.sort(diagnosisList);
        int iter = 1;
        System.out.println("=== Diagnosis overview ===");
        for(Diagnosis d : diagnosisList)
        {
            System.out.println("Diagnosis: " + iter);
            d.printDiagnosis();
            iter++;
        }
    }
    
    /**
     * Number of diagnoses answer intervals
     * @return Integer with number of output lines, each representing an interval
     */
    public int diagSize()
    {
        return diagnosisList.size();
    }
    
    /* Simple method for generating all the paths for some observation given 
    some traversed path. Stores them to obsPaths */
    private void simplePaths(GraphPath graphPath, Observation obs)
    {
        // need to create new set for each new observation!
        LinkedHashSet<DEdge> edgeExp = graph.possibleEdges(graphPath.getLastV());
        if(edgeExp == null || (edgeExp.size() < 1))
            return; // dead end!
        for(DEdge de : edgeExp)
        {
            if(de.getEnd().equals(obs.endV)) // end of edge equals end of observation ie. path is correct!
            {
                graphPath.addStep(de, de.getEnd());
                // SAVE
                LinkedHashSet<GraphPath> paths = obsPaths.get(obs);
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

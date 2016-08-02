/*
 * Copyright 2016 frans.
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
import diag.stn.STN.DEdge;
import diag.stn.STN.Graph;
import diag.stn.STN.Observation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * Analyst for consistency based diagnosis with the use of a fault model similar
 * to MAC diagnosis. 
 * @author frans
 */
public class ConAnalyst extends Analyst
{

    public ConAnalyst(Graph g)
    {
        super(g);
    }

    @Override
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
                
                //redone loop
                int[] change = new int[2];
                for(int m = 0; m < paths.length; m++)
                {
                    deltalb = o.endLb - pathBounds[m][1]; 
                    deltaub = o.endUb - pathBounds[m][0];
                    /**
                     * For consistent based diag this is turned around: 
                     * meaning that as soon as the pred and obs overlap, it is
                     * consistent and it is a valid answer
                     */
                    
                    change[0] = Math.min(deltalb, deltaub);
                    change[1] = Math.max(deltalb, deltaub);
                    
                    if(change[0] != 0 || change[1] != 0)
                        o.fixneeded = true;
                    
                    diffStore.put(paths[m], change.clone());
                    for(int n=1; n < paths[m].stepSize(); n++)
                    {
                        DEdge de = paths[m].getStepE(n);
                        de.addPossibleChange(change[0], change[1]); 
                        // store the possible changes
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
    
    @Override
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
                ArrayList<int[]> changes = edge.getPossibleConChanges();
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
}

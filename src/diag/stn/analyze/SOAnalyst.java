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

import diag.stn.analyze.GraphPath;
import diag.stn.analyze.Diagnosis;
import diag.stn.STN.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Single Origin Analyst. An analyst that computes the diagnosis faster by
 * using the property of a system that there is only a single (starting) point
 * from which the observations are made. (Examples include when using a clock
 * to sync all observations)
 * @author Frans van den Heuvel
 */
public class SOAnalyst extends Analyst
{
    HashMap<GraphPath,Boolean> smallerThanObs;
    HashMap<GraphPath,Boolean> consistencyHazard;
    HashMap<GraphPath,Integer> predSizes;
    
    public SOAnalyst(Graph g)
    {
        super(g);
    }
    
    @Override
    public void addObservation(Observation ob)
    {
        if(!observations.isEmpty())
        {
            if(!ob.startV.equals(observations.get(0).startV))
            {
                System.err.println("Observations have different starting points");
                System.err.println("Use other Analyst or transform the problem");
                return;
            }
        }
        observations.add(ob);
    }
    
    @Override
    public void generatePaths()
    {
        /**
         * Combines the generate paths and propagate weights of the standard 
         * analyst
         */
        if(observations.isEmpty())
        {
            System.err.println("No observations added before calculation");
            return;
        }
        Observation ob = observations.get(0);
        for(int i = 1; i < observations.size(); i++)
        {
            Observation otherob = observations.get(i);
            if(ob.startV != otherob.startV)
            {
                System.err.println("Observations have "
                        + "different starting vertices");
            }
        }
        
        smallerThanObs = new HashMap();
        consistencyHazard = new HashMap();
        predSizes = new HashMap();
        
        GraphPath g = new GraphPath(ob.startV);
        Integer strtVal = fixedTimes.get(ob.startV); // lets hope that o.startV == p.getStepV(0)
        int lb,ub;
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
        pathCalc(g, lb, ub); // here the propagation occurs
        
        for(Observation o : observations)
        {
            o.moreAccurate = false;
            ob.fixneeded = false;
            int[] intersect = new int[2];
            
            LinkedHashSet<GraphPath> pathsSet = obsPaths.get(o);
            if(pathsSet == null) // prolly wont be filled anyway
                continue;
            GraphPath[] paths = pathsSet.toArray(new GraphPath[pathsSet.size()]);
            if(paths == null) 
                continue;
            for(int k = 0; k < paths.length; k++)
            {
                // iterate over combinations to see if there might be a problem
                if(paths.length > 1)
                {
                    for(int l = k + 1; l < paths.length; l++)
                    {
                        if(smallerThanObs.get(paths[k]) && smallerThanObs.get(paths[l]))
                        {
                            if((predSizes.get(paths[k]) + predSizes.get(paths[l])) < (o.endUb - o.endLb))
                            {
                                consistencyHazard.put(paths[k], Boolean.TRUE);
                                consistencyHazard.put(paths[l], Boolean.TRUE);
                            }
                            else
                            {
                                consistencyHazard.put(paths[k], Boolean.FALSE);
                                consistencyHazard.put(paths[l], Boolean.FALSE);
                            }
                        }
                        else
                        {
                            consistencyHazard.put(paths[k], Boolean.FALSE);
                            consistencyHazard.put(paths[l], Boolean.FALSE);
                        }
                    }
                }
                else
                    consistencyHazard.put(paths[k], Boolean.FALSE);
                
                int[] lbub = diffStore.get(paths[k]);
                int deltalb = o.endLb - lbub[0];
                int deltaub = o.endUb - lbub[1];
                int[] chng = new int[2];
                chng[0] = Math.min(deltalb, deltaub);
                chng[1] = Math.max(deltalb, deltaub);
                if(chng[0] != 0 || chng[1] != 0)
                    o.fixneeded = true;
                for(int n=1; n < paths[k].stepSize(); n++)
                {
                    DEdge de = paths[k].getStepE(n);
                    de.addPossibleChange(chng[0], chng[1]); 
                    // store the possible changes
                    if(consistencyHazard.get(paths[k]))
                        de.setPossibleConProblem(true);
                }
            }
            
        }
        // TODO: Combination of changes for the different paths 
        // of an observation here (see Analyst for now)
    }
    
    /***
     * @deprecated Is no longer needed for SOAnalyst when generatePaths is used
     */
    public void propagateWeights()
    {
        System.out.println("SOAnalist already propagates the weights when"
                + " generating the paths");
    }
    
    private void pathCalc(GraphPath g, int lb, int ub)
    {
        int dlb,dub;
        // combine generatePaths & propagateWeights
        LinkedHashSet<DEdge> edgeExp = graph.possibleEdges(g.getLastV());
        
         if(edgeExp == null)
            return; // dead end!
        for(DEdge de : edgeExp)
        {
            dlb = 0;
            dub = 0;
            dlb = de.getLowerb() + lb;
            dub = de.getUpperb() + ub;
            for(Observation o : observations)
            {
                if(de.getEnd().equals(o.endV))
                {
                    g.addStep(de, de.getEnd());
                    LinkedHashSet<GraphPath> paths = obsPaths.get(o);
                    if(paths == null) 
                    {
                        paths = new LinkedHashSet();
                        obsPaths.put(o, paths);
                    }
                    GraphPath copyPath = g.copy(); // Need to use a copy from now
                    paths.add(copyPath);
                    int[] lbub = new int[2];
                    lbub[0] = dlb;
                    lbub[1] = dub;
                    int predSize = dub-dlb;
                    predSizes.put(copyPath, predSize);
                    if(predSize < (o.endUb - o.endLb))
                        smallerThanObs.put(copyPath, Boolean.TRUE);
                    else
                        smallerThanObs.put(copyPath, Boolean.FALSE);

                    diffStore.put(copyPath, lbub);
                    // !!!! WARNING, STILL NEEDS CODE TO COMBINE CHANGES
                    // TO A PROPER lbub (for all paths on 1 obs)
                }
            }
            if(!g.edgeUsed(de))  // shouldn't be part of current path(takes)
            {
                g.addStep(de, de.getEnd());
                pathCalc(g,dlb,dub);
            }
            if(g.stepSize() > 1)
                g.removeLast();
        }
        
        /**
         * When storing use the observation that needs to be found with the 
         * combination of starting vertex and ending vertex!
         */
    }
    
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
}

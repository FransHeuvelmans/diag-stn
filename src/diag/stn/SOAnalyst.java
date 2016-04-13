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
package diag.stn;

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
 * @author frans
 */
public class SOAnalyst
{
    private Graph graph;
    private ArrayList<Observation> observations;
    private Map<Vertex, Integer> fixedTimes; // add a 0/time point to any vertex
    private Map<Observation, LinkedHashSet<GraphPath>> obsPaths;
    private ArrayList<Diagnosis> diagnosisList;
    
    private Map<GraphPath, int[]> diffStore;
    
    public SOAnalyst(Graph g)
    {
        observations = new ArrayList<>();
        fixedTimes = new HashMap<>();
        obsPaths = new HashMap<>(); 
        diffStore = new HashMap<>(); 
        diagnosisList = new ArrayList<>();
        graph = g;
    }
    
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
    
    public void addFixedTime(Vertex v, int i)
    {
        if(fixedTimes.containsKey(v))
        {
            System.err.println("Tried to add a fixed time point to a vertex that has one");
            return;
        }
        fixedTimes.put(v, i);
    }
    
    public void calculatePaths()
    {
        /**
         * Combines the generate paths and propagate weights of the standard 
         * analyst
         */
        
    }
    
    private void pathCalc(GraphPath g, int lb, int ub)
    {
        // combine generatePaths & propagateWeights
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
        }
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

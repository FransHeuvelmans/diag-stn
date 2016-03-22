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
import  java.lang.Math;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author frans
 */
public class Analyst
{
    private Graph graph;
    private ArrayList<Observation> observations;
    private Map<Vertex, Integer> fixedTimes; // add a 0/time point to any vertex
    private Map<Observation, LinkedHashSet<GraphPath>> obsPaths;
    private ArrayList<Diagnosis> diagnosisList;
    
    private Map<GraphPath, int[]> diffStore; // Just 4 testing/debugging the propagated diffs.
    
    
    // Each observation might have multiple paths connected
    
    public Analyst(Graph g)
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
    
    public void propagateWeights()
    {
        // For now first use the stored paths and calculate ALL paths
        int lb, ub, deltalb, deltaub, changelb, changeub;
        Integer strtVal;
        
        for(Observation o: observations)
        {
            LinkedHashSet<GraphPath> paths = obsPaths.get(o);
            int[] checkBounds = null;
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
                    deltalb = o.endLb - lb; // lets keep this order and use no abs!
                    deltaub = o.endUb - ub;
                    
                    changelb = Math.min(deltalb, deltaub);
                    changeub = Math.max(deltalb, deltaub);
                    int[] chng = new int[2]; //test & check code
                    chng[0] = changelb;
                    chng[1] = changeub;
                    
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
                    diffStore.put(p, chng);
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
                }
            }
        }
    }
    
    public Diagnosis[] generateDiagnosis()
    {
        Deque<GraphPath> needDiag = new LinkedList<>();
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
    
    private void generateDiagnosis(Diagnosis diag, Deque<GraphPath> wronglyPredicted, ArrayList<GraphPath> testedPaths)
    {
        if(wronglyPredicted.isEmpty())
            return;
        GraphPath path = wronglyPredicted.pop();    
        testedPaths.add(path);
        if(!diag.edgeUsed(path))
        {
            for(int i = 1; i < path.stepSize(); i++)
            {
                DEdge edge = path.getStepE(i);  // for EACH edge on this path!
                // if combine possible
                boolean combine = true;
                ArrayList<int[]> changes = edge.getPossibleChanges();
                int[] finalchng = changes.remove(changes.size());
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
                        generateDiagnosis(diag, wronglyPredicted, testedPaths);
                }
            }
        }
    }
    
    public void printPaths()
    {
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
            else if(!graphPath.edgeUsed(de))
            {
                graphPath.addStep(de, de.getEnd());
                simplePaths(graphPath, obs);
                graphPath.removeLast();
            }
        }
    }
    
    /**
     * Either point has a fixed time or assume first Vertex is 0
     */
}

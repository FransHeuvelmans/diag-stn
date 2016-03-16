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
import java.util.Map;

/**
 *
 * @author frans
 */
public class Analist
{
    private Graph graph;
    private ArrayList<Observation> observations;
    private Map<Vertex, Integer> fixedTimes; // add a 0/time point to any vertex
    private Map<Observation, LinkedHashSet<GraphPath>> obsPaths;
    // Each observation might have multiple paths connected
    
    public Analist(Graph g)
    {
        observations = new ArrayList<>();
        fixedTimes = new HashMap<>();
        obsPaths = new HashMap<>(); 
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
    
    public void printPaths()
    {
        for(Observation o: observations)
        {
            if(obsPaths.containsKey(o))
            {
                System.out.print("Observation: " + o.startV.getName() + " to "
                        + o.endV.getName()+ "\n\n");
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
    
    private void simplePaths(GraphPath graphPath, Observation obs)
    {
        // need to create new set for each new observation!
        LinkedHashSet<DEdge> edgeExp = graph.possibleEdges(graphPath.getLastV());
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

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

import diag.stn.STN.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A Diagnosis of a Graph. Is a full or partial diagnosis of the model.
 * Is build by a graph analyst
 * @author Frans van den Heuvel
 */
public class Diagnosis
{
    private ArrayList<DEdge> edges;
    private Map<DEdge, int[]> changes;
    
    /**
     * Create new empty diagnosis
     */
    public Diagnosis()
    {
        edges = new ArrayList<>();
        changes = new HashMap<>();
    }
    
    /**
     * Adds a partial diagnosis, meaning some sort of "fix" of a path by changing
     * an edge on that path with a certain amount.
     * @param edge the directed edge object that needs changing to fix some path
     * @param lowerbound integer with the lower bound on the change needed
     * @param upperbound integer with the upper bound on the change needed
     */
    public void addPartial(DEdge edge, int lowerbound, int upperbound)
    {
        int[] bound = new int[2];
        bound[0] = lowerbound;
        bound[1] = upperbound;
        edges.add(edge);
        changes.put(edge, bound);
    }
    
    /**
     * Check if an edge is used in any of the partial diagnosis of this full diagnosis
     * @param edge directed edge object used (must be same reference/exact object)
     * @return boolean true if edge is used, false if not
     */
    public boolean edgeUsed(DEdge edge)
    {
        return edges.contains(edge);
    }
    
    /**
     * Check if any of the edges in given path are used for any partial diagnosis 
     * @param path A path with edges that need to be checked
     * @return boolean true if any of the edge are used, false if none are.
     */
    public boolean edgeUsed(GraphPath path)
    {
        boolean used = false;
        for(int i = 1; i < path.stepSize(); i++)
        {
            if(edges.contains(path.getStepE(i)))
                return true;
        }
        return false;
    }
    
    /**
     * A simple print diagnosis to system out by printing the vertex start+end and
     * the change in [lowerbound,upperbound] for all the partial diagnosis in this full
     * diagnosis
     */
    public void printDiagnosis()
    {
        System.out.print("Delta = {");
        for(DEdge de: edges)
        {
            int[] chngs = changes.get(de);
            if(chngs[0] != 0 || chngs[1] != 0) // The System can be simply correct and no change needed
            {
                System.out.print("d" + de.getStart().getName() + "," + de.getEnd().getName());
                System.out.print(" \u2208 [" + chngs[0] + "," + chngs[1] + "] ");
            }
        }
        System.out.print("d-rest = [0,0]}\n");
    }
    
    /**
     * A copy method that creates a new Diagnosis. Uses the same directed
     * edge object references.
     * @return Diagnosis object copy of this original
     */
    public Diagnosis copy()
    {
        Diagnosis cpy = new Diagnosis();
        for(DEdge d: edges)
        {
            int[] chngs = changes.get(d);
            cpy.addPartial(d, chngs[0], chngs[1]);
        }
        return cpy;
    }
}

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
import java.util.Map;

/**
 * Can contain a full or partial diagnosis of the model.
 * Is usually build by some analyst
 * @author frans
 */
public class Diagnosis
{
    ArrayList<DEdge> edges;
    Map<DEdge, int[]> changes;
    
    public Diagnosis()
    {
        edges = new ArrayList<>();
        changes = new HashMap<>();
    }
    
    public void addPartial(DEdge edge, int lowerbound, int upperbound)
    {
        int[] bound = new int[2];
        bound[0] = lowerbound;
        bound[1] = upperbound;
        edges.add(edge);
        changes.put(edge, bound);
    }
    
    public boolean edgeUsed(DEdge edge)
    {
        return edges.contains(edge);
    }
    
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
}

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

/**
 *
 * @author frans
 */
public class GraphPath
{
    private ArrayList<Vertex> vertices;
    private ArrayList<DEdge> edges;
    
    public GraphPath(Vertex start)
    {
        vertices = new ArrayList<>();
        edges = new ArrayList<>();
        vertices.add(start);
    }
    
    public void addStep(DEdge edg, Vertex next)
    {
        edges.add(edg);
        vertices.add(next);
    }
    
    public Vertex getStepV(int step)
    {
        return vertices.get(step);
    }
    
    public DEdge getStepE(int step)
    {
        return edges.get(step - 1);
    }
    
    public Vertex getLastV()
    {
        return vertices.get(vertices.size()-1);
    }
    
    public int stepSize()
    {
        return vertices.size();
    }
    
    public void removeLast()
    {
        vertices.remove(vertices.size() - 1);
        edges.remove(edges.size() - 1);
    }
    
    public boolean edgeUsed(DEdge de)
    {
        return edges.contains(de); // if contains then used !
    }
    
    public GraphPath copy()
    {
        GraphPath newGP = new GraphPath(vertices.get(0));
        for(int i = 1; i < vertices.size(); i++)
        {
            newGP.addStep(getStepE(i), getStepV(i));
        }
        /**
         * Do not (deep) copy the edges and vertices because they need 
         * to reference the right/same objects
         */
        return newGP;
    }
    
    public void simplePrint()
    {
        System.out.print(vertices.get(0).getName());
        for(int i = 1; i < vertices.size(); i++)
        {
            System.out.print(" -> " + vertices.get(i).getName());
        }
        System.out.print("\n");
    }
}

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

/**
 * A path in the Graph. Consists of a series of nodes in a certain order and the
 * edges used to traverse these nodes in order.
 * @author Frans van den Heuvel
 */
public class GraphPath
{
    private ArrayList<Vertex> vertices;
    private ArrayList<DEdge> edges;
    
    /**
     * Construct an empty Graph with only the starting vertex.
     * @param start Vertex object used as start of the path.
     */
    public GraphPath(Vertex start)
    {
        vertices = new ArrayList<>();
        edges = new ArrayList<>();
        vertices.add(start);
    }
    /**
     * Add a step to the path. Done by adding a vertex reachable by an edge
     * @param edg directed edge object used for reaching new vertex
     * @param next vertex object that is the next step in the path
     */
    public void addStep(DEdge edg, Vertex next)
    {
        edges.add(edg);
        vertices.add(next);
    }
    
    /**
     * Get a certain (vertex) step on the path
     * @param step the step number. Starts from 0 (start) to size-1 / # steps added
     * @return a Vertex object
     */
    public Vertex getStepV(int step)
    {
        return vertices.get(step);
    }
    
    /**
     * Get a certain (edge) step on the path. returns the edge used to get to the
     * vertex on that step.
     * @param step the step number. Starts from 1 (start) to size-1 / # steps added
     * @return a Vertex object
     */
    public DEdge getStepE(int step)
    {
        return edges.get(step - 1);
    }
    
    /**
     * Quick method for returning the last added vertex
     * @return vertex object last added to the path
     */
    public Vertex getLastV()
    {
        return vertices.get(vertices.size()-1);
    }
    
    /**
     * Get the size of the path in vertices
     * @return full number of vertices in the path (including starting vertex so
     * # added vertices + 1)
     */
    public int stepSize()
    {
        return vertices.size();
    }
    
    /**
     * Quick method for removing the last vertex and edge used. Removes last addstep.
     */
    public void removeLast()
    {
        vertices.remove(vertices.size() - 1);
        edges.remove(edges.size() - 1);
    }
    
    /**
     * Method for checking if a certain edge is used in the graph
     * @param de a directed edge object that is used in the graph (must be exact object)
     * @return boolean value true if used, false if not
     */
    public boolean edgeUsed(DEdge de)
    {
        return edges.contains(de); // if contains then used !
    }
    
    /**
     * Quick method for returning the edges in order used by this graphpath
     * @return Array of DEdge objects
     */
    public DEdge[] toEdges()
    {
        return edges.toArray(new DEdge[edges.size()]);
    }
    
    /**
     * A copy value that creates a new GraphPath but uses the same object 
     * references inside the new GraphPath object. (shallow copy)
     * @return new GraphPath object that has the same references.
     */
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
    
    /**
     * A basic print function, prints all the (vertex)names of the path in order.
     */
    public void simplePrint()
    {
        System.out.print(vertices.get(0).getName());
        for(int i = 1; i < vertices.size(); i++)
        {
            DEdge e = getStepE(i);
            System.out.print(" -[" + e.getLowerb() + "," + e.getUpperb() + "]-> ");
            System.out.print(vertices.get(i).getName());
        }
        System.out.print("\n");
    }
    
    public void smallPrint()
    {
        System.out.print(vertices.get(0).getName());
        for(int i = 1; i < vertices.size(); i++)
        {
            DEdge e = getStepE(i);
            System.out.print("-");
            System.out.print(vertices.get(i).getName());
        }
        System.out.print("\n");
    }
    
    /**
     * A basic check if 2 GraphPaths use the same vertices (v with the same id) 
     * @param other
     * @return 
     */
    public boolean isSimilar(GraphPath other)
    {
        if(other.vertices.size() != this.vertices.size())
            return false;
        for(int i = 0 ; i < vertices.size(); i++)
        {
            if(this.vertices.get(i).getID() !=
                    other.vertices.get(i).getID())
                return false;
        }
        return true;
    }
}

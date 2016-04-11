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
package diag.stn.STN;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Stores the full graph for solving STN. Only the basic Graph representation, no
 * other time or other information.
 * @author Frans van den Heuvel
 */
public class Graph
{
    private LinkedHashSet<Vertex> nodes;
    private LinkedHashSet<DEdge> edges;
    private Map<Vertex, LinkedHashSet<DEdge>> map = new HashMap(); 
    // Extra map with all the edges from a Vertex
    
    /**
     * Generate empty Graph
     */
    public Graph()
    {
        nodes = new LinkedHashSet<>();
        edges = new LinkedHashSet<>();
        
        // a hashmap which gives all edges which start at
        // a particular node
        map = new HashMap();
    }
    
    /**
     * Create a new edge and add it to the Graph
     * @param start Starting vertex of the edge
     * @param end Ending vertex
     * @param lowerbound lower bound on the time needed (cost)
     * @param upperbound upper bound on the time needed (cost) 
     */
    public void addEdge(Vertex start, Vertex end, int lowerbound, int upperbound)
    {
        // check if vertices exist! (are part of the network)
        if(!nodes.contains(start) || !nodes.contains(end))
        {
            System.err.println("Edge cannot be added, one of the vertices is not part of the network");
            return;
        }
        DEdge e = new DEdge(start, end, lowerbound, upperbound);
        edges.add(e);
        LinkedHashSet<DEdge> adjacent = map.get(start);
        if(adjacent==null) {
            adjacent = new LinkedHashSet();
            map.put(start, adjacent);
        }
        adjacent.add(e);
    }
    
    /**
     * Adds a Vertex to the Graph
     * @param v Vertex object created outside the Graph
     */
    public void addVertex(Vertex v)
    {
        nodes.add(v);
    }
    
    /**
     * Find all vertices that can be reached in 1 step from given Vertex
     * @return LinkedList with all Vertices near
     */
    public LinkedList<Vertex> adjacentNodes(Vertex last)
    {
        LinkedHashSet<DEdge> edges = map.get(last);
        LinkedList<Vertex> adjacent = new LinkedList<>();
        for(DEdge edg : edges)
        {
            adjacent.add(edg.getEnd());
        }
        return adjacent;
    }
    
    /**
     * What possible edges can be used from given Vertex
     */
    public LinkedHashSet<DEdge> possibleEdges(Vertex v)
    {
        return map.get(v); // warning, can return null!
    }
    
    /**
     * Is there an edge going from 1 vertex to the other
     * @param fro Staring Vertex
     * @param to Destination/ending Vertex
     */
    public boolean directReach(Vertex fro, Vertex to)
    {
        Set<DEdge> edges = map.get(fro);
        if(edges==null) {
            return false;
        }
        for(DEdge edg : edges)
        {
            if(edg.getEnd().equals(to)) // ? need similar ??
                return true;
        }
        return false;
    }
    
    /**
     * Returns first Vertex with a certain name
     * @param name String with the name of the Vertex
     * @return the Vertex OR null !
     */
    public Vertex getVertex(String name)
    {
        for(Vertex v : nodes)
        {
            if(v.getName().equals(name))
                return v;
        }
        System.err.println("Vertex not found, name: " + name);
        return null;
    }
    
    /**
     * Returns the Vertex with a certain ID value
     * @param id integer for identification
     * @return Vertex object or null if no vertex was found
     */
    public Vertex getVertex(int id)
    {
        for(Vertex v : nodes)
        {
            if(v.getID() == id)
                return v;
        }
        System.err.println("Vertex not found, id: " + id);
        return null;
    }
}
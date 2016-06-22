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
    private Map<Vertex, LinkedHashSet<DEdge>> reverseMap;
    // Extra map with all the edges from a Vertex
    
    private boolean checkNegativeEdges;
    
    /**
     * Generate empty Graph
     */
    public Graph()
    {
        nodes = new LinkedHashSet<>();
        edges = new LinkedHashSet<>();
        
        checkNegativeEdges = true;
        
        // a hashmap which gives all edges which start at
        // a particular node
        map = new HashMap();
        reverseMap = new HashMap();
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
        addEdge(start,end,lowerbound,upperbound,false);
    }
    
    /**
     * Create a new edge and add it to the Graph
     * @param start Starting vertex of the edge
     * @param end Ending vertex
     * @param lowerbound lower bound on the time needed (cost)
     * @param upperbound upper bound on the time needed (cost) 
     * @param cont True if the edge must not change, false if it is a normal edge
     */
    public void addEdge(Vertex start, Vertex end, int lowerbound, int upperbound, boolean cont)
    {
        // check if vertices exist! (are part of the network)
        if(!nodes.contains(start) || !nodes.contains(end))
        {
            System.err.println("Edge cannot be added, one of the vertices is not part of the network");
            return;
        }
        if(lowerbound > upperbound)
            System.err.println("incorrect bounds: lb > ub");
        
        
        // Negative edges can be reversed in STN
        if(checkNegativeEdges && (lowerbound < 0 && upperbound < 0))
        {
            Vertex temp = start;
            start = end;
            end = temp;
            int tub = upperbound;
            int tlb = lowerbound;
            lowerbound = Math.abs(tub);
            upperbound = Math.abs(tlb);
        }
        
        
        DEdge e = new DEdge(start, end, lowerbound, upperbound);
        if(cont)
            e.makeContigent();
        edges.add(e);
        LinkedHashSet<DEdge> adjacent = map.get(start);
        if(adjacent==null) 
        {
            adjacent = new LinkedHashSet();
            map.put(start, adjacent);
        }
        LinkedHashSet<DEdge> incoming = reverseMap.get(end);
        if(incoming == null)
        {
            incoming = new LinkedHashSet();
            reverseMap.put(end, incoming);
        }
        adjacent.add(e);
        incoming.add(e);
    }
    
    public boolean removeEdge(DEdge de)
    {
        if(edges.contains(de))
        {
            edges.remove(de);
            LinkedHashSet x = map.remove(de.getStart());
            LinkedHashSet y = reverseMap.remove(de.getEnd());
            if(x == null || y == null)
                System.err.println("Edge wasnt fully dialed in on removal");
            return true;
        }
        else
        {
            return false;
        }
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
     * Removes Vertex and all its edges from the graph
     * @param v Vertex object that needs removal
     * @return Vertex was present in graph or not
     */
    public boolean removeVertex(Vertex v)
    {
        boolean out = nodes.remove(v);
        if(!out)
            return false;
        if(map.containsKey(v))
        {
            LinkedHashSet<DEdge> edgz = map.get(v);
            for(DEdge e: edgz)
            {
                edges.remove(e);
            }
            map.remove(v);
        }
        if(reverseMap.containsKey(v))
        {
            LinkedHashSet<DEdge> edgz = reverseMap.get(v);
            for(DEdge e: edgz)
            {
                edges.remove(e);
            }
            reverseMap.remove(v);
        }
        return true;
    }
    
    public void reverseNegativeEdge(boolean checkForNeg)
    {
        checkNegativeEdges = checkForNeg;
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
     * Out degree. How many edges come from this Vertex to some other Vertex
     * @param fro Vertex
     * @return integer with # edges
     */
    public int outDegree(Vertex fro)
    {
        LinkedHashSet<DEdge> edges = map.get(fro);
        return edges.size();
    }
    
    public LinkedList<Vertex> incomingNodes(Vertex to)
    {
        LinkedHashSet<DEdge> edges = reverseMap.get(to);
        LinkedList<Vertex> from = new LinkedList<>();
        for(DEdge edg : edges)
        {
            from.add(edg.getEnd());
        }
        return from;
    }
    
    /**
     * In degree. How many edges come to this Vertex from some other Vertex
     * @param to Vertex 
     * @return integer with # edges
     */
    public int inDegree(Vertex to)
    {
        LinkedHashSet<DEdge> edges = reverseMap.get(to);
        return edges.size();
    }
    
    /**
     * What possible edges can be used from given Vertex
     */
    public LinkedHashSet<DEdge> possibleEdges(Vertex v)
    {
        return map.get(v); // warning, can return null!
    }
    
    /**
     * What edges arrive at a certain vertex
     */
    public LinkedHashSet<DEdge> incomingEdges(Vertex v)
    {
        return reverseMap.get(v);
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
     * Change edge bounds/properties on a later moment. 
     * Only useful during generation, after which changing the network can cause
     * problems (only use it before generating observations).
     * @param fro Staring Vertex
     * @param to Destination/ending Vertex
     * @param lb lower bound on the time needed (cost)
     * @param ub upper bound on the time needed (cost) 
     * @return 
     */
    public boolean changeEdgeBounds(Vertex fro, Vertex to, int lb, int ub)
    {
        Set<DEdge> edges = map.get(fro);
        if(edges==null) {
            return false;
        }
        for(DEdge edg : edges)
        {
            if(edg.getEnd().equals(to)) // ? need similar ??
            {
                edg.setLowerb(lb);
                edg.setUpperb(ub);
                return true;
            }
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
    
    /**
     * Give the array with all the vertices
     * @return Vertex[] all the vertices
     */
    public Vertex[] listAllVertices()
    {
        Vertex[] vertices = nodes.toArray(new Vertex[nodes.size()]);
        return vertices;
    }
    
    /**
     * Number of vertices
     * @return integer size of vertex array
     */
    public int vSize()
    {
        return nodes.size();
    }
    
    public boolean getNegativeEdgeCheck()
    {
        return checkNegativeEdges;
    }
}
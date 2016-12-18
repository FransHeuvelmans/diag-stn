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
import java.util.Random;
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
    
    /**
     * Removes an edge from the graph.
     * @param de Exact object reference to the edge that needs to be removed
     * @return Remove successful
     */
    public boolean removeEdge(DEdge de)
    {
        if(edges.contains(de))
        {
            edges.remove(de);
            LinkedHashSet mapSet = map.get(de.getStart());
            LinkedHashSet revmapSet = reverseMap.get(de.getEnd());
            boolean x = mapSet.remove(de);
            boolean y = revmapSet.remove(de);
            if(x == false || y == false)
                System.err.println("Edge wasnt fully dialed in on removal: " +
                        "" + de.getStart().getName() + " - " + de.getEnd().getName());
            
            // Some cleanup after removing edges
            if(mapSet.size() < 1)
            {   // No mapset needed anymore
               map.remove(de.getStart());
            }
            if(revmapSet.size() < 1)
            {   // same
                reverseMap.remove(de.getEnd());
            }
            
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
    
    /**
     * Set this to make the graph automatically reverse edges with a lower- and
     * upperbound which are negative.
     * @param checkForNeg boolean flag, true for reverse
     */
    public void reverseNegativeEdge(boolean checkForNeg)
    {
        checkNegativeEdges = checkForNeg;
    }
    
    /**
     * Find all vertices that can be reached in 1 step from given Vertex
     * @param last The vertex from which those others can be reached
     * @return LinkedList with all Vertices near
     */
    public LinkedList<Vertex> adjacentNodes(Vertex last)
    {
        LinkedHashSet<DEdge> edges = map.get(last);
        LinkedList<Vertex> adjacent = new LinkedList<>();
        if(edges == null)
            return adjacent;
        for(DEdge edg : edges)
        {
            adjacent.add(edg.getEnd());
        }
        return adjacent;
    }
    
    /**
     * Out degree. How many edges come from this Vertex to some other Vertex
     * @param fro The about vertex
     * @return integer with number of edges
     */
    public int outDegree(Vertex fro)
    {
        LinkedHashSet<DEdge> edges = map.get(fro);
        if(edges == null)
            return 0;
        return edges.size();
    }
    
    /**
     * What vertices go into this vertex.
     * @param to The vertex other go into
     * @return Linked list with all the vertex objects from which edges go into
     * target vertex
     */
    public LinkedList<Vertex> incomingNodes(Vertex to)
    {
        LinkedHashSet<DEdge> edges = reverseMap.get(to);
        LinkedList<Vertex> from = new LinkedList<>();
        if(edges != null)
        {
            for(DEdge edg : edges)
            {
                from.add(edg.getStart());
            }
        }
        return from;
    }
    
    /**
     * In degree. How many edges come to this Vertex from some other Vertex
     * @param to About vertex
     * @return integer with number of edges
     */
    public int inDegree(Vertex to)
    {
        LinkedHashSet<DEdge> edges = reverseMap.get(to);
        if(edges == null)
            return 0;
        return edges.size();
    }
    
    /**
     * What possible edges can be used from given Vertex.
     * @param v About vertex
     * @return Set containing all out edges
     */
    public LinkedHashSet<DEdge> possibleEdges(Vertex v)
    {
        return map.get(v); // warning, can return null!
    }
    
    /**
     * What edges arrive at a certain vertex.
     * @param v About vertex
     * @return Set with all incoming edges
     */
    public LinkedHashSet<DEdge> incomingEdges(Vertex v)
    {
        return reverseMap.get(v);
    }
    
    /**
     * Return a (pseudo) randomly picked edge from the set of all edges
     * @return DEdge object ref
     */
    public DEdge randomEdge()
    {
        Random randie = new Random();
        int randEdge = randie.nextInt(edges.size());
        return edges.toArray(new DEdge[edges.size()])[randEdge];
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
     * Returns the edge between two vertices if there is such an edge and the
     * vertex objects are the same as used by the Graph object.
     * @param fro Vertex object From 
     * @param to Vertex object To
     * @return DEdge object ref - can return null
     */
    public DEdge getDirectEdge(Vertex fro, Vertex to)
    {
         Set<DEdge> edges = map.get(fro);
        if(edges==null) {
            return null;
        }
        for(DEdge edg : edges)
        {
            if(edg.getEnd().equals(to)) // ? need similar ??
                return edg;
        }
        return null;
    }
    
    /**
     * Change edge bounds/properties on a later moment. 
     * Only useful during generation, after which changing the network can cause
     * problems (only use it before generating observations).
     * @param fro Staring Vertex
     * @param to Destination/ending Vertex
     * @param lb lower bound on the time needed (cost)
     * @param ub upper bound on the time needed (cost) 
     * @return True if successful change, false if otherwise
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
     * Give the array with all the edges
     * @return Array with all the edge objects
     */
    public DEdge[] listAllEdges()
    {
        DEdge[] edgeList = edges.toArray(new DEdge[edges.size()]);
        return edgeList;
    }
    
    /**
     * Number of vertices
     * @return integer size of vertex array
     */
    public int vSize()
    {
        return nodes.size();
    }
    
    /**
     * Returns the current state of the NegativeCheck flag. If this flag is set,
     * than all edges with a negative upper and lower bound are reversed.
     * @return true is flag = true
     */
    public boolean getNegativeEdgeCheck()
    {
        return checkNegativeEdges;
    }
    
    /**
     * A deep copy method. Uses the old ID's and names.
     * @return Graph copy
     */
    public Graph copy()
    {
        Graph clone = new Graph();
        for(Vertex v : nodes)
        {
            clone.addVertex(new Vertex(v.getID(), v.getName()));
        }
        for(DEdge de : edges)
        {
            Vertex start = clone.getVertex(de.getStart().getID());
            Vertex end = clone.getVertex(de.getEnd().getID());
            clone.addEdge(start, end, de.getLowerb(), de.getUpperb(), de.isContingent());
        }
        clone.reverseNegativeEdge(checkNegativeEdges);
        return clone;
    }
}
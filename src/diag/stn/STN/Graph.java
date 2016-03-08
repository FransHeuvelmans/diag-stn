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
 *
 * @author frans
 */
public class Graph
{
    private LinkedHashSet<Vertex> nodes;
    private LinkedHashSet<DEdge> edges;
    private Map<Vertex, LinkedHashSet<DEdge>> map = new HashMap();
    private int vertexIds = 0;
    
    public Graph()
    {
        nodes = new LinkedHashSet<>();
        edges = new LinkedHashSet<>();
        
        // a hashmap which gives all edges which start at
        // a particular node
        map = new HashMap();
    }
    
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
    
    public void addVertex(String name)
    {
        Vertex v = new Vertex(++vertexIds, name);
        nodes.add(v);
    }
    
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
}
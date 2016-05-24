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

import java.util.ArrayList;

/**
 * A Directed Edge. Currently only type of edge used in the graphs
 * @author Frans van den Heuvel
 */
public class DEdge
{
    private Vertex start, end;
    private int plowerbound, pupperbound; // predicted lower + upper
    private ArrayList<int[]> posChanges; // only use int[2] for lb/ub change
    private boolean hazard;
    
    /**
     * Constructor of a separate directed edge
     * @param s start vertex
     * @param e to end vertex
     */
    public DEdge(Vertex s, Vertex e)
    {
        start = s;
        end = e;
        posChanges = new ArrayList<>();
        hazard = false;
    }
    
    /**
     * Quick constructor to instantiate the directed edge at once
     * @param s start vertex
     * @param e end vertex
     * @param lb lower bound on cost/time
     * @param ub upper bound on cost/time
     */
    public DEdge(Vertex s, Vertex e, int lb, int ub)
    {
        start = s;
        end = e;
        plowerbound = lb;
        pupperbound = ub;
        posChanges = new ArrayList<>();
        hazard = false;
    }
    
    /**
     * Adds a possible change to the edge. This means that a path of which this
     * edge is part of can be fixed by changing the bounds on this edge by an amount
     * bounded by given bound
     * @param lowerboundChange a given lower bound on the change
     * @param upperboundChange a given upper bound on the change
     */
    public void addPossibleChange(int lowerboundChange, int upperboundChange)
    {
        int[] posCha = new int[2];
        posCha[0] = lowerboundChange; // for now assume that both can be pos/neg
        posCha[1] = upperboundChange; // and no checks !
        posChanges.add(posCha);
    }
    
    /**
     * Set this to include output to the user to warn when applying a diagnosis
     * for a possible inconsistency.
     * @param problem 
     */
    public void setPossibleConProblem(boolean problem)
    {
        this.hazard = problem;
    }
    
    /**
     * A list of all possible changes. Needed to see if the changes can be 
     * combined and to give an overview of all changes that the diagnosis algorithm
     * has stored in this particular edge
     * @return An arraylist with all the changes (stored in int[2] format lb,ub)
     */
    public ArrayList<int[]> getPossibleChanges()
    {
        ArrayList<int[]> changesPos = new ArrayList<>(posChanges.size());
        for(int[] chng: posChanges)
        {
            int[] x = new int[2];
            x[0] = chng[0];
            x[1] = chng[1];
            changesPos.add(x);
        }
        return changesPos; // Dirty for now but separate STN from diagnosis!
    }
    
    /**
     * Set the lower bound for this edge.
     * @param lb lower bound
     */
    public void setLowerb(int lb)
    {
        plowerbound = lb;
    }
    
    /**
     * Set the upper bound for this edge
     * @param ub upper bound
     */
    public void setUpperb(int ub)
    {
        pupperbound = ub;
    }
    
    /**
     * Get the start vertex
     * @return starting vertex object
     */
    public Vertex getStart()
    {
        return start;
    }
    
    /**
     * Get the end vertex
     * @return ending vertex object
     */
    public Vertex getEnd()
    {
        return end;
    }
    
    /**
     * Get lower bound
     * @return integer with lower bound
     */
    public int getLowerb()
    {
        return plowerbound;
    }
    
    /**
     * Get upper bound
     * @return integer with upper bound
     */
    public int getUpperb()
    {
        return pupperbound;
    }
    
    /**
     * Is there a possible consistency error when changing this edge in 
     * combination with other changes in the network
     * @return boolean which is true if there is a possibility
     */
    public boolean possibleConProblem()
    {
        return hazard;
    }
    
    
}

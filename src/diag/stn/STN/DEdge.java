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
package diag.stn.STN;

import java.util.ArrayList;

/**
 * A Directed Edge
 * @author frans
 */
public class DEdge
{
    private Vertex start, end;
    private int plowerbound, pupperbound; // predicted lower + upper
    private ArrayList<int[]> posChanges; // only use int[2] for lb/ub change
    
    public DEdge(Vertex s, Vertex e)
    {
        start = s;
        end = e;
        posChanges = new ArrayList<>();
    }
    
    public DEdge(Vertex s, Vertex e, int lb, int ub)
    {
        start = s;
        end = e;
        plowerbound = lb;
        pupperbound = ub;
        posChanges = new ArrayList<>();
    }
    
    public void addPossibleChange(int lowerboundChange, int upperboundChange)
    {
        int[] posCha = new int[2];
        posCha[0] = lowerboundChange; // for now assume that both can be pos/neg
        posCha[1] = upperboundChange; // and no checks !
        posChanges.add(posCha);
    }
    
    public ArrayList<int[]> getPossibleChanges()
    {
        return posChanges; // Dirty for now but separate STN from diagnosis!
    }
    
    public void setLowerb(int lb)
    {
        plowerbound = lb;
    }
    
    public void setUpperb(int ub)
    {
        pupperbound = ub;
    }
    
    public Vertex getStart()
    {
        return start;
    }
    
    public Vertex getEnd()
    {
        return end;
    }
    
    public int getLowerb()
    {
        return plowerbound;
    }
    
    public int getUpperb()
    {
        return pupperbound;
    }
    
    
}

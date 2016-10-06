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

import diag.stn.STN.DEdge;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * A Consistency based Diagnosis (for use without fault model).
 * @author Frans van den Heuvel
 */
public class ConDiagnosis
{
    /**
     * Is made for use in Analyst when generating Consistency based diangoses
     * without fault models. ConAnalyst uses "normal" Diagnosis objects because
     * the fault model is the same as MAC diagnosis & needed.
     */
    private ArrayList<DEdge> faultyEdges;
    private ArrayList<DEdge> correctEdges;
    
    public ConDiagnosis()
    {
        faultyEdges = new ArrayList();
        correctEdges = new ArrayList();
    }
    
    /**
     * Add some misbehaving edge
     * @param wrong DEdge object with the faulty edge
     */
    public void addFaultyEdge(DEdge wrong)
    {
        if(correctEdges.contains(wrong))
            System.err.println("How can some edge be faulty and correct?");
        if(!faultyEdges.contains(wrong))
            faultyEdges.add(wrong);
    }
    
    /**
     * Add some edge as a correct edge
     * @param right DEdge object with the correct edge
     */
    public void addCorrectEdge(DEdge right)
    {
        if(!correctEdges.contains(right))
            correctEdges.add(right);
    }
    
    /**
     * Is some edge part of the set of correct edges?
     * @param de DEdge object (checks for object reference so needs to be based
     * on the same graph)
     * @return boolean true if it was found false if it was not
     */
    public boolean edgeCorrect(DEdge de)
    {
        return correctEdges.contains(de);
    }
    
    /**
     * Is some edge already solved with a certain diagnosis ?
     * @param de DEdge object (checks for object reference so needs to be based
     * on the same graph)
     * @return bool if edge was part of set of misbehaving edges
     */
    public boolean edgeSolved(DEdge de)
    {
        return faultyEdges.contains(de);
    }
    
    /**
     * See if some path has been solved. This presumes that the path was wrongly
     * predicted and needed solving. It checks if a edge on the path is noted as
     * misbehaving edge.
     * @param p GraphPath object (needs to use the same DEdge objects so needs
     * to use the same original graph)
     * @return bool true if an edge was found false if not
     */
    public boolean pathSolved(GraphPath p)
    {
        for(int i = 1; i < p.stepSize(); i++)
        {
            DEdge edgeIn = p.getStepE(i);
            if(faultyEdges.contains(edgeIn))
                return true;
        }
        return false;
    }
    
    /**
     * Is this diagnosis a subset of another diagnosis ? Ie. does the other
     * diagnosis use every indicated faulty edge of this diagnosis as well.
     * @param other This might be a superset/equal or just another ConDiagnosis
     * @return bool
     */
    public boolean isSubsetOrEqual(ConDiagnosis other)
    {
        for(DEdge fault : faultyEdges)
        {
            if(!other.edgeSolved(fault))
                return false;
        }
        // compare the 2, for removal of all non-minimal condiag
        return true;
    }
    
    /**
     * Get the size of the solution ie. the number of faulty edges (that was 
     * needed to get to this diagnosis).
     * @return int of the faulty edges array size
     */
    public int solSize()
    {
        return faultyEdges.size();
    }
    
    /**
     * Get all faulty edges in a list
     * @return DEdge object array
     */
    public DEdge[] getFaultyEdges()
    {
        ArrayList<DEdge> clone = new ArrayList(faultyEdges);
        return clone.toArray(new DEdge[clone.size()]);
    }
    
    /**
     * Get all correct edges in a list
     * @return DEdge object array
     */
    public DEdge[] getCorrectEdges()
    {
        ArrayList<DEdge> clone = new ArrayList(correctEdges);
        return clone.toArray(new DEdge[clone.size()]);
    }
    
    /**
     * Create a copy of this diagnosis. Shallow copy with the same DEdge object
     * references inside.
     * @return ConDiagnosis object
     */
    public ConDiagnosis copy()
    {
        ConDiagnosis theCopy = new ConDiagnosis();
        for(DEdge coredge: correctEdges)
            theCopy.addCorrectEdge(coredge);
        for(DEdge wrngedge : faultyEdges)
            theCopy.addFaultyEdge(wrngedge);
        return theCopy;
    }
    
    /**
     * Print this diagnosis
     */
    public void printDiagnosis()
    {
        System.out.print("Abnormal = {");
        for(DEdge fedge : faultyEdges)
        {
            System.out.print("d" + fedge.getStart().getName() + "," 
                    + fedge.getEnd().getName() + " \u02C4 ");
        }
        System.out.print("} , d-rest = normal\n");
    }
    
    /**
     * Print all diagnosis in disjunctive normal form.
     * @param all All Different ConDiagnosis objects generated by some analyst.
     */
    public static void printAllDiag(ConDiagnosis[] all)
    {
        int iter = 1;
        System.out.println("=== Consistency based diagnosis overview ===");
        for(ConDiagnosis d : all)
        {
            System.out.println("Diagnosis: " + iter);
            d.printDiagnosis();
            iter++;
        }
    }
}

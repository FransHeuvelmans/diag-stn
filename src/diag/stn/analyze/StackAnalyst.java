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

import diag.stn.DiagSTN;
import diag.stn.STN.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * A non-recursive version of Analyst (DiagSTN). Could perform better than 
 * recursive version in Java. (Warning just testing version)
 * @author Frans van den Heuvel
 */
public class StackAnalyst extends Analyst
{
    
    public StackAnalyst(Graph g)
    {
        super(g);
    }
    
    /**
     * Get a list of all possible full diagnosis for this model. Needs propagateWeights
     * to be done before diagnosis can start.
     * @return A list of possible Diagnosis for the model
     */
    public Diagnosis[] generateDiagnosis()
    {
        LinkedList<GraphPath> needDiag = new LinkedList<>();
        // First repair possible inconsistent observations
        if(!DiagSTN.IGNOREINCONSIST && inconsistent != null) 
        {
            if(DiagSTN.PRINTWARNING)
                System.out.println("Found inconsistent paths, will proceed with"
                        + " repair suggestions");
            Iterator<Observation> incIter = inconsistent.iterator();
            while(incIter.hasNext())
            {
                Observation incObs = incIter.next();
                LinkedHashSet<GraphPath> pathSet = obsPaths.get(incObs);
                for(GraphPath p : pathSet)
                    needDiag.add(p);
            }
        }
        else
        {
            for(Observation ob: observations)
            {
                if(ob.fixneeded)
                {
                    LinkedHashSet<GraphPath> pathSet = obsPaths.get(ob);
                    for(GraphPath p : pathSet)
                        needDiag.add(p);    // faster way to copy?
                }
            }
        }
        
        generateDiagnosis(needDiag);
        return diagnosisList.toArray(new Diagnosis[diagnosisList.size()]);
    }
    
    /* Underlying generateDiagnosis method used to recursively traverse the tree
     * of problem edges */ 
    private void generateDiagnosis(LinkedList<GraphPath> wronglyPredicted)
    {
        LinkedList<GraphPath> solvedPaths = new LinkedList(); 
        LinkedList<Diagnosis> tempDiagnoses = new LinkedList(); // All partial diagnoses
        
        // Initialize tempDiag.
        GraphPath current = wronglyPredicted.pop();
        
        for(int i = 1; i < current.stepSize(); i++)
        {
            DEdge edge = current.getStepE(i);
            Diagnosis diag = new Diagnosis();
            
            // if combine possible
            boolean combine = true;
            ArrayList<int[]> changes = edge.getPossibleChanges();
            int[] finalchng = changes.remove(changes.size()-1); // lets start with the last added bounds
            for(int[] bounds : changes)
            {
                if((bounds[0] > finalchng[1])||(bounds[1] < finalchng[0]))
                {
                    combine = false;
                    break;
                }
                else
                {
                    if(bounds[0] > finalchng[0])
                        finalchng[0] = bounds[0];
                    if(bounds[1] < finalchng[1])
                        finalchng[1] = bounds[1];
                }
            }
            if(combine)
            {
                diag.addPartial(edge, finalchng[0], finalchng[1]);
                tempDiagnoses.add(diag);
            }
        }
        solvedPaths.add(current);
        
        // Every time a GraphPath has been tested for all diagnoses, it is removed
        while(!wronglyPredicted.isEmpty())
        {
            // A list which will replace the tempDiagnoses when done
            // (Continue to next round)
            LinkedList<Diagnosis> contiDiag = new LinkedList();
            
            // Pop a path and continue with changed Diagnoses.
            current = wronglyPredicted.pop();
            
            for(Iterator<Diagnosis> it = tempDiagnoses.iterator(); it.hasNext();)
            {
                Diagnosis d = it.next();
                
                // Any diagnosis that already solves this path can be passed on
                if(d.edgeUsed(current))
                {
                    contiDiag.add(d);
                }
                else // All the other diagnoses must be processed
                {
                    // For every edge, there might a solution diagnosis
                    for(int j = 1; j < current.stepSize(); j++)
                    {
                        DEdge edge = current.getStepE(j);
                               
                        // Edge shouldnt be part of a path which is already solved
                        boolean partOfSolved = false;
                        for(GraphPath p : solvedPaths)
                        {
                            if(p.edgeUsed(edge))
                            {
                                partOfSolved = true;
                                break;
                            }
                        }
                        if(partOfSolved)
                            continue; // Skip this edge, 
                        
                        // if combine possible
                        boolean combine = true;
                        ArrayList<int[]> changes = edge.getPossibleChanges();
                        // lets start with the last added bounds
                        int[] finalchng = changes.remove(changes.size()-1);
                        for(int[] bounds : changes)
                        {
                            if((bounds[0] > finalchng[1])||(bounds[1] < finalchng[0]))
                            {
                                combine = false;
                                break;
                            }
                            else
                            {
                                if(bounds[0] > finalchng[0])
                                    finalchng[0] = bounds[0];
                                if(bounds[1] < finalchng[1])
                                    finalchng[1] = bounds[1];
                            }
                        }
                        if(combine)
                        {
                            Diagnosis dConti = d.copy();
                            dConti.addPartial(edge, finalchng[0], finalchng[1]);
                            contiDiag.add(dConti);
                        }
                        // others are simply forgotten in some tempDiagnoses
                    }
                }
            }
            // contiDiag contains the diagnoses that passed the test of this 
            // path (i.e. conflict set)
            solvedPaths.add(current);
            tempDiagnoses = contiDiag;
        }
        
        // Done, copy them to final Array
        for(Diagnosis finalDiag : tempDiagnoses)
            diagnosisList.add(finalDiag);
    }
}

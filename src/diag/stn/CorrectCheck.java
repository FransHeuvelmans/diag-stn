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
package diag.stn;

import diag.stn.STN.*;
import diag.stn.GraphGenerator.GraphObs;
import diag.stn.analyze.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Class with static methods for showing and checking how good the diagnoses 
 * are. Plus methods for comparing answers, and input problems.
 * @author Frans van den Heuvel
 */
public  class CorrectCheck
{
    /**
     * Prints the errors that were actually introduced in the Graph
     * @param grOb 
     */
    public static void printErrorsIntroduced(GraphObs grOb)
    {
        System.out.println("\n*** Errors introduced :");
        for(int i = 0; i < grOb.errorEdges.size(); i++)
        {
            DEdge de = grOb.errorEdges.get(i);
            System.out.print("d" + de.getStart().getName() + "," + 
                    de.getEnd().getName());
            int diff = grOb.errorDiffs.get(i);
            System.out.print(" by: " +  diff + " ");
        }
    }
    
    /**
     * See if there is a MAC diagnosis among all diagnoses that has the actual 
     * introduced errors.
     * @param grOb
     * @param diagnoses
     * @return 
     */
    public static boolean errorInDiagnoses(GraphObs grOb, Diagnosis[] diagnoses)
    {
        boolean found = false;
        for(Diagnosis d : diagnoses)
        {
            boolean hasAllErrors = true;
            for(int i = 0; i < grOb.errorEdges.size(); i++)
            {
                int trueError = grOb.errorDiffs.get(i);
                if(trueError == 0)
                    continue; // no error introduced TODO improve checking
                
                // Needs to have used each malfunctioning edge in the GraphObs
                // ie. each introduced error
                DEdge errorEdg = grOb.errorEdges.get(i);
                if(!d.edgeUsed(errorEdg))
                {
                    hasAllErrors = false;
                    break;
                }
                else    // its in there but does it have the correct bounds?
                {
                    int[] diagBounds = d.getChanges(errorEdg);
                    if(trueError < diagBounds[0] || trueError > diagBounds[1])
                    {
                        hasAllErrors = false;
                        // Still a problem , not the correct bounds
                        break;
                    }
                }
            }
            if(hasAllErrors)
            {
                found = true;
                break;
            }
        }
        return found;
    }
    
    /**
     * See if there is a Consistency based diagnosis that has the 
     * actual introduced errors.
     * @param grOb
     * @param diagnoses
     * @return 
     */
    public static boolean errorInConDiagnoses(GraphObs grOb, ConDiagnosis[] diagnoses)
    {
        boolean found = false;
        for(ConDiagnosis d : diagnoses)
        {
            boolean hasAllErrors = true;
            for(DEdge errEdge : grOb.errorEdges)
            {
                if(!d.edgeSolved(errEdge))
                {
                    hasAllErrors = false;
                    break;  // Must have each added error position (= edge)!
                }
                // Does not need to know what the error is
            }
            if(hasAllErrors)
            {
                found = true;
                break;  // One diagnosis is correct so it is in the set 
            }
        }
        return found;
    }
    
    /**
     * Compare the diagnosis size of two answer sets. Used to compare
     * the possible answers when using Consistency based diagnosis (+ Fault Model)
     * vs MAC diagnosis
     * @param a
     * @param b
     * @return 
     */
    public static int compareDiagnosisSize(Diagnosis[] a, Diagnosis[] b)
    {
        int totalSizeA = 0;
        int countA = a.length;
        for(Diagnosis da : a)
        {
            int diagSize = 0;
            DEdge[] changed = da.getEdgesChanged();
            for(DEdge cEdge : changed)
            {
                int[] chnges = da.getChanges(cEdge);
                int chngSize = chnges[1] - chnges[0];
                totalSizeA += chngSize;
            }
        }
        //double ansA = (double) totalSizeA; // no average so no / (double) countA
        
        int totalSizeB = 0;
        int countB = b.length;
        for(Diagnosis db : b)
        {
            int diagSize = 0;
            DEdge[] changed = db.getEdgesChanged();
            for(DEdge cEdge : changed)
            {
                int[] chnges = db.getChanges(cEdge);
                int chngSize = chnges[1] - chnges[0];
                totalSizeB += chngSize;
            }
        }
        //double ansB = (double) totalSizeB;
        // no average so no / (double) countB
        
        return (totalSizeA - totalSizeB);
    }
    
    /**
     * Calculates the total prediction interval size of all the observations
     * @param pd initialized GraphObs object with the problem description
     * @return integer with interval size
     */
    public static int totalPredictionSize(GraphObs pd)
    {
        int total = 0;
        for(Observation ob : pd.observations)
        {
            Vertex st = ob.startV;
            Vertex fin = ob.endV;
            GraphPath startPath = new GraphPath(st);
            ArrayList<int[]> boundsFound = GraphGenerator.pathCalc(startPath, 0,
                    0, fin, pd.graph);
            /*
            Warning uses the union instead of the intersection like GraphGen.
            Because a priori any value within the union is possible for some
            prediction
            */
            int[] boufou = unionPaths(boundsFound);
            
            int inter = boufou[1] - boufou[0];
            total += inter;
        }
        return total;
    }
    
    /**
     * takes the union of all the path bound (intervals)
     * @param paths Arraylist of ints with the bounds
     * @return int[2] array with the union bounds
     */
    public static int[] unionPaths(ArrayList<int[]> paths)
    {
        if(paths.size() < 1)
            System.err.println(" Cant combine empty arraylist!");
        
        // Takes the Intersection
        int[] finalbounds = paths.remove(paths.size()-1); // take last
        for(int[] p: paths)
        {
            if(p[0] < finalbounds[0])
                finalbounds[0] = p[0];
            if(p[1] > finalbounds[1])
                finalbounds[1] = p[1];
        }
        return finalbounds;
    }
    
    /**
     * Number of unique edges which are observed 
     * @param pd initialized GraphObs object with the problem description
     * @return # of unique edges observed
     */
    public static int numberUniqueEdges(GraphObs pd)
    {
        LinkedHashSet<DEdge> edges = new LinkedHashSet();
        for(Observation ob : pd.observations)
        {
            Vertex st = ob.startV;
            Vertex fin = ob.endV;
            GraphPath startPath = new GraphPath(st);
            ArrayList<GraphPath> pathsFound = GraphGenerator.obsPaths(startPath,
                    fin, pd.graph);
            
            // For each path add all the edges to a set (with no doubles)
            for(GraphPath gp : pathsFound)
            {
                for(int i=1; i < gp.stepSize(); i++)
                {
                    DEdge de = gp.getStepE(i);
                    if(!edges.contains(de))
                        edges.add(de);
                }
            }
        }
        // return the size of the total set of edges
        return edges.size();
    }
    
    /**
     * Total number of edges that is observed. Some edges are multiple times in
     * this total. This method shows the input size in # edges of the 
     * generateDiagnosis() method.
     * @return # edges observed with doubles
     */
    public static int totalNumberEdges(GraphObs pd)
    {
        int totalEdges = 0;
        for(Observation ob : pd.observations)
        {
            Vertex st = ob.startV;
            Vertex fin = ob.endV;
            GraphPath startPath = new GraphPath(st);
            ArrayList<GraphPath> pathsFound = GraphGenerator.obsPaths(startPath,
                    fin, pd.graph);
            
            // For each path add all the edges to a set (with no doubles)
            for(GraphPath gp : pathsFound)
            {
                int pathSize = gp.toEdges().length;
                totalEdges += pathSize;
            }
        }
        return totalEdges;
    }
}

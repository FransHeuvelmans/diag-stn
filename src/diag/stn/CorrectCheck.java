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
package diag.stn;

import diag.stn.STN.*;
import diag.stn.GraphGenerator.GraphObs;
import diag.stn.analyze.*;

/**
 * Class with static methods for showing and checking how good the diagnoses 
 * are. 
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
        System.out.println("*** Errors introduced :");
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
                    int trueError = grOb.errorDiffs.get(i);
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
        return false;
    }
    
}

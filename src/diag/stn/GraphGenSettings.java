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

/**
 * Settings used for generating sample Simple Temporal Diagnosis Problems.
 * @author Frans van den Heuvel
 */
public class GraphGenSettings {
    
    public int type;    // For general settings
    
    public int difference;    // Diff percentage
    public int numObservations;
    public int observationLength;
    public boolean timeSyncT0;
    
    // BA settings
    public int vertexSize;
    public int BALinksPerVertexAddition;
    public boolean onlyMaxAdditions;
    
    // Plan settings
    public int numLines;
    public int lineLengthLB;
    public int lineLengthUB;
    public int maxInterLineConnect;
    public int maxLineVertConnect;
    
    public static final int BAGRAPH = 42;
    public static final int PLANLIKEGRAPH = 24;
    
    public GraphGenSettings()
    {
        // nothing, needs setting
    }
    
    /**
     * Creates a Setting object for a certain type of network
     * @param whatType 
     */
    public GraphGenSettings(int whatType)
    {
        if(whatType == BAGRAPH)
            BAGraph();
        else if(whatType == PLANLIKEGRAPH)
            planlikeGraph();
        else
            System.err.println("Wrong type of default Graph Settings generated");
    }
    
    /**
     * Transform Setting object to default Barabàsi-Albert graph settings.
     */
    public void BAGraph()
    {
        // use some defaults settings
        type = BAGRAPH;
        
        this.vertexSize = 50;
        this.BALinksPerVertexAddition = 2;
        this.onlyMaxAdditions = false;
        
        this.numObservations = 2;
        this.observationLength = 5;
        this.difference = 10;
        this.timeSyncT0 = false;
    }
    
    /**
     * Set specific Barabàsi-Albert graph settings.
     * @param size Number of vertices
     * @param linksPerStep when adding a vertex, how many edges need to be added
     * @param onlymax Always try to add max edges or a random amount (1-max)
     * @param observations # false observations
     * @param obsLength Length of the observations added (1 path with edge size)
     * @param diff Percentage in int (ie. 50% = 50) that will be added (or
     * subtracted when negative) of the observation (path) prediction
     * @param zeroPoint Add a time synchronization point 
     */
    public void BAGraph(int size, int linksPerStep, boolean onlymax, 
            int observations, int obsLength, int diff, boolean zeroPoint)
    {
        type = BAGRAPH;
        
        this.vertexSize = size;
        this.BALinksPerVertexAddition = linksPerStep;
        this.onlyMaxAdditions = onlymax;
        
        this.numObservations = observations;
        this.observationLength = obsLength;
        this.difference = diff;
        this.timeSyncT0 = zeroPoint;
    }
    
    /**
     * Transform Setting object to default Plan-like graph settings.
     */
    public void planlikeGraph()
    {
        // set some sane default settings
        
        type = PLANLIKEGRAPH;
        
        this.numLines = 3;
        this.lineLengthLB = 8;
        this.lineLengthUB = 12;
        this.maxInterLineConnect = 2;
        this.maxLineVertConnect = 2;
        
        this.numObservations = 2;
        this.observationLength = 5;
        this.difference = 10;
        this.timeSyncT0 = false;
    }
    
    /**
     * Set specific Plan-like graph settings
     * @param line Number of plans combined for this network
     * @param linelb Lower bound on plan length in edges
     * @param lineub Upper bound on the plan length in edges 
     * @param maxLineCon With how many plans is one plan connected
     * @param maxVertCon How many edges are used to connect 2 plans
     * @param observations # false observations 
     * @param obsLength Length of the observations added (1 path with edge size)
     * @param diff Percentage in int (ie. 50% = 50) that will be added (or
     * subtracted when negative) of the observation (path) prediction
     * @param zeroPoint Add a time synchronization point  
     */
    public void planlikeGraph(int line, int linelb, int lineub, 
            int maxLineCon, int maxVertCon, int observations, int obsLength, 
            int diff, boolean zeroPoint)
    {
        type = PLANLIKEGRAPH;
        
        this.numLines = line;
        this.lineLengthLB = linelb;
        this.lineLengthUB = lineub;
        this.maxInterLineConnect = maxLineCon;
        this.maxLineVertConnect = maxVertCon;
        
        this.numObservations = observations;
        this.observationLength = obsLength;
        this.difference = diff;
        this.timeSyncT0 = zeroPoint;
    }
    
    /**
     * Creates a string that describes the full Settings object.
     * @return String object with a full description.
     */
    @Override
    public String toString()
    {
        String out;
        if(type == BAGRAPH)
        {
            out = "BAGRaph =";
            out = out + " # vertex: " + vertexSize;
            out = out + " links/vertex: " + BALinksPerVertexAddition;
            out = out + " only max: " + onlyMaxAdditions;
            out = out + " # obs: " + numObservations;
            out = out + " obs len: " + observationLength;
            out = out + " diff: " + difference;
            out = out + " T0: " + timeSyncT0;
        }
        else if(type == PLANLIKEGRAPH)
        {
            out = "PlanlikeGraph =";
            
            out = out + " # lines: " + numLines;
            out = out + " line length lb: " + lineLengthLB;
            out = out + " line length ub: " + lineLengthUB;
            out = out + " line inter con: " + maxInterLineConnect;
            out = out + " line vert con: " + maxLineVertConnect;
            out = out + " # obs: " + numObservations;
            out = out + " obs len: " + observationLength;
            out = out + " diff: " + difference;
            out = out + " T0: " + timeSyncT0;
        }
        else
            out = "UnknownGraphSettings";
        return out;
    }
}

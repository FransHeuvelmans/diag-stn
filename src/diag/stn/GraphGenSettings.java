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
        // nuthin, needs setting
    }
    
    public GraphGenSettings(int whatType)
    {
        if(whatType == BAGRAPH)
            BAGraph();
        else if(whatType == PLANLIKEGRAPH)
            planlikeGraph();
        else
            System.err.println("Wrong type of default Graph Settings generated");
    }
    
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
    
    public void planlikeGraph()
    {
        // set some sane default settings
        
        type = PLANLIKEGRAPH;
    }
    
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
}

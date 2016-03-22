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


/**
 *
 * @author frans
 */
public class Observation
{
    public Vertex startV;
    public Vertex endV;
    public int endLb, endUb; // time
    public boolean fixneeded;
    
    
    public Observation(Vertex sV, Vertex eV, int lb, int ub)
    {
        startV = sV;
        endV = eV;
        endLb = lb;
        endUb = ub;
    }
}

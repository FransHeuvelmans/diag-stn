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

/**
 * A single point in a Graph as object.
 * @author Frans van den Heuvel
 */
public class Vertex
{
    private String name;
    private int id;
    
    /**
     * Create Vertex with a certain id. Uses id as temporary name.
     * @param ident integer with identity
     */
    public Vertex(int ident)
    {
        id = ident;
        name = "" + id;
    }
    
    /**
     * Create a Vertex with certain id and name.
     * @param ident integer with identity
     * @param n String with the name
     */
    public Vertex(int ident, String n)
    {
        id = ident;
        name = n;
    }
    
    /**
     * Changes the name of the Vertex
     * @param n String with the name
     */
    public void setName(String n)
    {
        name = n;
    }
    
    /**
     * The name of the current vertex (if there is one)
     * @return String with name
     */
    public String getName()
    {
        if(name != null)
            return name;
        else
            return ("" + id);
    }
    
    /**
     * Return the id of the Vertex. Should be unique and can not be changed.
     * @return integer with identity
     */
    public int getID()
    {
        return id;
    }
}

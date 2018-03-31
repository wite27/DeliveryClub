package models;

import java.util.ArrayList;

/**
 * Created by K750JB on 24.03.2018.
 */
public class VertexSettings {
    public String Name;

    public ArrayList<String> AdjacentVertices;

    public VertexSettings(String name, ArrayList<String> adjacentVertices)
    {
        this.Name = name;
        this.AdjacentVertices = adjacentVertices;
    }
}

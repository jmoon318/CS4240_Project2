import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Random;
import java.util.Stack;

import ir.IRInstruction;

public class InterferenceGraph {
    public class InstrVertex {
        IRInstruction instruction;
        ArrayList<IRInstruction> edges;
        int color;
    }
    ArrayList<InstrVertex> vertices;
    public InterferenceGraph(BasicBlock block) {
        this.vertices = new ArrayList<InstrVertex>();

        for (IRInstruction instr : block.instructions) {
            // setup
            InstrVertex vertex = new InstrVertex();
            vertex.instruction = instr;
            vertex.edges = new ArrayList<IRInstruction>();
            // colors will start at 0, for now no color.
            vertex.color = -1;

            Integer[] liveRange = getLiveRange(instr, block);
            // we now have the biggest line # of where this instr def is used
            // this might be useful for building Live Out set as well
            for (IRInstruction otherInstr : block.instructions) {
                // for each of the other instructions see if we overlap, if so, add an edge
                if (instr.irLineNumber != otherInstr.irLineNumber && overlap(liveRange, getLiveRange(otherInstr, block))) {
                    vertex.edges.add(otherInstr);
                    // the edges for the otherInstr vertex will be added on it's iteration
                }
            }
            // add the vertex to the graph
            this.vertices.add(vertex);
        }
    }

    // range[0] is the start of the range and range[1] is the end
    private boolean overlap(Integer[] range1, Integer[] range2) {
        // if range 2 starts after range 1 ends or vice versa descriobes all situations without overlap (i think at 11pm)
        return !(range1[1] <= range2[0] || range1[0] >= range2[1]); 
    }

    public Integer[] getLiveRange(IRInstruction instr, BasicBlock block) {
        int instrLine = instr.irLineNumber;
        int maxUseLine = instrLine;
        // for each of the other instructions in the block
        for (IRInstruction otherInstr : block.instructions) {
            // get used defs of each other line
            for(String use : otherInstr.getUseSet()) {
                Integer defLine = Integer.valueOf(use.substring(3, use.indexOf("(")));
                // if the passed instruction is one of the used defs and this is a greater line number than we've already found
                // update that max value
                if (defLine == instrLine && otherInstr.irLineNumber > maxUseLine) {
                    maxUseLine = otherInstr.irLineNumber;
                }
            }
        }
        Integer[] out = {instr.irLineNumber, maxUseLine};
        return out;
    }
    
    public ArrayList<InstrVertex> cBriggsColor() {
        Stack<InstrVertex> stack = new Stack<InstrVertex>();
        InstrVertex vert = null;

        while (this.vertices.size() > 0) {
            // if the graph has no vertices with degree < 10
            // step 2 in lec11 slides
            if (getDegVertex() == null) {
                vert = getRandVertex();
                stack.push(vert);
                this.rmVertex(vert);
            }
            // step 1 in lec11 slides
            while ((vert = this.getDegVertex()) != null) {
                stack.push(vert);
                this.rmVertex(vert);
            }
            // when the above while loop breaks we either have no vertices with degreee < 10
            // or the graph is now empty
        }
        while (true) {
            try {
                // pop an instruction off the top of the stack
                InstrVertex vertex = stack.pop();

                // attempt to add it back with the lowest color
                int i = 0;
                while (!addWithColor(vertex, i++));
            }
            // once the stack is empty, break the loop.
            catch (EmptyStackException e) {
                break;
            }
        }
        // this.verticies should now hold verticies with the appropriate color
        return this.vertices;
    }

    // returns false if a neighbor already has this color, returns true if a color was assigned
    // and the vertex was added back to the graph
    private boolean addWithColor(InstrVertex vertex, Integer color) {
        for (InstrVertex v : this.vertices) {
            // if we find a neighbor (shared edge) that has been added back with the same color
            // return false
            if (vertex.edges.contains(v.instruction) && v.color == color) {
                return false;
            }
        }
        vertex.color = color;
        this.vertices.add(vertex);
        // make sure we add the edges to this vertex back
        for (InstrVertex v : this.vertices) {
            if (vertex.edges.contains(v.instruction)) {
                v.edges.add(vertex.instruction);
            }
        }
        return true;
    }

    // we will only have 10 registers (colors) to use
    private InstrVertex getDegVertex(){
        for (InstrVertex v : this.vertices) {
            // return a vertex with degree less than 10
            if (v.edges.size() < 10) {
                return v;
            }
        }
        return null;
    }

    private InstrVertex getRandVertex(){
        // this should actually be by some heuristic? for now random tho. 
        int indx = new Random().nextInt(this.vertices.size() - 1);
        return this.vertices.get(indx);
    }

    // removes a vertex and edges to that vertex from this graph
    private InstrVertex rmVertex(InstrVertex v) {
        boolean held = this.vertices.remove(v);
        for (InstrVertex otherV : this.vertices) {
            otherV.edges.remove(v.instruction);
        }
        if (held) {
            return v;
        } else {
            return null;
        }
    }

    @Override
    public String toString(){
        this.vertices = this.cBriggsColor();
        String out = "INTERFERENCE GRAPH:\n";
        for (InstrVertex v : this.vertices) {
            out += "  VERTEX:\n";
            out += "  " + v.instruction.toString() + " COLOR: " + v.color + "\n";
            out += "    EDGES:\n";
            for (IRInstruction edge : v.edges) {
                out += "    - " + edge.toString() + "\n";
            }
            out += "\n";
        }
        return out;
    }
} 
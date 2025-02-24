import java.util.ArrayList;

import ir.IRInstruction;

public class InterferenceGraph {
    public class InstrVertex {
        IRInstruction instruction;
        ArrayList<IRInstruction> edges;
    }
    ArrayList<InstrVertex> vertices;
    public InterferenceGraph(BasicBlock block) {
        this.vertices = new ArrayList<InstrVertex>();
        for (IRInstruction instr : block.instructions) {
            // setup
            InstrVertex vertex = new InstrVertex();
            vertex.instruction = instr;
            vertex.edges = new ArrayList<IRInstruction>();

            Integer[] liveRange = instr.getLiveRange();
            // we now have the biggest line # of where this instr def is used
            // this might be useful for building Live Out set as well
            for (IRInstruction otherInstr : block.instructions) {
                // for each of the other instructions see if we overlap, if so, add an edge
                if (!instr.equals(otherInstr) && overlap(liveRange, otherInstr.getLiveRange())) {
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
        return !(range1[1] < range2[0] || range1[0] > range2[1]); 
    }

    @Override
    public String toString(){
        String out = "INTERFERENCE GRAPH:\n";
        for (InstrVertex v : this.vertices) {
            out = "  VERTEX:\n";
            out += "  " + v.instruction.toString() + "\n";
            out += "    EDGES:\n";
            for (IRInstruction edge : v.edges) {
                out += "    - " + edge.toString() + "\n";
            }
            out += "\n";
        }
        return out;
    }
    // we will only have 10 registers (colors) to use
}
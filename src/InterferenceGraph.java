import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

import ir.IRInstruction;
import ir.operand.IRVariableOperand;

public class InterferenceGraph {
    public class InstrVertex {
        IRInstruction instruction;
        ArrayList<IRInstruction> edges;
    }
    ArrayList<InstrVertex> vertices;
    HashMap<String, ArrayList<Integer>> liveRanges;
    public InterferenceGraph(BasicBlock block) {
        // even indecies of the arraylist are the start of a range and odd ones are the end
        this.liveRanges = new HashMap<String, ArrayList<Integer>>();
        this.vertices = new ArrayList<InstrVertex>();

        for (IRInstruction instr : block.instructions) {
            // setup
            if (instr.getDefOperand() instanceof IRVariableOperand) {
                liveRanges.put(instr.getDefOperand().toString(), new ArrayList<Integer>());
            }
            InstrVertex vertex = new InstrVertex();
            vertex.instruction = instr;
            vertex.edges = new ArrayList<IRInstruction>();
            // colors will start at 0, for now no color.

            ArrayList<Integer> liveRange = getLiveRange(instr, block);
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
    private boolean overlap(ArrayList<Integer> range1, ArrayList<Integer> range2) {
        // if range 2 starts after range 1 ends or vice versa descriobes all situations without overlap (i think at 11pm)
        for (int i = 0; i < range1.size(); i += 2) {
            int r1Start = range1.get(i);
            int r1End = range1.get(i + 1);
            for (int j = 0; j < range2.size(); j += 2) {
                int r2Start = range2.get(j);
                int r2End = range2.get(j + 1);
                // range 2 start is between range 1 start and end or
                if ((r1Start < r2Start  && r1End > r1Start) ||
                // range 2 end is between range 1 start and end or
                    (r1Start < r2End && r1End > r2End) ||
                // range 1 is entirely contained by range 2
                    (r1Start > r2Start && r1End < r2End)) {
                        return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Integer> getLiveRange(IRInstruction instr, BasicBlock block) {
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
            if (otherInstr.gen && 
                otherInstr.getDefOperand().toString().equals(instr.getDefOperand().toString()) && 
                otherInstr.irLineNumber > instr.irLineNumber) {
                // if the other instruction defines the same register as the passed instruction
                // and it is on a later line than this instruction then we have found the limit of this
                // instruction's live range.
                maxUseLine = otherInstr.irLineNumber;
                break;
            }
        }
        this.liveRanges.get(instr.getDefOperand().toString()).add(instr.irLineNumber);
        this.liveRanges.get(instr.getDefOperand().toString()).add(maxUseLine);
        return liveRanges.get(instr.getDefOperand().toString());
    }
    
    // returns a map of String var name to integer register assignment
    // 0-9 coorespond to t0 - t9, and 10 means the variable is spilled
    public HashMap<String, Integer> getRegMap() {
        HashMap<String, Integer> out = new HashMap<String, Integer>();
        
        HashMap<String, Integer> rangeLens = new HashMap<String, Integer>();
        for (String var : this.liveRanges.keySet()) {
            ArrayList<Integer> live = this.liveRanges.get(var);
            int rangeSize = 0;
            for (int i = 0; i < live.size(); i += 2) {
                rangeSize += (live.get(i - 1) - live.get(i));
            }
            rangeLens.put(var, rangeSize);
        }
        // now our rangeLens hashmap has each var to the range size
        // assign each of 10 registers (0-9) to the largest range sizes
        for (int i = 0; i < 10; i ++) {
            int highest = 0;
            for (String var : rangeLens.keySet()) {
                if (rangeLens.get(var) > highest) {
                    // add to our output
                    out.put(var, i);
                    // remove from our working list
                    rangeLens.remove(var);
                }
            }
        }
        // assign the register #10 to the remaining variables, these will be spilled
        for (String var : rangeLens.keySet()) {
            out.put(var, 10);
        }
        return out;
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
        String out = "INTERFERENCE GRAPH:\n";
        for (InstrVertex v : this.vertices) {
            out += "  VERTEX:\n";
            out += "  " + v.instruction.toString() + "\n";
            out += "    EDGES:\n";
            for (IRInstruction edge : v.edges) {
                out += "    - " + edge.toString() + "\n";
            }
            out += "\n";
        }
        return out;
    }
} 
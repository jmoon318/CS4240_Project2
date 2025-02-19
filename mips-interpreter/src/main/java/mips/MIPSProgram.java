package main.java.mips;

import java.util.List;
import java.util.Map;

public class MIPSProgram {

   public Map<Integer, MIPSInstruction> instructions;
   public Map<Integer, Integer> data;
   public Map<String, Integer> labels;

   public MIPSProgram(Map<Integer, MIPSInstruction> instructions,
                      Map<Integer, Integer> data, Map<String, Integer> labels) {
       this.instructions = instructions;
       this.data = data;
       this.labels = labels;
   }

   public void printLabels() {
       for (String label : labels.keySet()) {
           System.out.println(label + " -> " + Integer.toHexString(labels.get(label)));
       }
   }
}

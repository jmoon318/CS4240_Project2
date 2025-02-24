import java.util.ArrayList;
import java.util.HashMap;

import ir.operand.IROperand;

public class LiveSet {
    // this will hold the actual live set as a String variable name mapped to
    // an array of integers. This array contains the start of a range in the even
    // indecies and the end of a range in the odd ones. If there is only one live
    // range for this variable then [0] has the start line number and [1] has the end. 
    // the var is live for all lines in between
    private HashMap<String, Integer[]> liveSet;

    // builds the live set for the function given it's head basic block.
    public LiveSet(BasicBlock head) {
        ArrayList<BasicBlock> worklist = head.getBlockList();
        while (worklist.size() > 0) {
            BasicBlock block = worklist.get(0);
            worklist.remove(0);


            @SuppressWarnings("unchecked") 
            // we want to make sure that this list is not updated when computing the new one on the block
            ArrayList<IROperand> oldLiveIn = (ArrayList<IROperand>) block.getLiveIn().clone();
            block.computeLiveOut();
            ArrayList<IROperand> newLiveIn = block.computeLiveIn();
            // if the LiveIn changed then add predecessors to the worklist.
            if (!newLiveIn.equals(oldLiveIn)) {
                worklist.addAll(block.pre);
            }
        }
        // now we have the final LiveIn and LiveOut for each BB
        
    }
}

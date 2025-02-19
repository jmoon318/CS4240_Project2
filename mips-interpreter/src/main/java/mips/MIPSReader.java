package main.java.mips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import main.java.mips.operand.*;
import main.java.exceptions.*;

public class MIPSReader {

    // compiler directives
    private static String directivePat = "\\.[a-zA-Z]+";

    private static String opPat;
    private static String labelPat = "[a-zA-Z_]\\w*:";

    // operands
    private static String registerPat = "\\$[a-zA-Z0-9]+|zero";
    private static String immHexPat = "0x[a-fA-F0-9]+";
    private static String immDecPat = "-?[0-9]+";
    private static String immFloatingPointPat = "-?[0-9]+\\.[0-9]*";

    // addressing modes
    private static String addrPCRelativePat = "[a-zA-Z_]\\w*";
    private static String addrRegisterPat = "\\((\\$[a-zA-Z0-9]+|zero)\\)";
    private static String addrBaseOffPat = "(0x[a-fA-F0-9]+|-?[0-9]+)\\((\\$[a-zA-Z0-9]+|zero)\\)";

    private static Map<String, MIPSOp> opcodes;

    static {
        // build pattern for recognizing MIPS opcodes
        opcodes = new HashMap<>();
        MIPSOp[] ops = MIPSOp.values();

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ops.length; i++) {
            opcodes.put(ops[i].toString(), ops[i]);

            builder.append(ops[i].toString());
            if (i != ops.length - 1) {
                builder.append("|");
            }
        }

        opPat = builder.toString();
    }

    public MIPSProgram parseMIPSFile(String filename) throws IOException {
        BufferedReader reader;

        Map<Integer, MIPSInstruction> instructions = new HashMap<>();
        Map<Integer, Integer> data = new HashMap<>();
        Map<String, Integer> labels = new HashMap<>();

        reader = new BufferedReader(new FileReader(filename));
        String line = reader.readLine();

        String label = null;
        int nextAddr = 0;
        boolean isWord = false;

        int lineNum = 0;

        while (line != null) {

            if (line.equals("")) {
                line = reader.readLine();
                continue;
            }

            String[] tokens = line.split("\\s+|,");

            MIPSOp op = null;
            List<MIPSOperand> operands = new ArrayList<>();
            boolean comment = false;

            for (String token : tokens) {
                // trim trailing commas
                token = token.replaceAll(",|\\s+", "");

                if (token.length() == 0) {
                    continue;
                } else if (comment || token.charAt(0) == '#') {
                    comment = true;
                    continue;
                }

                if (isWord) {
                    if (token.matches(immHexPat)) {
                        data.put(nextAddr, Integer.decode(token));
                        nextAddr += 4;
                        continue;
                    } else if (token.matches(immDecPat)) {
                        data.put(nextAddr, Integer.parseInt(token));
                        nextAddr += 4;
                        continue;
                    } else {
                        isWord = false;
                    }
                }

                if (token.matches(directivePat)) {
                    switch (token) {
                        case ".text":
                            nextAddr = MemLayout.TEXT;
                            break;
                        case ".data":
                            nextAddr = MemLayout.DATA;
                            break;
                        case ".word":
                            isWord = true;
                        default:
                            isWord = true;
                            break;
                    }
                } else if (token.matches(opPat)) {
                    op = opcodes.get(token);
                } else if (token.matches(labelPat)) {
                    label = token.replaceAll(":$", "");
                    if (labels.containsKey(label)) {
                        throw new ParseException("duplicate label found: " + label + " (@ line " + lineNum + ")");
                    }
                    labels.put(label, nextAddr);
                } else if (token.matches(registerPat)) {
                    operands.add(new Register(token));
                } else if (token.matches(immHexPat)) {
                    if (isWord) {
                        data.put(nextAddr, Integer.decode(token));
                    } else {
                        operands.add(new Imm(token, "HEX"));
                    }
                } else if (token.matches(immFloatingPointPat)) {
                    if (op.precision.equals("s")) {
                        operands.add(new Imm(token, "SINGLE"));
                    } else {
                        operands.add(new Imm(token, "DOUBLE"));
                    }
                } else if (token.matches(immDecPat)) {
                    if (isWord) {
                        data.put(nextAddr, Integer.parseInt(token));
                    } else {
                        operands.add(new Imm(token, "DEC"));
                    }
                } else if (token.matches(addrPCRelativePat)) {
                    operands.add(new Addr(token));
                } else if (token.matches(addrRegisterPat)) {
                    Register reg = new Register(token.replaceAll("\\(|\\)", ""));
                    operands.add(new Addr(reg));
                } else if (token.matches(addrBaseOffPat)) {
                    int i = token.indexOf('(');
                    String offset = token.substring(0, i);
                    Imm imm;
                    if (offset.matches(immHexPat)) {
                        imm = new Imm(offset, "HEX");
                    } else {
                        imm = new Imm(offset, "DEC");
                    }

                    String base = token.substring(i);
                    operands.add(new Addr(imm,
                            new Register(base.replaceAll("\\(|\\)", ""))));
                } else {
                    throw new ParseException("unknown token: " + token + " (@ line " + lineNum + ")");
                }
            }

            if (op != null) {
                MIPSOperand[] operandsAsArray = new MIPSOperand[operands.size()];
                for (int i = 0; i < operandsAsArray.length; i++) {
                    operandsAsArray[i] = operands.get(i);
                }
                instructions.put(nextAddr, new MIPSInstruction(op, label, operandsAsArray));
                // clear label

                label = null;
                nextAddr += 4;
            }

            // read next line
            line = reader.readLine();
            lineNum++;
        }
        reader.close();

        return new MIPSProgram(instructions, data, labels);
    }
}

package main.java.mips;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.nio.ByteBuffer;

import main.java.mips.operand.*;
import main.java.exceptions.*;

public class MIPSInterpreter {

    // syscall table
    private static final int PRINT_INT = 1;
    private static final int PRINT_FLOAT = 2;
    private static final int PRINT_DOUBLE = 3;
    private static final int READ_INT = 5;
    private static final int READ_FLOAT = 6;
    private static final int READ_DOUBLE = 7;
    private static final int SBRK = 9;
    private static final int EXIT = 10;
    private static final int PRINT_CHAR = 11;

    private static final int WORD_SIZE = 4;
    private static final int BYTE = 1;

    // for parsing user commands in debug mode
    private static String printRegPat = "p \\$[a-z0-9]+|zero";
    private static String examineMemPat = "x(/-?[0-9]+)? (0x[a-fA-F0-9]+|-?[0-9]+)\\((\\$[a-z0-9]+|zero)\\)";
    private static String gotoPat = "g [a-zA-Z_]\\w*";
    private static String immHexPat = "0x[a-fA-F0-9]+";

    public MIPSProgram program;

    private MIPSReader reader;

    private int pc;
    private Map<String, Integer> regSet;
    private Map<Integer, Integer> mem;
    private int next_heap_addr = 0x10040000; // Assume heap starts at this address

    private Map<String, String> regTypes;

    private boolean finished;
    private boolean control;

    private Scanner input;
    private BufferedReader inputReader;
    private boolean debug;

    public MIPSInterpreter() {
        this(false);
    }

    public MIPSInterpreter(boolean debug) {
        reader = new MIPSReader();

        pc = MemLayout.TEXT;
        regSet = new HashMap<>();
        mem = new HashMap<>();
        regTypes = new HashMap<>();

        // populate reg set
        writeRegister("$sp", MemLayout.STACK);
        writeRegister("$a0", 0);
        writeRegister("$a1", 0);
        writeRegister("$a2", 0);
        writeRegister("$a3", 0);

        writeRegister("$ra", 0);
        writeRegister("$v0", 0);

        writeRegister("$fp", 0);
        writeRegister("$s0", 0);

        writeRegister("zero", 0);
        writeRegister("$zero", 0);
        writeRegister("$0", 0);

        // populate floating point regs
        for (int i = 0; i < 32; i++) {
            writeRegister("$f" + i, 0.0f);
        }

        input = new Scanner(System.in);
        this.debug = debug;
    }

    public void setDebug(boolean val) {
        this.debug = val;
    }

    public void setInputReader(BufferedReader reader) {
        this.inputReader = reader;
    }

    public void run(String filename) {
        try {
            program = reader.parseMIPSFile(filename);
            mem = program.data;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            finished = true;
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            finished = true;
        }

        String command;
        boolean go = false;
        String stopLabel = null;

        while (!finished) {
            if (go || !debug) {
                try {
                    MIPSInstruction inst = program.instructions.get(pc);
                    if (go && stopLabel.equals(inst.label)) {
                        go = false;
                    } else {
                        execute(inst);
                    }
                } catch (IllegalReadException e) {
                    System.out.println(e.getMessage());
                    System.out.println("    " + program.instructions.get(pc));
                    finished = true;
                } catch (IllegalWriteException e) {
                    System.out.println(e.getMessage());
                    System.out.println("    " + program.instructions.get(pc));
                    finished = true;
                }
            } else if (debug) {
                System.out.println("next -> " + program.instructions.get(pc));
                System.out.print("> ");
                command = input.nextLine();

                if (command.matches(printRegPat)) {
                    String reg = command.substring(2);
                    if (regSet.containsKey(reg)) {
                        if (regTypes.get(reg).equals("INT")) {
                            System.out.println("\n  " + reg + ": "
                                + String.format("0x%08X", readIntRegister(reg)) + "\n");
                        } else if (regTypes.get(reg).equals("FP")) {
                            System.out.println("\n  " + reg + ": "
                                + String.format("%.7f", readDoubleRegister(reg)) + "\n");
                        }
                    } else {
                        System.out.println("\n  tried to read unknown register\n");
                    }
                } else if (command.matches(examineMemPat)) {
                    String addr = command.substring(command.indexOf(' ') + 1);

                    int i = addr.indexOf('(');
                    String offset = addr.substring(0, i);
                    Imm imm;
                    if (offset.matches(immHexPat)) {
                        imm = new Imm(offset, "HEX");
                    } else {
                        imm = new Imm(offset, "DEC");
                    }

                    int n;

                    int j = command.indexOf('/');

                    if (j != -1) {
                        String count = command.substring(j + 1, command.indexOf(' '));
                        n = Integer.parseInt(count);
                    } else {
                        n = 1;
                    }

                    String base = addr.substring(i);
                    Addr address = new Addr(imm, new Register(base.replaceAll("\\(|\\)", "")));
                    int addrVal = addrVal(address);

                    System.out.println();
                    int start = addrVal;
                    if (n < 0) {
                        start += ((n * WORD_SIZE) + WORD_SIZE);
                    }

                    for (i = 0; i < Math.abs(n); i++) {
                        String pointers = "";
                        addrVal = start + (i * WORD_SIZE);
                        if (readIntRegister("$sp") == addrVal) {
                            pointers += " <-- $sp";
                        }
                        if (readIntRegister("$fp") == addrVal) {
                            pointers += " <-- $fp";
                        }

                        System.out.println("  " + String.format("0x%08X", addrVal)
                            + ": " + String.format("0x%08X", readMemInt(addrVal)) + pointers);
                    }

                    System.out.println();
                } else if (command.matches(gotoPat)) {
                    go = true;
                    // extract label
                    stopLabel = command.substring(2);
                } else if (command.equals("exit")) {
                    finished = true;
                } else if (command.length() != 0) {
                    System.out.println("\n  command not recognized; try again.\n");
                } else {
                    execute(program.instructions.get(pc));
                }
            }
        }
    }

    private void execute(MIPSInstruction inst) {
        Register dest;
        Register rs;
        Register rt;
        Imm imm;
        Addr addr;

        int memData;
        float memDataSingle;
        double memDataDouble;

        switch (inst.op) {
            case ADD:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readIntRegister(rs.name) + readIntRegister(rt.name));
                pc += 4;
                return;
            case ADDI:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                imm = (Imm)inst.operands.get(2);

                writeRegister(dest.name,
                        readIntRegister(rs.name) + imm.getInt());
                pc += 4;
                return;
            case SUB:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readIntRegister(rs.name) - readIntRegister(rt.name));
                pc += 4;
                return;
            case MUL:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readIntRegister(rs.name) * readIntRegister(rt.name));
                pc += 4;
                return;
            case DIV:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readIntRegister(rs.name) / readIntRegister(rt.name));
                pc += 4;
                return;
            case AND:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readIntRegister(rs.name) & readIntRegister(rt.name));
                pc += 4;
                return;
            case ANDI:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                imm = (Imm)inst.operands.get(2);

                writeRegister(dest.name,
                        readIntRegister(rs.name) & imm.getInt());
                pc += 4;
                return;
            case OR:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readIntRegister(rs.name) | readIntRegister(rt.name));
                pc += 4;
                return;
            case ORI:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                imm = (Imm)inst.operands.get(2);

                writeRegister(dest.name,
                        readIntRegister(rs.name) | imm.getInt());
                pc += 4;
                return;
            case SLL:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                imm = (Imm)inst.operands.get(2);

                writeRegister(dest.name,
                        readIntRegister(rs.name) << imm.getInt());
                pc += 4;
                return;
            case LI:
                dest = inst.getWrite();
                imm = (Imm)inst.operands.get(1);
                writeRegister(dest.name, imm.getInt());
                pc += 4;
                return;
            case LA:
                dest = inst.getWrite();
                addr = (Addr)inst.operands.get(1);
                writeRegister(dest.name, addrVal(addr));
                pc += 4;
                return;
            case LW:
                dest = inst.getWrite();
                addr = (Addr)inst.operands.get(1);

                memData = readMemInt(addrVal(addr));
                writeRegister(dest.name, memData);
                pc += 4;
                return;
            case MOVE:
                dest = inst.getWrite();
                rs = inst.getReads()[0];

                writeRegister(dest.name, readIntRegister(rs.name));
                pc += 4;
                return;
            case SW:
                addr = (Addr)inst.operands.get(1);
                rs = inst.getReads()[0];

                writeMemInt(addrVal(addr), readIntRegister(rs.name));
                pc += 4;
                return;
            case BEQ:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                if (readIntRegister(rs.name) == readIntRegister(rt.name)) {
                    addr = (Addr)inst.operands.get(2);
                    pc = addrVal(addr);
                } else {
                    pc += 4;
                }

                return;
            case BNE:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                if (readIntRegister(rs.name) != readIntRegister(rt.name)) {
                    addr = (Addr)inst.operands.get(2);
                    pc = addrVal(addr);
                } else {
                    pc += 4;
                }

                return;
            case BLT:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                if (readIntRegister(rs.name) < readIntRegister(rt.name)) {
                    addr = (Addr)inst.operands.get(2);
                    pc = addrVal(addr);
                } else {
                    pc += 4;
                }

                return;
            case BGT:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                if (readIntRegister(rs.name) > readIntRegister(rt.name)) {
                    addr = (Addr)inst.operands.get(2);
                    pc = addrVal(addr);
                } else {
                    pc += 4;
                }

                return;
            case BGE:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                if (readIntRegister(rs.name) >= readIntRegister(rt.name)) {
                    addr = (Addr)inst.operands.get(2);
                    pc = addrVal(addr);
                } else {
                    pc += 4;
                }

                return;
            case J:
                addr = (Addr)inst.operands.get(0);
                pc = addrVal(addr);
                return;
            case JAL:
                writeRegister("$ra", (int)pc + 4);
                addr = (Addr)inst.operands.get(0);
                pc = addrVal(addr);
                return;
            case JR:
                rs = inst.getReads()[0];
                pc = readIntRegister(rs.name);

                return;
            case SYSCALL:
                switch (readIntRegister("$v0")) {
                    case PRINT_INT:
                        System.out.print(readIntRegister("$a0"));
                        break;
                    case PRINT_FLOAT:
                        System.out.print(readSingleRegister("$f12"));
                        if (debug) {
                            System.out.println();
                        }
                        break;
                    case PRINT_DOUBLE:
                        System.out.print(readDoubleRegister("$f12"));
                        if (debug) {
                            System.out.println();
                        }
                        break;
                    case READ_INT:
                        writeRegister("$v0", readInt());
                        break;
                    case READ_FLOAT:
                        writeRegister("$f0", readSingle());
                        break;
                    case READ_DOUBLE:
                        writeRegister("$f0", readDouble());
                        break;
                    case SBRK:
                        int n_bytes = readIntRegister("$a0");
                        
                        // Return address to heap allocated buffer
                        writeRegister("$v0", next_heap_addr);

                        // Zero-initialize heap allocated buffer
                        for(int i = 0; i < n_bytes; i++) {
                            writeMemH(next_heap_addr, 0, BYTE);
                            next_heap_addr += 1;
                        }
                        break;
                    case EXIT:
                        finished = true;
                        break;
                    case PRINT_CHAR:
                        int val = readIntRegister("$a0");
                        char c = (char)val;
                        System.out.print(c);
                        if (debug && c != 10) {
                            System.out.println();
                        }
                        break;
                }

                pc += 4;
                return;
            case ADD_S:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readSingleRegister(rs.name) + readSingleRegister(rt.name));
                pc += 4;
                return;
            case ADDI_S:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                imm = (Imm)inst.operands.get(2);

                writeRegister(dest.name,
                        readSingleRegister(rs.name) + imm.getSingle());
                pc += 4;
                return;
            case SUB_S:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readSingleRegister(rs.name) - readSingleRegister(rt.name));
                pc += 4;
                return;
            case MUL_S:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readSingleRegister(rs.name) * readSingleRegister(rt.name));
                pc += 4;
                return;
            case DIV_S:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readSingleRegister(rs.name) / readSingleRegister(rt.name));
                pc += 4;
                return;
            case LI_S:
                dest = inst.getWrite();
                imm = (Imm)inst.operands.get(1);
                writeRegister(dest.name, imm.getSingle());
                pc += 4;
                return;
            case MOV_S:
                dest = inst.getWrite();
                rs = inst.getReads()[0];

                writeRegister(dest.name, readSingleRegister(rs.name));
                pc += 4;
                return;
            case L_S:
                dest = inst.getWrite();
                addr = (Addr)inst.operands.get(1);

                memDataSingle = readMemSingle(addrVal(addr));
                writeRegister(dest.name, memDataSingle);
                pc += 4;
                return;
            case S_S:
                addr = (Addr)inst.operands.get(1);
                rs = inst.getReads()[0];

                writeMemSingle(addrVal(addr), readSingleRegister(rs.name));
                pc += 4;
                return;
            case C_EQ_S:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readSingleRegister(rs.name) == readSingleRegister(rt.name));
                pc += 4;
                return;
            case C_NE_S:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readSingleRegister(rs.name) != readSingleRegister(rt.name));
                pc += 4;
                return;
            case C_LT_S:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readSingleRegister(rs.name) < readSingleRegister(rt.name));
                pc += 4;
                return;
            case C_GT_S:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readSingleRegister(rs.name) > readSingleRegister(rt.name));
                pc += 4;
                return;
            case C_GE_S:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readSingleRegister(rs.name) >= readSingleRegister(rt.name));
                pc += 4;
                return;
            case ADD_D:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readDoubleRegister(rs.name) + readDoubleRegister(rt.name));
                pc += 4;
                return;
            case ADDI_D:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                imm = (Imm)inst.operands.get(2);

                writeRegister(dest.name,
                        readDoubleRegister(rs.name) + imm.getDouble());
                pc += 4;
                return;
            case SUB_D:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readDoubleRegister(rs.name) - readDoubleRegister(rt.name));
                pc += 4;
                return;
            case MUL_D:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readDoubleRegister(rs.name) * readDoubleRegister(rt.name));
                pc += 4;
                return;
            case DIV_D:
                dest = inst.getWrite();
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                writeRegister(dest.name,
                        readDoubleRegister(rs.name) / readDoubleRegister(rt.name));
                pc += 4;
                return;
            case LI_D:
                dest = inst.getWrite();
                imm = (Imm)inst.operands.get(1);
                writeRegister(dest.name, imm.getDouble());
                pc += 4;
                return;
            case MOV_D:
                dest = inst.getWrite();
                rs = inst.getReads()[0];

                writeRegister(dest.name, readDoubleRegister(rs.name));
                pc += 4;
                return;
            case L_D:
                dest = inst.getWrite();
                addr = (Addr)inst.operands.get(1);

                memDataDouble = readMemDouble(addrVal(addr));
                writeRegister(dest.name, memDataDouble);
                pc += 4;
                return;
            case S_D:
                addr = (Addr)inst.operands.get(1);
                rs = inst.getReads()[0];

                writeMemDouble(addrVal(addr), readDoubleRegister(rs.name));
                pc += 4;
                return;
            case C_EQ_D:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readDoubleRegister(rs.name) == readDoubleRegister(rt.name));
                pc += 4;
                return;
            case C_NE_D:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readDoubleRegister(rs.name) != readDoubleRegister(rt.name));
                pc += 4;
                return;
            case C_LT_D:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readDoubleRegister(rs.name) < readDoubleRegister(rt.name));
                pc += 4;
                return;
            case C_GT_D:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readDoubleRegister(rs.name) > readDoubleRegister(rt.name));
                pc += 4;
                return;
            case C_GE_D:
                rs = inst.getReads()[0];
                rt = inst.getReads()[1];

                control = (readDoubleRegister(rs.name) >= readDoubleRegister(rt.name));
                pc += 4;
                return;
            case BC1T:
                addr = (Addr)inst.operands.get(0);
                if (control) {
                    pc = addrVal(addr);
                } else {
                    pc += 4;
                }
                return;
            case BC1F:
                addr = (Addr)inst.operands.get(0);
                if (!control) {
                    pc = addrVal(addr);
                } else {
                    pc += 4;
                }
                return;
            default:
                pc += 4;
        }
    }

    private int readInt() {
        if (inputReader != null) {
            try {
                String next = inputReader.readLine();
                if (next == null) {
                    throw new IllegalReadException("something wrong with input file");
                }
                return Integer.parseInt(next);
            } catch (IOException e) {
                throw new IllegalReadException("something wrong with input file");
            }
        }

        int ans = input.nextInt();
        input.nextLine();
        return ans;
    }

    private float readSingle() {
        if (inputReader != null) {
            try {
                String next = inputReader.readLine();
                if (next == null) {
                    throw new IllegalReadException("something wrong with input file");
                }
                return Float.parseFloat(next);
            } catch (IOException e) {
                throw new IllegalReadException("something wrong with input file");
            }
        }

        float ans = input.nextFloat();
        input.nextLine();
        return ans;
    }

    private double readDouble() {
        if (inputReader != null) {
            try {
                String next = inputReader.readLine();
                if (next == null) {
                    throw new IllegalReadException("something wrong with input file");
                }
                return Double.parseDouble(next);
            } catch (IOException e) {
                throw new IllegalReadException("something wrong with input file");
            }
        }

        double ans = input.nextDouble();
        input.nextLine();
        return ans;
    }

    private int readIntRegister(String name) {
        if (!regSet.containsKey(name)) {
            throw new IllegalReadException("tried to read uninitialized register: " + name);
        } else if (!regTypes.get(name).equals("INT")) {
            throw new IllegalReadException("tried to read floating-point register as an int: " + name);
        }

        return regSet.get(name);
    }

    private float readSingleRegister(String name) {
        if (!regSet.containsKey(name)) {
            throw new IllegalReadException("tried to read uninitialized register: " + name);
        } else if (!regTypes.get(name).equals("FP")) {
            throw new IllegalReadException("tried to read int register as floating point:" + name);
        }

        int word = regSet.get(name);
        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE).putInt(0, word);
        return bytes.getFloat();
    }

    private double readDoubleRegister(String name) {
        if (!regSet.containsKey(name)) {
            throw new IllegalReadException("tried to read uninitialized register: " + name);
        } else if (!regTypes.get(name).equals("FP")) {
            throw new IllegalReadException("tried to read int register as floating point:" + name);
        }

        String regLetter = name.substring(1, 2);
        int regNum = Integer.parseInt(name.substring(2, name.length()));

        if (regNum % 2 != 0) {
            throw new IllegalWriteException("attempted to read double-precision value to single-precision register");
        }

        int word = regSet.get(name);
        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE * 2);
        bytes.putInt(0, word);
        
        word = regSet.get("$" + regLetter + (regNum + 1));
        bytes.putInt(WORD_SIZE, word);
        return bytes.getDouble();
    }

    private void writeRegister(String name, int data) {
        if (regSet.containsKey(name)) {
            if (!regTypes.get(name).equals("INT")) {
                throw new IllegalWriteException("attempted to write int to a floating-point register");
            }
        }

        regSet.put(name, data);
        regTypes.put(name, "INT");
    }

    private void writeRegister(String name, float data) {
        if (regSet.containsKey(name)) {
            if (!regTypes.get(name).equals("FP")) {
                throw new IllegalWriteException("attempted to write floatin point to an int register");
            }
        }

        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE).putFloat(0, data);
        regSet.put(name, bytes.getInt());
        regTypes.put(name, "FP");
    }

    private void writeRegister(String name, double data) {
        if (regSet.containsKey(name)) {
            if (!regTypes.get(name).equals("FP")) {
                throw new IllegalWriteException("attempted to write floating point to an int register");
            }
        }

        String regLetter = name.substring(1, 2);
        int regNum = Integer.parseInt(name.substring(2, name.length()));

        if (regNum % 2 != 0) {
            throw new IllegalWriteException("attempted to write floating point to an int register");
        }

        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE * 2).putDouble(0, data);
        regSet.put(name, bytes.getInt());
        // System.out.print(name + ": " + regSet.get(name));
        name = "$" + regLetter + (regNum + 1);
        regSet.put(name, bytes.getInt());
        // System.out.print(" " + name + ": " + regSet.get(name) + "\n");
        regTypes.put(name, "FP");
    }

    private int readMemInt(int addr) {
        return readMemH(addr, WORD_SIZE);
    }

    private float readMemSingle(int addr) {
        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE);
        bytes.putInt(0, readMemH(addr, WORD_SIZE));
        return bytes.getFloat();
    }

    private double readMemDouble(int addr) {
        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE * 2);
        bytes.putInt(0, readMemH(addr, WORD_SIZE));
        bytes.putInt(WORD_SIZE, readMemH(addr + 4, WORD_SIZE));
        return bytes.getDouble();
    }

    private int readMemH(int addr, int size) {
        if (Integer.compareUnsigned(addr, MemLayout.DATA) < 0) {
            throw new IllegalReadException("Memory read below .data: " + String.format("0x%08X", addr));
        } else if (Integer.compareUnsigned(addr, MemLayout.KTEXT) >= 0) {
            throw new IllegalReadException("Memory read above .ktext: " + String.format("0x%08X", addr));
        }

        // if address is byte-aligned
        if (addr % 4 == 0) {
            switch (size) {
                case WORD_SIZE:
                    return mem.getOrDefault(addr, 0);
                case BYTE:
                    return mem.getOrDefault(addr, 0) & 0xFF;
                default:
                    throw new IllegalReadException("memory read illegal size: " + size);
            }
        } else {
            int offset = (int)addr % 4;
            int aligned = addr - offset;

            if (mem.containsKey(aligned)) {
                switch (size) {
                    case WORD_SIZE:
                        int data = (mem.get(aligned) >> (offset * 8)) & (0xFFFFFFFF >>> (offset * 8));
                        int next = aligned + 4;

                        for (int i = 0; i < offset; i++) {
                            data |= ((mem.getOrDefault(next, 0) >> (i * 8)) & 0xFF) << ((4 - offset + i) * 8);
                        }
                        return data;
                    case BYTE:
                        return (mem.get(aligned) >> (offset * 8)) & 0xFF;
                    default:
                        throw new IllegalReadException("memory read illegal size: " + size);
                }
            } else {
                return 0;
            }
        }
    }

    private void writeMemInt(int addr, int data) {
        writeMemH(addr, data, WORD_SIZE);
    }

    private void writeMemSingle(int addr, float data) {
        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE).putFloat(0, data);
        writeMemH(addr, bytes.getInt(), WORD_SIZE);
    }

    private void writeMemDouble(int addr, double data) {
        ByteBuffer bytes = ByteBuffer.allocate(WORD_SIZE * 2).putDouble(0, data);
        writeMemH(addr, bytes.getInt(), WORD_SIZE);
        writeMemH(addr + WORD_SIZE, bytes.getInt(), WORD_SIZE);
    }

    private void writeMemH(int addr, int data, int size) {
        if (Integer.compareUnsigned(addr, MemLayout.DATA) < 0) {
            throw new IllegalWriteException("memory write below .data: " + String.format("0x%08X", addr));
        } else if (Integer.compareUnsigned(addr, MemLayout.KTEXT) >= 0) {
            throw new IllegalWriteException("memory write above .ktext: " + String.format("0x%08X", addr));
        }

        // if address is byte-aligned
        if (addr % 4 == 0) {
            switch (size) {
                case WORD_SIZE:
                    mem.put(addr, data);
                    return;
                case BYTE:
                    int val = mem.getOrDefault(addr, 0);
                    mem.put(addr, val | (data & 0xFF));
                    return;
                default:
                    throw new IllegalWriteException("Memory write illegal size: " + size);
            }
        } else {
            int offset = (int)addr % 4;
            int aligned = addr - offset;

            int val;
            switch (size) {
                case WORD_SIZE:
                    int low = data & (0xFFFFFFFF >>> ((4 - offset) * 8));
                    int high = (data >> ((4 - offset) * 8)) & (0xFFFFFFFF >>> ((4 - offset) * 8));

                    mem.put(aligned, mem.getOrDefault(aligned, 0) | (low << (offset * 8)));
                    mem.put(aligned + 4, mem.getOrDefault(aligned + 4, 0) | high);
                    return;
                case BYTE:
                    val = mem.getOrDefault(aligned, 0);
                    mem.put(addr, val | ((data & 0xFF) << (offset * 8)));
                    return;
                default:
                    throw new IllegalReadException("Memory write illegal size: " + size);
            }
        }
    }

    private int addrVal(Addr addr) {
        switch (addr.mode) {
            case PC_RELATIVE:
                return program.labels.get(addr.label);
            case REGISTER:
                return readIntRegister(addr.register.name);
            case BASE_OFFSET:
                return readIntRegister(addr.register.name) + addr.constant.getInt();
            default:
                return 0;
        }
    }

    public static void main(String[] args) {
        MIPSInterpreter interpreter = new MIPSInterpreter();
        // parse arguments
        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];

            if (arg.equals("--debug")) {
                interpreter.setDebug(true);
            } else if (arg.equals("--in")) {
                try {
                    String filename = args[++i];
                    BufferedReader reader = new BufferedReader(new FileReader(filename));
                    interpreter.setInputReader(reader);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    System.exit(1);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("incorrect argument format around " + arg);
                    System.exit(1);
                }
            } else {
                System.out.println("unrecognized argument: " + arg);
                System.exit(1);
            }
        }

        interpreter.run(args[args.length - 1]);
    }
}

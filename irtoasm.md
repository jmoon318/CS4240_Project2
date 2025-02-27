assign, a, b
LB Load Byte MIPS I
LBU Load Byte Unsigned I
SB Store Byte I
LH Load Halfword I
LHU Load Halfword Unsigned I
SH Store Halfword I
LW Load Word I
SW Store Word I

add, t, a, b
sub, t, a, b
mult, t, a, b
div, t, a, b
and, t, a, b
or, t, a, b

ADDI
ADDIU Add Immediate Unsigned Word 
SLTI Set on Less Than Immediate 
SLTIU Set on Less Than Immediate Unsigned 
ANDI And Immediate 
ORI Or Immediate 
XORI Exclusive Or Immediate 
LUI Load Upper Immediate 
MULT
MULTU Multiply Unsigned Word 
DIV
DIVU Divide Unsigned Word 
ADD
ADDU Add Unsigned Word 
SUB
SUBU Subtract Unsigned Word 
SLT Set on Less Than 
SLTU Set on Less Than Unsigned 
AND
OR
XOR
NOR


breq, after_if_part, a, b
brneq, after_if_part, a, b
brlt, after_if_part, a, b
brgt, after_if_part, a, b
brgeq, after_if_part, a, b

BEQ 
BNE 
BLEZ 
BGTZ 


goto, after loop
return, a
call, foo, x
callr, a, foo, x, y, z

J Jump MIPS I
JAL Jump and Link I
JR Jump Register MIPS I
JALR Jump and Link Register I
SYSCALL System Call MIPS I
BREAK Breakpoint I

array store, a, arr, 0
array load, a, arr, 0
assign, X, 100, 10

[more detail on each MIPS instruction starting page 40 of this document](https://www.cs.cmu.edu/afs/cs/academic/class/15740-f97/public/doc/mips-isa.pdf)
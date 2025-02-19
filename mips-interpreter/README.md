# CS 4240 MIPS Interpreter

MIPS interpreter that lets you execute assembly code designed for the MIPS32 architecture.
It can execute assembly code that uses either physical or virtual registers. Virtual registers must still begin with a `$`, and their names are limited to a-z, A-Z, 0-9 - no special characters.

## Description
Currently, the MIPS interpreter can execute a subset of the SPIM instruction set. [SPIM](https://en.wikipedia.org/wiki/SPIM#targetText=SPIM%20is%20a%20MIPS%20processor,the%20University%20of%20Wisconsin%E2%80%93Madison.) is a MIPS simulator. It also supports a subset of MIPS addressing modes and assembler directives. The main purpose of this tool is to help students in debugging their implementation of project2.

## Usage
Navigate to the root directory of this project. To compile, run:

```
mkdir build
javac -d build -cp src src/main/java/mips/MIPSInterpreter.java
```

To run a MIPS program, run:

```
java -cp build main.java.mips.MIPSInterpreter file.s
```

Replace `file.s` with the path to the file you want to run.

To run a MIPS program with inputs stored in another file, run:

```
java -cp build main.java.mips.MIPSInterpreter file.s < in
```

or

```
java -cp build main.java.mips.MIPSInterpreter --in in file.s
```

Replace `file.s` and `in` with the names of the MIPS assembly file and input file, respectively.

## Debugger
MIPS interpreter comes with simple debugging features that might be helpful if you have errors in
your code. The debugger lets you run programs instruction by instruction, print register values, and
examine memory contents.

To run a MIPS program with debugging turned on, run:

```
java -cp build main.java.mips.MIPSInterpreter --debug file.s
```

Replace `file.s` with the name of the file you want to run.

You will enter debug mode:

```
next -> main: move $fp, $sp
> 
```

the instruction pointed to by `next` is the next instruction to be executed. To execute the
instruction, simply press _Enter_.

To print a register value, run:

```
> p <reg_name>
```

Replace `<reg_name>` with the name of the register you want to print.

To examine memory contents, run:

```
> x/n <addr>
```

Replace `<addr>` with the address of the memory you want to examine,
specified in one of the addressing modes. `n` is the number of
contiguous words you want to examine starting from `<addr>`.

For example, running

```
> x/-5 0($sp)
```

will produce the output:

```
  0x7FFFFFF0: 0x00000000
  0x7FFFFFF4: 0x00000000
  0x7FFFFFF8: 0x00000000
  0x7FFFFFFC: 0x00000000
  0x80000000: 0x00000000 <-- $sp
```

Notice that you can examine memory backwards by having `n` as a negative number.

If you don't specify `n`, it will default to 1:

```
> x 0($sp)

  0x80000000: 0x00000000 <-- $sp

next -> main: move $fp, $sp
>
```

You can also fast forward to a particular place in code. To do this, run:

```
> g <label_name>
```

The code will execute until it reaches an instruction with label `<label_name>`.

To exit the program while in debug mode, enter `exit`.

**Caution:** The debugger is interactive and so reads command inputs from `stdin`. This causes
problems when you run a MIPS file with an input file using input redirection using `<`.

So, if you want to turn debugging on _and_ run feed an input file, don't use input redirection. Instead, just
run

```
java -cp build main.java.mips.MIPSInterpreter --debug --in in file.s
```

where `file.s` and `in` are the names of the MIPS assembly file and input file, respectively.

## Try It
The repo comes with example MIPS files you can run: `tests/hello.s`, `tests/sbrk.s`, and `tests/quicksort/quicksort.s`. 






# Using `sbrk` syscall to heap-allocate and initialize array to make [5, 4, 3, 2, 1]
.text
main:
    li $v0, 9
    li $a0, 20  # number of bytes to be allocated on heap
    syscall
    move $t0, $v0
    move $t2, $v0

    li $t1, 5
array_init:
    sw $t1, 0($t0)
    addi $t0, $t0, 4       # Move on to next int of array
    addi $t1, $t1, -1
    bne $t1, $zero, array_init

    # Print ints in array
    li $t1, 5
    li $v0, 1
array_iter:
    lw $a0, 0($t2)
    syscall
    addi $t2, $t2, 4
    addi $t1, $t1, -1
    bne $t1, $zero, array_iter

    # Exit program
    li $v0, 10
    syscall
 
.text

main:
    addi $sp, $sp, -4
    sw   $fp, 0($sp)
    move $fp, $sp
    addi $sp, $sp, -16
    addi $sp, $sp, -4
    sw   $ra, 0($sp)
    li $v0, 9
    li $a0, 100
    syscall
    sw $v0, -4($fp)
    li $t1, 0
    sw $t1, -8($fp)
    li $v0, 5
    syscall
    move $t0, $v0
    sw $t0, -16($fp)
    lw $t0, -16($fp)
    li $t1, 100
    bgt $t0, $t1, main_return
    lw $t1, -16($fp)
    li $t2, 1
    sub $t0, $t1, $t2
    sw $t0, -16($fp)
    li $t1, 0
    sw $t1, -12($fp)
main_loop0:
    lw $t0, -12($fp)
    lw $t1, -16($fp)
    bgt $t0, $t1, main_exit0
    li $v0, 5
    syscall
    move $t0, $v0
    sw $t0, -8($fp)
    lw $t2, -12($fp)
    lw $t0, -8($fp)
    lw $t1, -4($fp)
    sll $t2, $t2, 2
    add $t2, $t2, $t1
    sw $t0, 0($t1)
    lw $t1, -12($fp)
    li $t2, 1
    add $t0, $t1, $t2
    sw $t0, -12($fp)
    j main_loop0
main_exit0:
    lw $a0, -4($fp)
    lw $a1, -0($fp)
    lw $a2, -16($fp)
    lw $a3, -4($fp)
    lw $t0, -0($fp)
    sw $t0, 0($sp)
    lw $t0, -16($fp)
    sw $t0, 4($sp)
    addi $sp, $sp, -8
    jal quicksort
    addi $sp, $sp, 8
    lw $ra, 32($sp)
    li $t1, 0
    sw $t1, -12($fp)
main_loop1:
    lw $t0, -12($fp)
    lw $t1, -16($fp)
    bgt $t0, $t1, main_exit1
    lw $t2, -12($fp)
    sll $t2, $t2, 2
    lw $t1, -4($fp)
    add $t2, $t2, $t1
    lw $t1, 0($t2)
    sw $t1, -8($fp)
    lw $a0, -8($fp)
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    lw $t1, -12($fp)
    li $t2, 1
    add $t0, $t1, $t2
    sw $t0, -12($fp)
    j main_loop1
main_exit1:
main_return:
  li $v0, 10
  syscall


quicksort:
    addi $sp, $sp, -4
    sw   $fp, 0($sp)
    move $fp, $sp
    sw $a0, -4($fp)
    sw $a1, -8($fp)
    sw $a2, -12($fp)
    addi $sp, $sp, -44
    addi $sp, $sp, -4
    sw   $ra, 0($sp)
    li $v0, 9
    li $a0, 100
    syscall
    sw $v0, -4($fp)
    li $t1, 0
    sw $t1, -40($fp)
    li $t1, 0
    sw $t1, -44($fp)
    lw $t0, -8($fp)
    lw $t1, -12($fp)
    bge $t0, $t1, quicksort_end
    lw $t1, -8($fp)
    lw $t2, -12($fp)
    add $t0, $t1, $t2
    sw $t0, -32($fp)
    lw $t1, -32($fp)
    li $t2, 2
    div $t0, $t1, $t2
    sw $t0, -32($fp)
    lw $t2, -32($fp)
    sll $t2, $t2, 2
    lw $t1, -4($fp)
    add $t2, $t2, $t1
    lw $t1, 0($t2)
    sw $t1, -36($fp)
    lw $t1, -8($fp)
    li $t2, 1
    sub $t0, $t1, $t2
    sw $t0, -40($fp)
    lw $t1, -12($fp)
    li $t2, 1
    add $t0, $t1, $t2
    sw $t0, -44($fp)
quicksort_loop0:
quicksort_loop1:
    lw $t1, -40($fp)
    li $t2, 1
    add $t0, $t1, $t2
    sw $t0, -40($fp)
    lw $t2, -40($fp)
    sll $t2, $t2, 2
    lw $t1, -4($fp)
    add $t2, $t2, $t1
    lw $t1, 0($t2)
    sw $t1, -28($fp)
    lw $t1, -28($fp)
    sw $t1, -16($fp)
    lw $t0, -16($fp)
    lw $t1, -36($fp)
    blt $t0, $t1, quicksort_loop1
quicksort_loop2:
    lw $t1, -44($fp)
    li $t2, 1
    sub $t0, $t1, $t2
    sw $t0, -44($fp)
    lw $t2, -44($fp)
    sll $t2, $t2, 2
    lw $t1, -4($fp)
    add $t2, $t2, $t1
    lw $t1, 0($t2)
    sw $t1, -28($fp)
    lw $t1, -28($fp)
    sw $t1, -20($fp)
    lw $t0, -20($fp)
    lw $t1, -36($fp)
    bgt $t0, $t1, quicksort_loop2
    lw $t0, -40($fp)
    lw $t1, -44($fp)
    bge $t0, $t1, quicksort_exit0
    lw $t2, -44($fp)
    lw $t0, -16($fp)
    lw $t1, -4($fp)
    sll $t2, $t2, 2
    add $t2, $t2, $t1
    sw $t0, 0($t1)
    lw $t2, -40($fp)
    lw $t0, -20($fp)
    lw $t1, -4($fp)
    sll $t2, $t2, 2
    add $t2, $t2, $t1
    sw $t0, 0($t1)
    j quicksort_loop0
quicksort_exit0:
    lw $t1, -44($fp)
    li $t2, 1
    add $t0, $t1, $t2
    sw $t0, -24($fp)
    lw $a0, -4($fp)
    lw $a1, -8($fp)
    lw $a2, -44($fp)
    lw $a3, -4($fp)
    lw $t0, -8($fp)
    sw $t0, 0($sp)
    lw $t0, -44($fp)
    sw $t0, 4($sp)
    addi $sp, $sp, -8
    jal quicksort
    addi $sp, $sp, 8
    lw $ra, 32($sp)
    lw $t1, -44($fp)
    li $t2, 1
    add $t0, $t1, $t2
    sw $t0, -44($fp)
    lw $a0, -4($fp)
    lw $a1, -44($fp)
    lw $a2, -12($fp)
    lw $a3, -4($fp)
    lw $t0, -44($fp)
    sw $t0, 0($sp)
    lw $t0, -12($fp)
    sw $t0, 4($sp)
    addi $sp, $sp, -8
    jal quicksort
    addi $sp, $sp, 8
    lw $ra, 32($sp)
quicksort_end:
    lw   $ra, 0($sp)
    addi $sp, $sp, 4
    addi $sp, $sp, 44
    lw   $fp, 0($sp)
    addi $sp, $sp, 4

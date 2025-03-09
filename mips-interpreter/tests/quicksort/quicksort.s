.text
main:
  move $fp, $sp
  addi $sp, $sp, -452
  li $v0, 5
  syscall
  move $t1, $v0
  li $t0, 100
  bgt $t1, $t0, return_main
  li $t0, 1
  sub $t1, $t1, $t0
  li $t4, 0
loop0_main:
  bgt $t4, $t1, exit0_main
  li $v0, 5
  syscall
  move $t2, $v0
  sll $t3, $t4, 2
  addi $t0, $fp, 0
  sub $t0, $t0, $t3
  sw $t2, ($t0)
  addi $t4, $t4, 1
  j loop0_main
exit0_main:
  move $a0Quicksort, $fp
  addi $a0Quicksort, $a0Quicksort, 0
  li $a1Quicksort, 0
  move $a2Quicksort, $t1
  addi $sp, $sp, -4
  sw $t1, 0($sp)
  addi $sp, $sp, -8
  sw $fp, 0($sp)
  sw $ra, 4($sp)
  jal quicksort
  lw $fp, 0($sp)
  lw $ra, 4($sp)
  lw $t1, 8($sp)
  addi $sp, $sp, 12
  li $t4, 0
loop1_main:
  bgt $t4, $t1, exit1_main
  sll $t0, $t4, 2
  addi $t2, $fp, 0
  sub $t0, $t2, $t0
  lw $t2, ($t0)
  move $a0, $t2
  li $v0, 1
  syscall
  li $a0, 10
  li $v0, 11
  syscall
  addi $t4, $t4, 1
  j loop1_main
exit1_main:
return_main:
  li $v0, 10
  syscall

quicksort:
  move $fp, $sp
  addi $sp, $sp, -104
  bge $a1Quicksort, $a2Quicksort, end_quicksort
  add $t1, $a1Quicksort, $a2Quicksort 
  li $t0, 2
  div $t1, $t1, $t0
  sll $t0, $t1, 2
  sub $t0, $a0Quicksort, $t0
  lw $t4, ($t0)
  li $t0, 1
  sub $t5, $a1Quicksort, $t0
  addi $t0, $a2Quicksort, 1
loop0_quicksort:
loop1_quicksort:
  addi $t5, $t5, 1
  sll $t1, $t5, 2
  sub $t1, $a0Quicksort, $t1
  lw $t1, ($t1)
  move $t3, $t1
  blt $t3, $t4, loop1_quicksort
loop2_quicksort:
  li $t1, 1
  sub $t0, $t0, $t1
  sll $t1, $t0, 2
  sub $t1, $a0Quicksort, $t1
  lw $t1, ($t1)
  move $t1, $t1
  bgt $t1, $t4, loop2_quicksort
  bge $t5, $t0, exit0_quicksort
  sll $t2, $t0, 2
  sub $t2, $a0Quicksort, $t2
  sw $t3, ($t2)
  sll $t2, $t5, 2
  sub $t2, $a0Quicksort, $t2
  sw $t1, ($t2)
  j loop0_quicksort
exit0_quicksort:
  addi $sp, $sp, -12
  sw $a0Quicksort, 0($sp)
  sw $a1Quicksort, 4($sp)
  sw $a2Quicksort, 8($sp)
  move $a0Quicksort, $a0Quicksort  
  move $a1Quicksort, $a1Quicksort
  move $a2Quicksort, $t0
  addi $sp, $sp, -4
  sw $t0, 0($sp)
  addi $sp, $sp, -8
  sw $fp, 0($sp)
  sw $ra, 4($sp)
  jal quicksort
  lw $fp, 0($sp)
  lw $ra, 4($sp)
  lw $t0, 8($sp)
  lw $a0Quicksort, 12($sp)
  lw $a1Quicksort, 16($sp)
  lw $a2Quicksort, 20($sp)
  addi $sp, $sp, 24
  addi $t0, $t0, 1
  addi $sp, $sp, -12
  sw $a0Quicksort, 0($sp)
  sw $a1Quicksort, 4($sp)
  sw $a2Quicksort, 8($sp)
  move $a0Quicksort, $a0Quicksort
  move $a1Quicksort, $t0
  move $a2Quicksort, $a2Quicksort
  addi $sp, $sp, -8
  sw $fp, 0($sp)
  sw $ra, 4($sp)
  jal quicksort
  lw $fp, 0($sp)
  lw $ra, 4($sp)
  lw $a0Quicksort, 8($sp)
  lw $a1Quicksort, 12($sp)
  lw $a2Quicksort, 16($sp)
  addi $sp, $sp, 20
end_quicksort:
  addi $sp, $sp, 104
  jr $ra

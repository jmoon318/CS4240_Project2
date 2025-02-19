.data
greeting:
.word 104, 51, 108, 108, 111, 44, 32, 119, 48, 114, 49, 100 

.text
main:
 move    $fp, $sp
 la      $t0, greeting
 li      $t1, 0
loop:
 li      $t2, 48
 bge     $t1, $t2, done 
 add     $t2, $t0, $t1
 lw      $a0, 0($t2)
 li      $v0, 11
 syscall
 addi    $t1, $t1, 4
 j loop
done:
 li      $v0, 11           # print space
 li      $a0, 10
 syscall                 
 li      $v0, 10           # exit
 syscall
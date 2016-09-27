.text
.align 2
.globl main


main:
	
	li $s6, 0 #current tally of total number of inputted players

	jal inputloop

	beqz $s6, end

	move $s7, $s6
	addi $s7, $s7, -1
	move $s5, $s7
	li $t0, 8
	mul $s5, $s5, $t0
	mul $s7, $s7, $s7 #bubble sort takes n^2 long in worst case, let it run

	jal bubble

	jal printPlayers

	li $v0, 10
	syscall



#sort method
bubble:
	beqz $s7, returnadd #check if any more sorts left to do
	move $s0, $ra

	li $t0, 0
	move $t8, $sp
	la $t9, 8($sp)
	jal sortloop

	move $ra, $s0

	addi $s7, $s7, -1
	j bubble


sortloop: 
	beq $t0, $s5, returnadd

	lw $t2, 0($t8)
	lw $t3, 4($t8)
	lw $t4, 0($t9)
	lw $t5, 4($t9)

	lwc1 $f3, 0($t2)
	lwc1 $f4, 0($t4)

	c.le.s $f3, $f4
	bc1f swap

	addi $t0, $t0, 8
	move $t8, $t9
	la $t9, 8($t9)
	j sortloop

swap:

	sw $t2, 0($t9)
	sw $t3, 4($t9)
	sw $t4, 0($t8)
	sw $t5, 4($t8)

	addi $t0, $t0, 8
	j sortloop
#sort method end


#inputing all of the players -- works correctly
inputloop:

	move $s7, $ra #save the $ra from the main function

	li $v0, 4
	la $a0, name
	syscall

	li $v0, 9
	li $a0, 60
	syscall

	move $a0, $v0
	li $a1, 60
	li $v0, 8
	syscall

	move $a1, $a0
	la $a2, done
	jal compareString #check if the name equals 'DONE'
	move $ra, $s7
	beqz $t0, returnadd


	addi $sp, $sp, -4 #saves address to heap space in stack
	sw $a1, 0($sp)

	li $v0, 4  		#start of the points input
	la $a0, points
	syscall

	li $v0, 6
	syscall

	mov.s $f2, $f0

	li $v0, 4  		#start of the minutes input
	la $a0, minutes
	syscall

	li $v0, 6
	syscall

	mov.s $f3, $f0

	jal checkzero #checks to make sure inpputs aren't 0
	move $ra, $s7

	li $v0, 9 # allocate memory for floating point
	li $a0, 4
	syscall

	s.s $f4, 0($v0)  #saves floating point at address in $v0
	move $a0, $v0

	addi $sp, $sp, -4	#allocate stack space to save address
	sw $a0, 0($sp)		#saves floating point address in stack space

	addi $s6, $s6, 1 #increment one to the total number of players
	j inputloop
#done inputting all of the players

checkzero:
	#checks to see if any of the inputs are 0
	mtc1 $zero, $f1
	c.eq.s $f1, $f2
	bc1t zeroinput
	c.eq.s $f1, $f3
	bc1t zeroinput
	div.s $f4, $f2, $f3
	j returnadd
	#checks to see if any of the inputs are 0
zeroinput:
	mtc1 $zero, $f4
	j returnadd




#string compare to DONE
compareString:
	la $t0, ($a1)
	la $t1, ($a2)
	lb $t2, 0($t0)
	lb $t3, 0($t1)
	beqz $t2, endstring
	bne $t2, $t3, unequal
	j loop
loop:
	addi $t0, $t0, 1
	addi $t1, $t1, 1
	lb $t2, ($t0)
	lb $t3, ($t1)
	beqz $t2, endstring
	beq $t2, $t3, loop
	bne $t2, $t3, unequal	
unequal:
	li $t0, 1
	j returnadd
endstring:
	li $t0, 0
	j returnadd
#end string compare to DONE

returnadd:
	jr $ra

#cylcle through and print all players
printPlayers:

	beqz $s6, returnadd
	move $s7, $ra
	
	lw $t0, 0($sp)
	lw $t1, 4($sp)

	addi $sp, $sp, 8

	li $v0, 4
	move $a0, $t1
	jal trim
	move $ra, $s7
	syscall

	la $a0, space
	syscall

	li $v0, 2
	lwc1 $f12, 0($t0)
	syscall

	li $v0, 4
	la $a0, newline
	syscall

	addi $s6, $s6, -1

	j printPlayers

end: 
	li $v0, 10
	syscall


#trimming new line character off
trim:
	la $t8, newline
	lb $t9, ($t8)
	move $t2, $a0
	lb $t1, ($t2)
	j cutloop
cutloop:
	beq $t1, $t9, cut
	addi $t2, $t2, 1
	lb $t1, ($t2)
	j cutloop
cut:
	sb $zero, ($t2)
	j returnadd





.data
done: .asciiz "DONE\n"
newline: .asciiz "\n"
name: .asciiz "Enter the player's last name:"
points: .asciiz "Enter the player's points per game:"
minutes: .asciiz "Enter the player's minutes per game:"
space: .asciiz " "
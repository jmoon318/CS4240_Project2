#!/bin/bash
java -cp ./build MakeASM $1 > $2
# Write a script to run your optimizer in this file 
# This script should take one command line argument: an path to 
# an input ir file as 
# This script should output an optimized ir file named "out.ir"

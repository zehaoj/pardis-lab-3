#!/bin/bash
# Change --mincpus to desired number of physical cpu cores to be used (doubled in JVM)
salloc -t 0:5:0 -N 1 -A edu22.dd2443 --mincpus=4 -p shared srun -n 1 ./runJava.sh

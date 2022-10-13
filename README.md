# Lab 3, lock-free concurrent skiplist

#### Contributors: Zehao Jiang, Yilin Chang.

## Description for the files

Most of the additional files are named based on the tasks they solved. For task 1 the corresponding file is Task1, and for task 2 Task2, so on.

The LockedLFSkipList class is the same as LockFreeSkipList except for adding global locks in its linearization points.
The Log class stores the structure of output logs.

In addition, Task3_1_2 corresponds to task 3.1 and 3.2. To run task 3.1 or 3.2, you need to set the parameter when creating LockedLFSkipList as _true_ or _false_ (in **line 172**) to choose using locks or not.

## How to run the code.

First modify _runJava.sh_ and write the class you want to run. Then simply run _allocateJob.sh_.

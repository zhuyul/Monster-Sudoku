# MonsterSudoku
Implemented a Sudoku solver to solve Monster Sudoku as a Constraint Satisfaction Problem in JAVA 7. The program is called with a command line that contains an input problem filename, an output log filename, a time out parameter in seconds, and zero or more of the tokens FC, MRV, DH, LCV, in any order, separated by space.

The program is able to read a sequence of tokens and properly activate corresponding components. The MRV (Minimum Remaining Value) token uses the MRV heuristic to select the next variable to explore next, the DH (Degree Heuristic) token uses the DH heuristic to select the next variable to explore next, the FC (Forward Checking) token runs forward checking after each assignment, and the LCV (Least Constraining Value) token uses the LCV heuristic to order the values.

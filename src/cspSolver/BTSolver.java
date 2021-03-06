package cspSolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sudoku.Converter;
import sudoku.SudokuFile;
/**
 * Backtracking solver. 
 *
 */
public class BTSolver implements Runnable{

	//===============================================================================
	// Properties
	//===============================================================================

	private ConstraintNetwork network;
	private static Trail trail = Trail.getTrail();
	private boolean hasSolution = false;
	private SudokuFile sudokuGrid;

	private int numAssignments;
	private int numBacktracks;
	private long startTime;
	private long endTime;
	
	public enum VariableSelectionHeuristic 	{ None, MinimumRemainingValue, Degree, MRV_DH };
	public enum ValueSelectionHeuristic 		{ None, LeastConstrainingValue };
	public enum ConsistencyCheck				{ None, ForwardChecking, ArcConsistency };
	
	private VariableSelectionHeuristic varHeuristics;
	private ValueSelectionHeuristic valHeuristics;
	private ConsistencyCheck cChecks;
	//===============================================================================
	// Constructors
	//===============================================================================

	public BTSolver(SudokuFile sf)
	{
		this.network = Converter.SudokuFileToConstraintNetwork(sf);
		this.sudokuGrid = sf;
		numAssignments = 0;
		numBacktracks = 0;
	}

	//===============================================================================
	// Modifiers
	//===============================================================================
	
	public void setVariableSelectionHeuristic(VariableSelectionHeuristic vsh)
	{
		this.varHeuristics = vsh;
	}
	
	public void setValueSelectionHeuristic(ValueSelectionHeuristic vsh)
	{
		this.valHeuristics = vsh;
	}
	
	public void setConsistencyChecks(ConsistencyCheck cc)
	{
		this.cChecks = cc;
	}
	//===============================================================================
	// Accessors
	//===============================================================================

	/** 
	 * @return true if a solution has been found, false otherwise. 
	 */
	public boolean hasSolution()
	{
		return hasSolution;
	}

	/**
	 * @return solution if a solution has been found, otherwise returns the unsolved puzzle.
	 */
	public SudokuFile getSolution()
	{
		return sudokuGrid;
	}

	public void printSolverStats()
	{
		System.out.println("Time taken:" + (endTime-startTime) + " ms");
		System.out.println("Number of assignments: " + numAssignments);
		System.out.println("Number of backtracks: " + numBacktracks);
	}

	/**
	 * 
	 * @return time required for the solver to attain in seconds
	 */
	public long getTimeTaken()
	{
		return endTime-startTime;
	}
	
	public long getStartTime()
	{
		return startTime;
	}
	
	public long getEndTime()
	{
		return endTime;
	}

	public int getNumAssignments()
	{
		return numAssignments;
	}

	public int getNumBacktracks()
	{
		return numBacktracks;
	}

	public ConstraintNetwork getNetwork()
	{
		return network;
	}

	//===============================================================================
	// Helper Methods
	//===============================================================================

	/**
	 * Checks whether the changes from the last time this method was called are consistent. 
	 * @return true if consistent, false otherwise
	 */
	private boolean checkConsistency()
	{
		boolean isConsistent = false;
		switch(cChecks)
		{
		case None: 				isConsistent = assignmentsCheck();
		break;
		case ForwardChecking: 	isConsistent = forwardChecking();
		break;
		case ArcConsistency: 	isConsistent = arcConsistency();
		break;
		default: 				isConsistent = assignmentsCheck();
		break;
		}
		return isConsistent;
	}
	
	/**
	 * default consistency check. Ensures no two variables are assigned to the same value.
	 * @return true if consistent, false otherwise. 
	 */
	private boolean assignmentsCheck()
	{
		for(Variable v : network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther : network.getNeighborsOfVariable(v))
				{
					if (v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement forward checking. 
	 */
	private boolean forwardChecking()
	{
		for(Variable v : network.getVariables()){
			if(v.isAssigned()){
				for(Variable vOther : network.getNeighborsOfVariable(v)){
					if (v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
					vOther.removeValueFromDomain(v.getAssignment());
					if(vOther.getDomain().isEmpty()){
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement Maintaining Arc Consistency.
	 */
	private boolean arcConsistency()
	{
		return false;
	}
	
	/**
	 * Selects the next variable to check.
	 * @return next variable to check. null if there are no more variables to check. 
	 */
	private Variable selectNextVariable()
	{
		Variable next = null;
		switch(varHeuristics)
		{
		case None: 					next = getfirstUnassignedVariable();
		break;
		case MinimumRemainingValue: next = getMRV();
		break;
		case Degree:				next = getDegree();
		break;
		case MRV_DH:				next = getMRV_DH();
		break;
		default:					next = getfirstUnassignedVariable();
		break;
		}
		return next;
	}
	
	/**
	 * default next variable selection heuristic. Selects the first unassigned variable. 
	 * @return first unassigned variable. null if no variables are unassigned. 
	 */
	private Variable getfirstUnassignedVariable()
	{
		for(Variable v : network.getVariables())
		{
			if(!v.isAssigned())
			{
				return v;
			}
		}
		return null;
	}

	/**
	 * TODO: Implement MRV heuristic
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned. 
	 */
	private Variable getMRV()
	{
		Variable to_return = null;
		int min = Integer.MAX_VALUE;
		
		for(Variable v : network.getVariables()){
			if(v.isAssigned()){
				for(Variable vOther : network.getNeighborsOfVariable(v)){
					vOther.removeValueFromDomain(v.getAssignment());
				}
			}
		}
		
		for (Variable v : network.getVariables()){
			if (!v.isAssigned()){
				int sizeofdomain = v.getDomain().size();
				if (sizeofdomain< min ){
					min = sizeofdomain;
					to_return = v;
				}
			}
		}
		return to_return;
	}
	
	private Variable getMRV_DH()
	{
		int MRVmin = Integer.MAX_VALUE;
		int DHmax = Integer.MIN_VALUE;
		ArrayList<Variable> to_return = new ArrayList<Variable>();
		
		for(Variable v : network.getVariables()){
			if(v.isAssigned()){
				for(Variable vOther : network.getNeighborsOfVariable(v)){
					vOther.removeValueFromDomain(v.getAssignment());
				}
			}
		}
		
		for (Variable v : network.getVariables()){
			if (!v.isAssigned()){
				int sizeofdomain = v.getDomain().size();
				if (sizeofdomain < MRVmin ){
					MRVmin = sizeofdomain;
					to_return.clear();
					to_return.add(v);
				}
				else if (sizeofdomain == MRVmin){
					to_return.add(v);
				}
			}
		}
		
		if(to_return.isEmpty()){
			return null;
		}
		else if (to_return.size() == 1){
			return to_return.get(0);
		}
		else{
			Variable DH_return = null;
			for (Variable v : to_return){
				int sizeofassignment = 0;
				for(Variable v1 : network.getNeighborsOfVariable(v)){
					if(!v1.isAssigned()){
						sizeofassignment += 1;
					}
				}
				if (sizeofassignment > DHmax ){
					DHmax = sizeofassignment;
					DH_return = v;
				}
			}
			return DH_return;
		}
	}
	
//	private int len(List<Variable> neighborsOfVariable) {
//		// TODO Auto-generated method stub
////		for (Variable v:network.getVariables()){
////			if (v.isAssigned()){
////				return (network.getNeighborsOfVariable(v)).size();
////			}
////		}
//		return 0;
//
//	}
	
	/**
	 * TODO: Implement Degree heuristic
	 * @return variable constrained by the most unassigned variables, null if all variables are assigned.
	 */
	private Variable getDegree()
	{
		Variable to_return = null;
		int max = Integer.MIN_VALUE;
		
		for(Variable v : network.getVariables()){
			if(v.isAssigned()){
				for(Variable vOther : network.getNeighborsOfVariable(v)){
					vOther.removeValueFromDomain(v.getAssignment());
				}
			}
		}
		
		for (Variable v : network.getVariables()){
			if (!v.isAssigned()){
				int sizeofassignment = 0;
				for(Variable v1 : network.getNeighborsOfVariable(v)){
					if(!v1.isAssigned()){
						sizeofassignment += 1;
					}
				}
				if (sizeofassignment > max ){
					max = sizeofassignment;
					to_return = v;
				}
			}
		}
		return to_return;
	}
	
	/**
	 * Value Selection Heuristics. Orders the values in the domain of the variable 
	 * passed as a parameter and returns them as a list.
	 * @return List of values in the domain of a variable in a specified order. 
	 */
	public List<Integer> getNextValues(Variable v)
	{
		List<Integer> orderedValues;
		switch(valHeuristics)
		{
		case None: 						orderedValues = getValuesInOrder(v);
		break;
		case LeastConstrainingValue: 	orderedValues = getValuesLCVOrder(v);
		break;
		default:						orderedValues = getValuesInOrder(v);
		break;
		}
		return orderedValues;
	}
	
	/**
	 * Default value ordering. 
	 * @param v Variable whose values need to be ordered
	 * @return values ordered by lowest to highest. 
	 */
	public List<Integer> getValuesInOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	
	/**
	 * TODO: LCV heuristic
	 */
	public List<Integer> getValuesLCVOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		final Variable var = v;
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				Integer constraint1 = 0;
				Integer constraint2 = 0;
				for (Variable v1 : network.getNeighborsOfVariable(var)){
					if(v1.getDomain().contains(i1)){
						constraint1 += 1;
					}
					if(v1.getDomain().contains(i2)){
						constraint2 += 1;
					}
				}
				return constraint1.compareTo(constraint2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	/**
	 * Called when solver finds a solution
	 */
	private void success()
	{
		hasSolution = true;
		sudokuGrid = Converter.ConstraintNetworkToSudokuFile(network, sudokuGrid.getN(), sudokuGrid.getP(), sudokuGrid.getQ());
	}

	//===============================================================================
	// Solver
	//===============================================================================

	/**
	 * Method to start the solver
	 */
	public void solve()
	{
		startTime = System.currentTimeMillis();
		try {
			solve(0);
		}catch (VariableSelectionException e)
		{
			System.out.println("error with variable selection heuristic.");
		}
		endTime = System.currentTimeMillis();
		Trail.clearTrail();
	}

	/**
	 * Solver
	 * @param level How deep the solver is in its recursion. 
	 * @throws VariableSelectionException 
	 */

	private void solve(int level) throws VariableSelectionException
	{
		if(!Thread.currentThread().isInterrupted())

		{//Check if assignment is completed
			if(hasSolution)
			{
				return;
			}

			//Select unassigned variable
			Variable v = selectNextVariable();		

			//check if the assignment is complete
			if(v == null)
			{
				for(Variable var : network.getVariables())
				{
					if(!var.isAssigned())
					{
						throw new VariableSelectionException("Something happened with the variable selection heuristic");
					}
				}
				success();
				return;
			}

			//loop through the values of the variable being checked LCV

			
			for(Integer i : getNextValues(v))
			{
				trail.placeBreadCrumb();

				//check a value
				v.updateDomain(new Domain(i));
				numAssignments++;
				boolean isConsistent = checkConsistency();
				
				//move to the next assignment
				if(isConsistent)
				{		
					solve(level + 1);
				}

				//if this assignment failed at any stage, backtrack
				if(!hasSolution)
				{
					trail.undo();
					numBacktracks++;
				}
				
				else
				{
					return;
				}
			}	
		}	
	}

	@Override
	public void run() {
		solve();
	}
}

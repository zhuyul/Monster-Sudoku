package cspSolver;

import cspSolver.BTSolver.ConsistencyCheck;
import cspSolver.BTSolver.ValueSelectionHeuristic;
import cspSolver.BTSolver.VariableSelectionHeuristic;
import sudoku.SudokuBoardGenerator;
import sudoku.SudokuBoardReader;
import sudoku.SudokuFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.io.*;

public class SudokuSolver{
	
	private static SudokuFile sf;
	private static BTSolver solver;
	private static int timeLimit = 60000;
	private static long startTime;
	private static long preStart;
	private static long preEnd;
	private static long timeoutTime;
	
	public static void main(String args[]){
		startTime = System.currentTimeMillis();
		if(args.length < 3){
			System.out.println("Error: Number of parameters does not match requirement.");
			return;
		}
		String inputPath = args[0];
		String outputPath = args[1];
		timeLimit = Integer.parseInt(args[2])*1000;
		ArrayList<String> tokens = new ArrayList<String>();
		if(args.length > 3){
			for(int i = 3; i < args.length; i++){
				tokens.add(args[i]);
			}
		}
		if(tokens.size() != 0){
			if(tokens.get(0).equals("GEN")){
				generateBoardFromFile(inputPath);
				outputGenToFile(sf, outputPath);
				return;
			} else{
				sf = SudokuBoardReader.readFile(inputPath);
				solve(tokens);
			}
		}
		else{
			sf = SudokuBoardReader.readFile(inputPath);
			solve(tokens);
		}
		
		if(solver.hasSolution())
		{
			solver.printSolverStats();
			System.out.println(solver.getSolution());
			outputSolToFile(sf, outputPath);
		}
		else
		{
			System.out.println("Failed to find a solution");
			timeoutTime = System.currentTimeMillis();
			outputNoSolToFile(sf, outputPath);
		}
	}
	
	public static void solve(ArrayList<String> tokens){
		solver = new BTSolver(sf);
		
//		solver.setConsistencyChecks(ConsistencyCheck.None);
//		solver.setValueSelectionHeuristic(ValueSelectionHeuristic.None);
//		solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.None);
		
		setToken(solver, tokens);
		
		Thread t1 = new Thread(solver);
		try
		{
			t1.start();
			t1.join(timeLimit);
			if(t1.isAlive())
			{
				t1.interrupt();
			}
		}catch(InterruptedException e)
		{
		}

	}
	
	public static void outputSolToFile(SudokuFile sf, String outputPath){
		ArrayList<Integer> solution = new ArrayList<Integer>();
		for(int row = 0; row < sf.getN(); row++){
			for(int col = 0; col < sf.getN(); col++){
				//The output in BTSolver of Java Shell switches row & column after solve.
				solution.add(solver.getSolution().getBoard()[col][row]);
			}
		}

		try (PrintWriter writer = new PrintWriter(outputPath, "UTF-8")){
			writer.format("TOTAL_START=%s%n", Long.toString(startTime/1000));
			writer.format("PREPROCESSING_START=%s%n", Long.toString(startTime/1000));//No AC developed
			writer.format("PREPROCESSING_DONE=%s%n", Long.toString(startTime/1000));//No AC developed
			writer.format("SEARCH_START=%s%n", Long.toString(solver.getStartTime()/1000));
			writer.format("SEARCH_DONE=%s%n", Long.toString(solver.getEndTime()/1000));
			writer.format("SOLUTION_TIME=%s%n", Long.toString((preEnd-preStart+solver.getEndTime()-solver.getStartTime())/1000));
			writer.println("STATUS=success");
			writer.print("SOLUTION=(" + solution.get(0));
			for(int i = 1; i < solution.size(); i++){
				writer.print("," + solution.get(i));
			}
			writer.println(")");
			writer.format("COUNT_NODES=%d%n", solver.getNumAssignments());
			writer.format("COUNT_DEADENDS=%d%n", solver.getNumBacktracks());
			writer.close();
		} catch(IOException e1){
			System.err.format("IOException: %s%n", e1);
		}
		
	}
	
	public static void outputGenToFile(SudokuFile sf, String outputPath){
		try{
	        File writename = new File(outputPath);  
	        writename.createNewFile();
	        BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
	        out.write("TOTAL_START = " + startTime);
	        out.write("SEARCH_START = ");
	        
	        out.flush(); 
	        out.close(); 
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void outputNoSolToFile(SudokuFile sf, String outputPath){
		try (PrintWriter writer = new PrintWriter(outputPath, "UTF-8")){
			writer.format("TOTAL_START=%s%n", Long.toString(startTime/1000));
			writer.format("PREPROCESSING_START=%s%n", Long.toString(startTime/1000));//No AC developed
			writer.format("PREPROCESSING_DONE=%s%n", Long.toString(startTime/1000));//No AC developed
			writer.format("SEARCH_START=%s%n", Long.toString(solver.getStartTime()/1000));
			writer.format("SEARCH_DONE=%s%n", Long.toString(timeoutTime/1000));
			writer.format("SOLUTION_TIME=%s%n", Long.toString((preEnd-preStart+timeoutTime-solver.getStartTime())/1000));
			writer.println("STATUS=timeout");
			writer.print("SOLUTION=(0");
			for(int i = 1; i < sf.getN()*sf.getN(); i++){
				writer.print(",0");
			}
			writer.println(")");
			writer.format("COUNT_NODES=%d%n", solver.getNumAssignments());
			writer.format("COUNT_DEADENDS=%d%n", solver.getNumBacktracks());
			writer.close();
		} catch(IOException e1){
			System.err.format("IOException: %s%n", e1);
		}
	}
	
	public static void generateBoardFromFile(String filePath){
		try (Reader reader = new FileReader(filePath)) {
			try(BufferedReader br = new BufferedReader(reader)){
				String line;
//				line = br.readLine();
//				System.out.println(line);
				if((line = br.readLine()) != null){
					String[] lineParts = line.split("\\s+");
					System.out.println(line);
					int N = Integer.parseInt(lineParts[1]);
					int P = Integer.parseInt(lineParts[2]);
					int Q = Integer.parseInt(lineParts[3]);
					int M = Integer.parseInt(lineParts[0]);
					sf = SudokuBoardGenerator.generateBoard(N, P, Q, M, timeLimit);
				}
				br.close();
			}
		} catch(IOException e1){
			System.err.println("Invalid file:"+filePath+". Skipping to the next file.");
		} catch (NumberFormatException e2) {
			e2.printStackTrace();
		}
	}
	
	public static void setToken(BTSolver solver, ArrayList<String> tokens){
		solver.setConsistencyChecks(ConsistencyCheck.None);
		solver.setValueSelectionHeuristic(ValueSelectionHeuristic.None);
		solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.None);
		boolean MRV = false;
		boolean DH = false;
		
		for(int i = 0; i < tokens.size(); i++){
			String t = tokens.get(i).toUpperCase();
			if (t.equals("MRV")){
				solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.MinimumRemainingValue);
				MRV = true;
			}
			else if (t.equals("DH")){
				solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.Degree);
				DH = true;
			}
			else if (t.equals("LCV")){
				solver.setValueSelectionHeuristic(ValueSelectionHeuristic.LeastConstrainingValue);
			}
			else if (t.equals("FC")){
				solver.setConsistencyChecks(ConsistencyCheck.ForwardChecking);
			}
			else if (t.equals("ACP")){
				solver.setConsistencyChecks(ConsistencyCheck.ArcConsistency);
			}
			else if (t.equals("MAC")){
				solver.setConsistencyChecks(ConsistencyCheck.None);
			}
			else {
				System.err.println("Error: Invalid input Token: " + t);
			}
		}
		
		if (MRV && DH){
			solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.MRV_DH);
		}
	}
	
}
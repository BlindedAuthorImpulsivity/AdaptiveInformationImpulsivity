package decimalNumber;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import decimalNumber.DecimalNumber.ComputationException;
import decimalNumber.TransformationFunction.TransformationFunctionDecimalNumber;
import helper.Helper;


// TODO: comments. Include: 1) matrix is defined as array of rows. However, also has represenation for columns (precomputed, because faster).
public class DecimalNumberMatrix implements Cloneable, Serializable, Iterable<DecimalNumberArray>{
	private static final long serialVersionUID = Helper.programmeVersion;
	
	private String[] columnNames; 					// The column names are set from the start and have to be specified
	private String[] rowNames;						// The row names are optional, and are set with setRowNames(String[] rowNames)
	
	private DecimalNumberArray[] matrix; 			// the matrix consists of row columns...
	private DecimalNumberArray[] columns;			//... but for ease of computation later on we also store a representation in columns.
	private int ncol;							
	private int nrow;
	private final DecimalFormat df = new DecimalFormat("0." + Helper.repString("0",10));
	
	private boolean immutable = false;

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	CONSTRUCTORS 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/** Instantiate a DecimalNumberMatrix from a 2 dimensional array of doubles ([rows][columns]). The nrow
	 * of the resulting DecimalNumberMatrix is the number of rows in the double matrix (first index). The ncol
	 * of the resulting DecimalNumberMatrix is the length of the first array in the arrays of doubles.
	 * 
	 * The doubleMatrix should have rows (first index) of equal length. If not, an IllegalArgumentException is thrown.
	 * @param matrix
	 */
	public DecimalNumberMatrix (double[][] doubleMatrix) {
		this.nrow = doubleMatrix.length;
		// find the ncol
		this.ncol = doubleMatrix[0].length;
		for (double[] row: doubleMatrix) if (row.length != ncol) 
			throw new IllegalArgumentException("Exception when constructing a DecimalNumberMatrix from a double[][] array: double[][] array contained rows of equal length");

		this.matrix = new DecimalNumberArray[nrow];
		for (int r = 0; r < nrow; r++)
			matrix[r] = new DecimalNumberArray(doubleMatrix[r]);
	}
	
	/** Instantiate a DecimalNumberMatrix from a 2 dimensional array of DecimalNumbers ([rows][columns]). 
	 * The decimalNumberMatrix should have rows (first index) of equal length. If not, an IllegalArgumentException is thrown.
	 * @param matrix
	 */
	public DecimalNumberMatrix (DecimalNumber[][] decimalNumberMatrix) {
		this.nrow = decimalNumberMatrix.length;

		// find the ncol
		int ncol = decimalNumberMatrix[0].length;
		for (DecimalNumber[] row: decimalNumberMatrix) if (row.length != ncol) 
			throw new IllegalArgumentException("Exception when constructing a DecimalNumberMatrix from a DecimalNumber[][] array: DecimalNumber[][] array contained rows of equal length");

		this.matrix = new DecimalNumberArray[nrow];
		for (int r = 0; r < nrow; r++)
			matrix[r] = new DecimalNumberArray(decimalNumberMatrix[r]);
	}
	
	public DecimalNumberMatrix (DecimalNumberArray... rows)
	{
		this.matrix = rows;
		
		this.nrow = rows.length;
		int columns = 0;
		for (DecimalNumberArray row: matrix)
			if (row.length()> columns)
				columns = row.length();
		ncol = columns;
		this.columns = new DecimalNumberArray[ncol];
		setAllColumns();
	}

	/**
	 * Creates a matrix containing nrow by ncol mutable DecimalNumbers with value 0 and without range.
	 * @param columnNames
	 * @param nrow
	 * @param ncol
	 */
	public DecimalNumberMatrix (int nrow, int ncol)
	{
		this.matrix = new DecimalNumberArray[nrow];

		this.ncol = ncol;
		this.nrow = nrow;
		
		for (int r = 0; r < nrow; r++)
			matrix[r] = DecimalNumberArray.rep(new DecimalNumber(0), ncol);
		columns = new DecimalNumberArray[ncol];
		setAllColumns();
	}
	
	/**
	 * Creates a matrix containing nrow by ncol mutable DecimalNumbers with value <value>. 
	 * @param columnNames
	 * @param nrow
	 * @param ncol
	 */
	public DecimalNumberMatrix (int nrow, int ncol, double value)
	{
		this.matrix = new DecimalNumberArray[nrow];

		this.ncol = ncol;
		this.nrow = nrow;
		
		for (int r = 0; r < nrow; r++)
			matrix[r] = DecimalNumberArray.rep(new DecimalNumber(value), ncol);
		columns = new DecimalNumberArray[ncol];
		setAllColumns();
	}
	
	
	/**
	 * Create a new DecimalNumberMatrix with nrow rows and ncol columns from an array of DecimalNumbers. If byrow is set to true,
	 * the DecimalNumbers are added per row. Otherwise, they are added per column.
	 * @param nrow
	 * @param ncol
	 * @param columnNames
	 * @param byrow
	 * @param decimalNumbers
	 */
	public DecimalNumberMatrix (int nrow, int ncol, boolean byrow, DecimalNumberArray array)  {
		if (nrow*ncol != array.length())
			throw new ComputationException("Error in DecimalNumberMatrix constructor (int row, int ncol, DecimalNumberArray): the product of the specified rows and columns does not match the length of the array specified ");
		matrix = new DecimalNumberArray[nrow];
		for (int i = 0 ; i < matrix.length; i ++)
			matrix[i] = new DecimalNumberArray(ncol);
			
		this.nrow = nrow;
		this.ncol = ncol;
		
		int pos = 0;
		if (byrow) 
			while (pos< array.length()) {
				matrix[pos/nrow].set(pos % nrow, array.array[pos]);
				pos++;
			}
		else
			while (pos < array.length()) {
				matrix[pos%nrow].set(pos/ nrow, array.array[pos]);
				pos++;
			}
		columns = new DecimalNumberArray[ncol];
		setAllColumns();
	}
	
	/** wraps the DecimalNumberArray in a matrix that has 1 column */
	public static DecimalNumberMatrix toColumnVector(DecimalNumberArray vector) {
		return new DecimalNumberMatrix(vector.length(), 1, false, vector);
	}
	
	 /** Create a new DecimalNumberMatrix with nrow rows and ncol columns from an array of DecimalNumbers. If byrow is set to true,
	 * the DecimalNumbers are added per row. Otherwise, they are added per column.
	 * @param nrow
	 * @param ncol
	 * @param columnNames
	 * @param byrow
	 * @param decimalNumbers
	 */
	public DecimalNumberMatrix (int nrow, int ncol, boolean byrow, DecimalNumber... decimalNumbers)  {
		if (nrow*ncol != decimalNumbers.length)
			throw new ComputationException("Error in DecimalNumberMatrix constructor (int row, int ncol, DecimalNumber... decimalNumbers): the product of the specified rows and columns does not match the number of decimalNumbers specified ");
		matrix = new DecimalNumberArray[nrow];
		for (int i = 0 ; i < matrix.length; i ++)
			matrix[i] = new DecimalNumberArray(ncol);
			
		this.nrow = nrow;
		this.ncol = ncol;
		
		int pos = 0;
		if (byrow) 
			while (pos< decimalNumbers.length) {
				matrix[pos/nrow].set(pos % nrow, decimalNumbers[pos]);
				pos++;
			}
		else
			while (pos < decimalNumbers.length) {
				matrix[pos%nrow].set(pos/ nrow, decimalNumbers[pos]);
				pos++;
			}
		columns = new DecimalNumberArray[ncol];
		setAllColumns();
	}
	
	/** Create a new DecimalNumberMatrix with nrow rows and ncol columns from an array of DecimalNumbers. If byrow is set to true,
	 * the DecimalNumbers are added per row. Otherwise, they are added per column.
	 * @param nrow
	 * @param ncol
	 * @param columnNames
	 * @param byrow
	 * @param decimalNumbers
	 */
	public DecimalNumberMatrix (int nrow, int ncol, boolean byrow, double... doubleNumbers)  {
		if (nrow*ncol != doubleNumbers.length)
			throw new ComputationException("Error in DecimalNumberMatrix constructor (int row, int ncol, double... doubleNumbers): the product of the specified rows and columns does not match the number of doubles specified ");
		matrix = new DecimalNumberArray[nrow];
		for (int i = 0 ; i < matrix.length; i ++)
			matrix[i] = new DecimalNumberArray(ncol);
		this.nrow = nrow;
		this.ncol = ncol;
		
		int pos = 0;
		if (byrow) 
			while (pos< doubleNumbers.length) {
				matrix[pos/ncol].set(pos % ncol, new DecimalNumber(doubleNumbers[pos]));
				pos++;
			}
		else
			while (pos < doubleNumbers.length) {
				matrix[pos%nrow].set(pos / nrow, new DecimalNumber(doubleNumbers[pos]));
				pos++;
			}
		columns = new DecimalNumberArray[ncol];
		setAllColumns();
	}

	/** Set the DecimalNumberArray[] columns to represent the columns in the matrix. Call this at initialization. */
	private void setAllColumns() {
		for (int c = 0; c < ncol; c++)
			setColumn(c);
	}
	
	private void setColumn(int c) {
		DecimalNumber[] columnArray = new DecimalNumber[nrow];
		for (int r = 0; r < nrow; r++){
				columnArray[r] = matrix[r].get(c);
		}
		columns[c] = new DecimalNumberArray(columnArray);
	}
	
	public static DecimalNumberMatrix randomMatrix (int nrow, int ncol, double min, double max, boolean integerOnly) {
		DecimalNumberArray[] rows = new DecimalNumberArray[nrow];
		for (int i = 0; i< nrow; i ++) {
			DecimalNumberArray newRow = new DecimalNumberArray(ncol);
			for (int j = 0; j < ncol; j++) {
				double value = Math.random()*(max-min) + min;
				if (integerOnly) value = Math.round(value);
				newRow.set(j, new DecimalNumber(value));
			}
				
			rows[i] = newRow;
		}
		return new DecimalNumberMatrix(rows);
		
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Getters and Setters 	/////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	public double[][] toDoubleMatrix(){
		double[][] doubleMatrix = new double[nrow][ncol];
		for (int r = 0; r < nrow; r++)
			doubleMatrix[r] = matrix[r].toDoubleArray();
		return doubleMatrix;
	}
	
	public int[][] toIntegerMatrix(){
		int[][] intMatrix = new int[nrow][ncol];
		for (int r = 0; r < nrow; r++)
			intMatrix[r] = matrix[r].toIntegerArray();
		return intMatrix;
	}
	
	public int nrow() { return nrow;}
	public int ncol() { return ncol;}
	
	/** Returns the dimensions of the matrix: (nrow, ncol) */
	public int[] dim() { return new int[] {nrow, ncol};}
	public boolean isImmutable() { return this.immutable; }
	
	/** Sets the matrix, AND ALL ARRAYS AND DECIMAL NUMBERS WITHIN THIS MATRIX to the desired immutability (i.e., unable to change). 
	 * If set to immutable, subsequent operation that attempts to change this matrix (or the DecimalNumberArrays and DecimalNumbers within
	 * this array) result in a UnsupportedOperationException being thrown.
	 * @param immutable
	 */
	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
		for (DecimalNumberArray dna: matrix)
			dna.setImmutable(immutable);
		
	}

	////////////////////////////	Arrays of rows/columns	/////////////////////////////
	/** Return this matrix as an array of rows. Note that the returned rows are SHALLOW clones of the actual rows - changes in the DecimalNumber values in the clone result in the same changes in the matrix, but adding an additional entry to the column does not add another entry in the matrix.*/ 
	public DecimalNumberArray[] rowMatrix()	{
		DecimalNumberArray[] shallowMatrix = new DecimalNumberArray[nrow];
		for (int r = 0; r < nrow; r++)
			shallowMatrix[r] = matrix[r].shallowClone();
		
		return shallowMatrix;
	}
	
	/** Return this matrix as an array of columns. Note that the returned columns are SHALLOW clones of the actual columns - changes in the DecimalNumber values in the clone result in the same changes in the matrix, but adding an additional entry to the column does not add another entry in the matrix. */
	public DecimalNumberArray[] columnMatrix() {
		DecimalNumberArray[] shallowMatrix = new DecimalNumberArray[ncol];
		for (int c = 0; c < ncol; c++)
			shallowMatrix[c] = columns[c].shallowClone();
		
		return shallowMatrix;
	}
	
	//////////////////////////// 	Names 	/////////////////////////////
	/** Attempts to set the columnNames of the matrix. If the number of names does not match the number of columns, an IllegalArgumentException is thrown. Throws an UnsupportedOperationException if the matrix is immutable*/
	public void setColumnNames (String... newNames) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setColumnNames: trying to set names of an immutable matrix.");
		if (newNames == null) {
			columnNames = null;
			return;
		}

		if (newNames.length != ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.setColumnNames: number of names does not match number of columns.");
		
		this.columnNames = newNames;
	}
	
	/** Attempts to set the columnNames of the matrix. If the number of names does not match the number of columns, an IllegalArgumentException is thrown. Throws an UnsupportedOperationException if the matrix is immutable*/
	public void setColumnNames (ArrayList<String> newNames) {
		setColumnNames(newNames.toArray(new String[newNames.size()]));
	}
	
	/** Attempts to set the column name of the matrix at index. If the column does not exist, an IllegalArgumentException is thrown. Throws a UnsupportedOperationException if the matrix is immutable.*/
	public void setColumnName (int index, String newName) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setColumnName: trying to set names of immutable matrix.");
		if (index < 0 || index > ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.setColumnName: specified column number does not exist.");
		
		if (columnNames == null){
			columnNames = new String[ncol];
			for (int i = 0; i < ncol; i++) columnNames[i] = "";
		}
		
		columnNames[index] = newName;
	}
	
	/** Returns the column names of this matrix. If none are specified, a null is returned.  */
	public String[] getColumnNames() { return columnNames; } 

	/** Attempts to set the row names of the matrix. If the number of names does not match the number of rows, an IllegalArgumentException is thrown. Throws an UnsupportedOperationException if the matrix is immutable*/
	public void setRowNames (String... newNames) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setRowName: trying to set names of immutable matrix.");
		
		if (newNames == null) {
			rowNames = null;
			return;
		}
		
		if (newNames.length != nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.setRowNames: number of names does not match number of rows.");
		
		this.rowNames = newNames;
	}

	/** Attempts to set the row names of the matrix. If the number of names does not match the number of rows, an IllegalArgumentException is thrown. Throws an UnsupportedOperationException if the matrix is immutable*/	
	public void setRowNames (ArrayList<String> newNames) {
		setRowNames(newNames.toArray(new String[newNames.size()]));
	}
	
	/** Attempts to set the row name of the matrix at index. If the row does not exist, an IllegalArgumentException is thrown. Throws a UnsupportedOperationException if the matrix is immutable.*/
	public void setRowName (int index, String newName) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setRowName: trying to set names of immutable matrix.");
		if (index < 0 || index >= nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.setRowName: specified column number does not exist.");
		
		if (rowNames == null){
			rowNames = new String[nrow];
			for (int i = 0; i < nrow; i++) rowNames[i] = "";
		}
		
		rowNames[index] = newName;
	}
	
	/** Returns the row with the specified name. If no names are specified a null. */
	public String[] getRowNames() { return rowNames; }
	
	////////////////////////////	Getting indices		/////////////////////////////
	/** Get the index of the column with the specified name. If no names have been specified a ComputationException is thrown. If the name does not exist, an IllegalArgumentException is thrown. */
	public int getIndexOfColumn(String name) {
		if (columnNames == null) 
			throw new ComputationException("Exception in DecimalNumberMatrix.getIndexOfColumn: attempting to get a column by name, but no names have been specified yet. " );
			
		for (int i = 0; i < columnNames.length; i ++)
			if (name.equals(columnNames[i]))
				return i;
		
		throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getIndexOfColumn: attempting to get a column by name (\"" + name + "\"), but no column with this name exists.");
	}
	
	/** Get the index of the row with the specified name. If no names have been specified, a ComputationException is thrown. If the name does not exist, an IllegalArgumentException is thrown. */
	public int getIndexOfRow(String name) {
		if (rowNames == null) {
			throw new ComputationException("Exception in DecimalNumberMatrix.getIndexOfRow: attempting to get a row by name, but no names have been specified yet. " );
		}
		for (int i = 0; i < rowNames.length; i ++)
			if (name.equals(rowNames[i]))
				return i;
		
		throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getIndexOfRow: attempting to get a row by name (\"" + name + "\"), but no row with this name exists.");
	}
	
	/** Get the row index where the specified column has a value of value. If multiple rows match the specification the
	 * lowest index is returned. If no match is found, returns -1.
	 * @return
	 */
	public int getIndexOfRowWhereColumnIs(String columnName, DecimalNumber value)
	{
		for (int i = 0; i<this.getColumn(columnName).length();i++)
			if (this.getColumn(columnName).get(i).equals(value))
				return i;
		return -1;
	}

	/** Get the row index where the specified column has a value of value. If multiple rows match the specification the
	 * lowest index is returned. If no match is found, returns -1.
	 * @return
	 */
	public int getIndexOfRowWhereColumnIs(String columnName, double value)
	{
		for (int i = 0; i<this.getColumn(columnName).length();i++)
			if (this.getColumn(columnName).get(i).equals(value))
				return i;
		return -1;
	}

	/**
	 * Get the row index where the specified column has a value of value. If multiple rows match the specification the
	 * lowest index is returned. If no match is found, returns -1.
	 * @return
	 */
	public int getIndexOfRowWhereColumnIs(int columnIndex, DecimalNumber value)
	{
		for (int i = 0; i<this.getColumn(columnIndex).length();i++)
			if (this.getColumn(columnIndex).get(i).equals(value))
				return i;
		return -1;
	}

	/** Get the row index where the specified column has a value of value. If multiple rows match the specification the
	 * lowest index is returned. If no match is found, returns -1.
	 * @return
	 */
	public int getIndexOfRowWhereColumnIs(int columnIndex, double value)
	{
		for (int i = 0; i<this.getColumn(columnIndex).length();i++)
			if (this.getColumn(columnIndex).get(i).equals(value))
				return i;
		return -1;
	}

	/**
	 * Get the first row in which the column has the value. Returns null if value does not occurring at all in the column.
	 * @param value
	 * @param column
	 * @return
	 */
	public DecimalNumberArray getRowWhereColumnIs (String columnName, DecimalNumber value)
	{
		DecimalNumberArray relevantColumn = this.getColumn(columnName);
		for (int i = 0; i < nrow; i++)
			if (relevantColumn.get(i).equals(value))
				return matrix[i];
		return null;
	}
	
	/** Returns the index of the DecimalNumberArray (as row) in the matrix. Note that this function only compares pointers, not values. Hence, the argument matches
	 * an row vector only if they are the same object in memory. Returns -1 if no match is found
	 * @return
	 */
	public int getIndexOfDecimalNumberArrayRow(DecimalNumberArray array) {
		for (int n =0; n < nrow; n++)
			if (matrix[n] == array)
				return n;
		return -1;
		
	}
	
	/** Returns the index of the DecimalNumberArray (as column) in the matrix. Note that this function only compares pointers, not values. Hence, the argument matches
	 * an row vector only if they are the same object in memory. Returns -1 if no match is found
	 * @return
	 */
	public int getIndexOfDecimalNumberArrayColumn(DecimalNumberArray array) {
		for (int n =0; n < nrow; n++)
			if (matrix[n] == array)
				return n;
		return -1;
		
	}

	
	////////////////////////////	Rows and columns	/////////////////////////////
	/** Returns a SHALLOW copy of the specified column. If the colnr exceeds the number of columns in the matrix, an IllegalArgumentException is returned. */
	public DecimalNumberArray getColumn (int colnr)
	{
		if (colnr < 0 || colnr >= ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getColumn: trying to get a column index higher than the number of columns in the matrix. ");
		return columns[colnr].shallowClone();
	}

	/** Returns a SHALLOW copy of the specified column. If the name does not exist in the matrix, an IllegalArgumentException is returned. */
	public DecimalNumberArray getColumn (String name)
	{
		if (columnNames == null)
			throw new ComputationException("Exception in DecimalNumberMatrix.getColumn: attempting to get a DecimalNumberArray at column " + name + ". However, column names have not been specified for this matrix.");
	
		int index = getIndexOfColumn(name);
		if (index == -1)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getColumn: Attempting to get a DecimalNumberArray at column " + name + ". However, no column has this name. ");

		return getColumn (index);
	}

	/** Set the column at the specified index position. If the colnr exceeds the number of columns in the matrix, an IllegalArgumentException is thrown. A UnsupportedOperationException is thrown if the matrix is set to immutable (default is mutable) */
	public void setColumn (int colnr, DecimalNumberArray array)
	{
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setColumn: trying to set immutable matrix.");
		
		if (array.length() != nrow) throw new IllegalArgumentException ("Exception in DecimalNumberMatrix.setColumn: new column has incorrect length.");
		
		if (colnr > ncol) throw new IllegalArgumentException ("Exception in DecimalNumberMatrix.setColumn: colnr out of bounds.");
		
		for (int i = 0; i < array.length(); i++)
			matrix[i].get(colnr).set(array.get(i));
	}

	/**Set the column with the specified name. If no column names have been specified a ComputationException is thrown. Throws an UnsupportedOperationException if the matrix is immutable. Throws an IllegalArgumentException name does not exist in the matrix.*/
	public void setColumn (String name, DecimalNumberArray array)
	{	
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setColumn: trying to set immutable matrix.");
		if (columnNames == null)  throw new ComputationException("Exception in DecimalNumberMatrix.setColumn: trying to set column with name \"" + name + "\", but no names have been specified.");
		
		int index = getIndexOfColumn(name);
		if (index == -1) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.setColumn: attempting to set a DecimalNumberArray at column " + name + ". However, no column has this name.");
		
		setColumn(index, array);
	}

	/** Returns a SHALLOW copy of the specified row. If the rownr exceeds the number of row in the matrix, an IllegalArgumentException is returned. */
	public DecimalNumberArray getRow (int rownr)
	{
		if (rownr < 0 || rownr >= nrow) throw new IllegalArgumentException("Exception in DecimalNumber.getRow: rownr out of bounds.");
		return matrix[rownr].shallowClone();
	}

	/** Get the named row. If rows do not have names, a ComputationException is thrown. If the name does not match any of the row names an IllegalArgumentException is thrown. */
	public DecimalNumberArray getRow (String rowName) {
		if (rowNames == null)
			throw new ComputationException("Exception in DecimalNumberMatrix.getRow: attempting to get a DecimalNumberArray with name " + rowName + ". However, row names have not been specified for this matrix.");
	
		int index = getIndexOfRow(rowName);
		if (index == -1)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getRow: Attempting to get a DecimalNumberArray with name " + rowName + ". However, no row has this name.");
			
		
		return getRow(index);
	}
	
	/** Set the row at the rownr position. If the rownr is <0 or larger than then number of rows, or the new row has an incorrect length, an IllegalArgumentException is thrown. Throws a UnsupportedOperationException if the matrix is immutable */
	public void setRow (int rownr, DecimalNumberArray array)
	{
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setRow: trying to set an immutable matrix.");
		
		if (rownr < 0 || rownr >= nrow) 
			throw new IllegalArgumentException ("Exception in DecimalNumberMatrix.setRow: rownr out of bounds.");
		
			
		if (array.length() != ncol) 
			throw new IllegalArgumentException ("Exception in DecimalNumberMatrix.setRow: new row has incorrect length.");
		
		for (int i = 0; i < ncol; i ++)
			matrix[rownr].get(i).set(array.get(i));
	}

	/** Set the row at the rownr position. If the new row has an incorrect length, or the name does not exist in the matrix, an IllegalArgumentException is thrown. Throws a ComputationException if row names have not been specified yet. Throws an UnsupportedOperationException if the matrix is immutable. */
	public void setRow (String name, DecimalNumberArray array)
	{	
		if (rowNames == null) throw new ComputationException("Exception in DecimalNumberMatrix.setRow: trying to set a row by name, but no names have been specified");
		
		int index = getIndexOfRow(name);
		if (index == -1) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.setRow: trying to set a row with name \"" + name + "\", but that name does not exist in the matrix.");
		
		setRow(index, array);
	}

	/**
	 * Get a SHALLOW clone of the first row in which the column has the value. Returns null if value does not occurring at all in the column.
	 * @param value
	 * @param column
	 * @return
	 */
	public DecimalNumberArray getRowWhereColumnIs (int columnIndex, DecimalNumber value)
	{
		DecimalNumberArray relevantColumn = this.getColumn(columnIndex);
		for (int i = 0; i < nrow; i++)
			if (relevantColumn.get(i).equals(value))
				return matrix[i].shallowClone();
		return null;
	}
	
	/** Get a shallow clone of the row vector where the specified column has the specified value. Use only if the column is sorted ascending. Returns null if the value is not in the column vector.*/
	public DecimalNumberArray getRowWhereColumnIs(int columnIndex, double value, boolean sorted) {
		if (columnIndex < 0 || columnIndex > ncol())
			return null;
		
		DecimalNumberArray column = getColumn(columnIndex);
		for (int r = 0; r < nrow; r++) {
			DecimalNumber entry = column.get(r);
			if (entry.equals(value))
				return matrix[r].shallowClone();
			if (entry.compareTo(value) == 1)
				return null;
		}
		return null;
	}
	
	////////////////////////////	DecimalNumbers	/////////////////////////////
	/** Set all the values in the matrix to v. Throws a UnsupportedOperationException if the matrix is immutable. */
	public void setAllValues(DecimalNumber v)
	{
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.setAllValues: trying to set values of immutable matrix.");
		for (int r =0;r<nrow;r++ )
			for (int c=0;c<ncol;c++)
				matrix[r].set(c, new DecimalNumber(v));
	}
	
	/** Returns the DecimalNumber at position (row, col). */
	public DecimalNumber getValueAt (int row, int col)
	{
		return matrix[row].get(col);
	}

	/** Get the DecimalNumber at the named row and named columName. Returns an IllegalArgumentException if either the row name or column name does not exist. Returns a ComputationException if either row or column names have not been specified yet. */
	public DecimalNumber getValueAt (String rowName, String columnName) {
		if (rowNames == null) throw new ComputationException("Exception in DecimalNumberMatrix.getValueAt: trying to get row by name, but no row names have been specified.");
		int rowIndex = getIndexOfRow(rowName);
		if (rowIndex == -1) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getValueAt: trying to set a row with name \"" + rowName + "\", but that name does not exist in the matrix.");
		
		
		if (columnNames == null) throw new ComputationException("Exception in DecimalNumberMatrix.getValueAt: trying to get column by name, but no column names have been specified.");
		int columnIndex = getIndexOfColumn(columnName);
		if (columnIndex == -1) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getValueAt: trying to set a column with name \"" + columnName + "\", but that name does not exist in the matrix.");
		
		return getValueAt(rowIndex, columnIndex);
	}
	
	/** Get the DecimalNumber at the numbered row of the named column. If the matrix has no column names a ComputationException is thrown. If the column name does not exist, an IllegalArgumentException is thrown. If the row index is invalid, an IllegalArgumentException is thrown.  */
	public DecimalNumber getValueAt (int row, String columnName) {
		if (columnNames == null) throw new ComputationException("Exception in DecimalNumberMatrix.getValueAt: trying to get column by name, but no column names have been specified.");
		int columnIndex = getIndexOfColumn(columnName);
		if (columnIndex == -1) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getValueAt: trying to set a column with name \"" + columnName + "\", but that name does not exist in the matrix.");
		
		if (row < 0 || row >= nrow) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getValueAt: attempting to acces a row with index that is out of bounds.");
		
		return getValueAt(row, columnIndex);
	}
	
	/** Set the value at position (row, col). The DecimalNumber object is maintained, but its value is changed. Returns a UnsupportedOperationException is the matrix (or DecimalNumber) is immutable. Throws an IllegalArgumentException is the row or column number is invalid. */
	public void setValueAt(int row, int col, DecimalNumber value)
	{
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.getValueAt: setting an immutable matrix.");
		
		if (row >= 0 && row < nrow)
			if (col >= 0 && col < ncol) {
				matrix[row].get(col).set(value);
				return;
				}
		throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getValueAt: out of bounds for either the row or column index.");
	}
	
	/** Set the value at position (row, col). The DecimalNumber object is maintained, but its value is changed. Returns a UnsupportedOperationException is the matrix (or DecimalNumber) is immutable. Throws an IllegalArgumentException is the row or column number is invalid. */
	public void setValueAt(int row, int col, double value)
	{
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.getValueAt: setting an immutable matrix.");
		
		if (row >= 0 && row < nrow)
			if (col >= 0 && col < ncol) {
				matrix[row].get(col).set(value);
				return;
				}
		throw new IllegalArgumentException("Exception in DecimalNumberMatrix.getValueAt: out of bounds for either the row or column index.");
	}

	/////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	Adding/removing rows/columns 	/////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	/** Appends a new row to the end of the matrix. Since this operation involves copying an array of DecimalNumberArrays, this operations can be inefficient. 
	 * Throws an UnsupportedOperationException if the matrix is immmutable. Throws an IllegalArgumentException if the new row has a length different from the number
	 * of columns in the matrix. If row names are specified, the name of this row is set to "".
	 */
	public void appendRow(DecimalNumberArray newRow) {
		if (immutable) 
			throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.appendRow: trying to append row to an immutable matrix.");
		
		if (newRow.length() != ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.appendRow: new row has an invalid length.");
		
		nrow++;
		matrix = Arrays.copyOf(matrix, nrow);
		matrix[nrow-1] = newRow;

		if (rowNames != null) {
			rowNames = Arrays.copyOf(rowNames, nrow);
			rowNames[nrow-1] = "";
		}
		for (int c = 0; c < ncol; c++)
			columns[c].insert(nrow-1, newRow.get(c));
	}
	
	/** Inserts a new row at the index position. Since this operation involves copying an array of DecimalNumberArrays, it might be an inefficient operation. 
	 * Throws a UnsupportedOperationException if the matrix is immutable. Throws an IllegalArgumentException if the length of newRow does not match the number
	 * of columns in the matrix or if the index is out of bounds.
	 * If row names are specified, the name of this row is set to newRowName.
	 */
	public void insertRow(int index, DecimalNumberArray newRow, String newRowName) {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.appendRow: trying to append row to an immutable matrix.");
		
		if (newRow.length() != ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.appendRow: new row has an invalid length.");

		if (index < 0 || index > nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.appendRow: index out of bounds.");
		
		DecimalNumberArray[] newMatrix = new DecimalNumberArray[nrow+1];
		System.arraycopy(matrix, 0, newMatrix, 0, index);
		System.arraycopy(matrix, index, newMatrix, index + 1, matrix.length - index );
		newMatrix[index] = newRow;
		matrix = newMatrix;

		if (rowNames != null) {
			String[] newRowNames = new String[nrow+1];
			System.arraycopy(rowNames, 0, newRowNames, 0, index);
			System.arraycopy(rowNames, index, newRowNames, index + 1, rowNames.length - index );
			newRowNames[index] = newRowName;
			rowNames = newRowNames;
		}

		nrow++;

		for (int c = 0; c < ncol; c++)
			columns[c].insert(index, newRow.get(c));
	}
	
	/** Inserts a new row at the index position. Since this operation involves copying an array of DecimalNumberArrays, it might be an inefficient operation. 
	 * Throws a UnsupportedOperationException if the matrix is immutable. Throws an IllegalArgumentException if the length of newRow does not match the number
	 * of columns in the matrix.
	 * If row names are specified, the name of this row is set to newRowName.
	 */
	public void insertRow(int index, DecimalNumberArray newRow) {
		insertRow(index, newRow, "");
	}

	/** Removes the row at the index position. Since this operation involves copying an array of DecimalNumberArrays, it might be an inefficient operation. 
	 * Throws a UnsupportedOperationException if the matrix is immutable. Throws an IllegalArgumentException if the index is out of bounds. */
	public void removeRow(int index) {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.removeRow: trying to remove row in an immutable matrix.");
		
		if (index < 0 || index > nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.RemoveRow: index out of bounds");

		DecimalNumberArray[] newMatrix = new DecimalNumberArray[matrix.length-1];		
		System.arraycopy(matrix,0,newMatrix,0,index);
		if (index != matrix.length)
			System.arraycopy(matrix, index+1, newMatrix, index, matrix.length-index-1);
		this.matrix = newMatrix;
		
		if (rowNames != null) {
			String[] newRowNames = new String[rowNames.length-1];
			System.arraycopy(rowNames,0,newRowNames,0,index);
			if (index != matrix.length)
				System.arraycopy(rowNames, index+1, newRowNames, index, rowNames.length-index-1);
			this.rowNames = newRowNames;
		}
		nrow = matrix.length;
		
		for (int c = 0; c < ncol; c++)
			columns[c].remove(index);

	}
	
	/** Inserts a new column at the index position. Since this operation involves copying an array of DecimalNumberArrays, it can be an quite inefficient operation. 
	 * Throws a UnsupportedOperationException if the matrix (or row vectors) is immutable. Throws an IllegalArgumentException if the length of newColumn does not match the number
	 * of rows in the matrix or if the index is out of bounds.
	 * If column names are specified, the name of this column is set to newRowName.
	 */
	public void insertColumn (int index, DecimalNumberArray newColumn, String newColumnName) {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.insertColumn: trying to insert row in an immutable matrix.");
		
		if (newColumn.length() != nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.insertColumn: new columnhas an invalid length.");

		if (index < 0 || index > ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.insertColumn: index out of bounds.");
		
		for (int r = 0; r < matrix.length;r++)
			matrix[r].insert(index, newColumn.get(r));
		
		if (columnNames != null) {
			String[] newColumnNames = new String[ncol+1];
			System.arraycopy(columnNames, 0, newColumnNames, 0, columnNames.length);
			System.arraycopy(columnNames, index, newColumnNames, index + 1, columnNames.length - index );
			newColumnNames[index] = newColumnName;
			columnNames = newColumnNames;
		}
		
		DecimalNumberArray[] newColumns = new DecimalNumberArray[ncol+1];
		System.arraycopy(columns, 0, newColumns, 0, index);
		System.arraycopy(columns, index, newColumns, index + 1, columns.length - index );
		newColumns[index] = newColumn;
		columns = newColumns;
		
		this.ncol++;
	}
	
	/** Inserts a new column at the index position. Since this operation involves copying an array of DecimalNumberArrays, it can be an quite inefficient operation. 
	 * Throws a UnsupportedOperationException if the matrix (or row vectors) is immutable. Throws an IllegalArgumentException if the length of newColumn does not match the number
	 * of rows in the matrix or if the index is out of bounds.
	 * If column names are specified, the name of this column is set to "".
	 */
	public void insertColumn (int index, DecimalNumberArray newColumn) {
		insertColumn(index, newColumn, "");
	}
	
	/** Removes the column at the index position. Since this operation involves copying an array of DecimalNumberArrays, it might be an inefficient operation. 
	 * Throws a UnsupportedOperationException if the matrix is immutable. Throws an IllegalArgumentException if the index is out of bounds. */
	public void removeColumn (int index) {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.removeColumn: trying to remove column in an immutable matrix.");
		
		if (index < 0 || index > nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.removeColumn: index out of bounds");

		
		for (DecimalNumberArray a: matrix)
			a.remove(index);
		
		if (columnNames != null) {
			String[] newColumnNames = new String[columnNames.length-1];
			System.arraycopy(columnNames,0,newColumnNames,0,index);
			if (index != matrix.length)
				System.arraycopy(columnNames, index+1, newColumnNames, index, columnNames.length-index-1);
			this.columnNames = newColumnNames;
		}
		
		DecimalNumberArray[] newColumns = new DecimalNumberArray[columns.length-1];		
		System.arraycopy(columns,0,newColumns,0,index);
		if (index != columns.length)
			System.arraycopy(columns, index+1, newColumns, index, columns.length-index-1);
		this.columns = newColumns;
		
		this.ncol--;
	}
	
	/** Replaces the array of DecimalNumberArrays (i.e., the matrix itself), the column names, and the row names of this matrix with the new matrix. In essence, 
	 * this matrix is removed, and replaced with the new matrix, while saving the reference pointer. Use this function is the whole matrix has to be replaced.
	 * Does nothing if the matrix is set to immutable (default is mutable)
	 * @param newMatrix
	 */
	public void replaceMatrixWith (DecimalNumberMatrix newMatrix) {
		if (immutable)
			return;
		
		this.ncol = newMatrix.ncol;
		this.nrow = newMatrix.nrow;
		this.columnNames = newMatrix.columnNames;
		this.rowNames = newMatrix.rowNames;
		this.matrix = newMatrix.matrix;
		this.setAllColumns();
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////		Matrix multiplication	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////
	/** Multiplies matrix A (with dimensions n*m) with matrix B (with dimensions
	 * m * p). The result is a matrix C with dimensions n*p, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = sum ( A(i,k) * B(k,j) ) forall k in [1, m].
	 * 
	 * if the number of columns in A does not match the number of rows in B (i.e.,
	 * ( dimension(A) = [n,m1], dimension(B) = [m2, p], and m1 != m2), an
	 * illegalArgumentException is thrown.
	 * 
	 * This method multiplies DecimalNumbers using the IJK method. The results from
	 * this method are free of floating point issues.
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static DecimalNumberMatrix matrixMultiplication (DecimalNumberMatrix A, DecimalNumberMatrix B)  {
		if (A.ncol != B.nrow)
			throw new IllegalArgumentException("Matrix multiplication: trying a multiply two matrix of size n * m1 with a matrix of size m2 * p, but m1 is not equal to m2.");
		
		// Create a new DecimalNumberMatrix with size [n, p], where n = rows(A) and p = column(B)
		DecimalNumberMatrix C = new DecimalNumberMatrix(A.nrow, B.ncol, 0);

		for (int i = 0; i < A.nrow; i++) 
			for (int j = 0; j < B.ncol; j++) 
				for (int k = 0; k < A.ncol; k++) 
					C.getRow(i).get(j).add(A.getRow(i).get(k).multiply(B.getRow(k).get(j),false),true);

		return C;
	}
	
	/** Multiplies matrix A (with dimensions n*m) with matrix B (with dimensions
	 * m * p). The result is a matrix C with dimensions n*p, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = sum ( A(i,k) * B(k,j) ) forall k in [1, m].
	 * 
	 * if the number of columns in A does not match the number of rows in B (i.e.,
	 * ( dimension(A) = [n,m1], dimension(B) = [m2, p], and m1 != m2), an
	 * illegalArgumentException is thrown.
	 * 
	 * This method multiplies DecimalNumbers using the IKJ method. The results from
	 * this method are free of floating point issues.
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static DecimalNumberMatrix matrixMultiplicationDecimalNumberIKJ (DecimalNumberMatrix A, DecimalNumberMatrix B)  {
		if (A.ncol != B.nrow)
			throw new IllegalArgumentException("Matrix multiplication: trying a multiply two matrix of size n * m1 with a matrix of size m2 * p, but m1 is not equal to m2.");
		
		// Create a new DecimalNumberMatrix with size [n, p], where n = rows(A) and p = column(B)
		DecimalNumberMatrix C = new DecimalNumberMatrix(A.nrow, B.ncol, 0);
		
		for (int i = 0; i < A.nrow; i++) 
	        for (int k = 0; k < A.ncol; k++) 
	            for (int j = 0; j < B.ncol; j++) 
	            	C.getRow(i).get(j).add(A.getRow(i).get(k).multiply(B.getRow(k).get(j),false),true);

		return C;
	}
	
	/** Multiplies matrix A (with dimensions n*m) with matrix B (with dimensions
	 * m * p). The result is a matrix C with dimensions n*p, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = sum ( A(i,k) * B(k,j) ) forall k in [1, m].
	 * 
	 * if the number of columns in A does not match the number of rows in B (i.e.,
	 * ( dimension(A) = [n,m1], dimension(B) = [m2, p], and m1 != m2), an
	 * illegalArgumentException is thrown.
	 * 
	 * This method first transforms the DecimalNumberMatrix to double[][] and subsequently
	 * multiplies these doubles using the IJK method. The results from this method are 
	 * NOT free of floating point issues - specifically, results cannot be trusted after
	 * the 9th decimal point or so.
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static DecimalNumberMatrix matrixMultiplicationDoubleIKJ (DecimalNumberMatrix A, DecimalNumberMatrix B)  {
		double[][] A2 = A.toDoubleMatrix();
		double[][] B2 = B.toDoubleMatrix();
		double[][] C  = new double[A.nrow][B.ncol];
		for (int i = 0; i < A.nrow; i++) 
	        for (int k = 0; k < A.ncol; k++) 
	            for (int j = 0; j < B.ncol; j++) 
	            	C[i][j] += A2[i][k] * B2[k][j];
		return new DecimalNumberMatrix(C);
	}
	
	/** Multiplies matrix A (with dimensions n*m) with matrix B (with dimensions
	 * m * p). The result is a matrix C with dimensions n*p, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = sum ( A(i,k) * B(k,j) ) forall k in [1, m].
	 * 
	 * if the number of columns in A does not match the number of rows in B (i.e.,
	 * ( dimension(A) = [n,m1], dimension(B) = [m2, p], and m1 != m2), an
	 * illegalArgumentException is thrown.
	 * 
	 * This method first transforms the DecimalNumberMatrix to double[][] and subsequently
	 * multiplies these doubles using the IJK method. The results from this method are 
	 * NOT free of floating point issues - specifically, results cannot be trusted after
	 * the 9th decimal point or so. This method is, however, way faster than the
	 * DecimalNumber implementation of matrix multiplication IF A AND B CONSIST OF
	 * MULTIPLE ROWS AND COLUMNS.
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static DecimalNumberMatrix matrixMultiplicationDoubleIJK (DecimalNumberMatrix A, DecimalNumberMatrix B)  {
		double[][] A2 = A.toDoubleMatrix();
		double[][] B2 = B.toDoubleMatrix();
		double[][] C  = new double[A.nrow][B.ncol];
		
		for (int i = 0; i < A.nrow; i++) 
			for (int j = 0; j < B.ncol; j++) 
				for (int k = 0; k < A.ncol; k++) 
					C[i][j] += A2[i][k] * B2[k][j];

		return new DecimalNumberMatrix(C);
	}
	
	
	/**
	 * Multiplies this matrix (with dimensions n*m) with matrix B (with dimensions
	 * m * p). The result is a matrix C with dimensions n*p, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = sum ( A(i,k) * B(k,j) ) forall k in [1, m].
	 * 
	 * if the number of columns in A does not match the number of rows in B (i.e.,
	 * ( dimension(A) = [n,m1], dimension(B) = [m2, p], and m1 != m2), an
	 * illegalArgumentException is thrown.
	 * 
	 * This call uses the IKJ method with DecimalNumbers (not doubles).
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public DecimalNumberMatrix matrixMultiplication (DecimalNumberMatrix othermatrix)  {
		return DecimalNumberMatrix.matrixMultiplicationDoubleIKJ(this, othermatrix);
	}
	

	/////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////		Other Matrix operations /////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////
	/** Round all DecimalNumbers in this matrix to significant digits, or throws UnsupportedOperationException (if matrix is immutable) */
	public DecimalNumberMatrix round(int significantDigits) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.round: trying to multiply multiplicand to immutable matrix.");
		
		for (DecimalNumberArray a: matrix )
			for (DecimalNumber dn: a)
				dn.round(significantDigits, RoundingMode.HALF_EVEN);
		return this;
	}
	
	/** Creates a new matrix where all entries are the scalar product of the values in matrix A and the multiplicand. 
	 * The original matrix is not changed.*/
	public static DecimalNumberMatrix scalarMultiplication(DecimalNumberMatrix A, DecimalNumber multiplicand) {
		DecimalNumberMatrix M = A.clone();
		for (DecimalNumberArray row: M)
			for (DecimalNumber v: row)
				v.multiply(multiplicand);
		return M;
	}
	
	/** Creates a new matrix where all entries are the scalar product of the values in matrix A and the multiplicand. 
	 * The original matrix is not changed.*/
	public static DecimalNumberMatrix scalarMultiplication(DecimalNumberMatrix A, double multiplicand) {
		DecimalNumberMatrix M = A.clone();
		for (DecimalNumberArray row: M)
			for (DecimalNumber v: row)
				v.multiply(multiplicand);
		return M;
	}
	
	/** Multiplies all DecimalNumbers in this matrix with the multiplicand. Throws UnsupportedOperationException if the matrix is mutable. Returns this
	 * @return */
	public DecimalNumberMatrix scalarMultiplication(DecimalNumber multiplicand) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.scalarMultiplication: trying to multiply multiplicand to immutable matrix.");
		for (DecimalNumberArray row:this)
			for (DecimalNumber v: row)
				v.multiply(multiplicand, true);
		return this;
	}
	
	/** Multiplies all DecimalNumbers in this matrix with the multiplicand. Throws UnsupportedOperationException if the matrix is immutable. Returns this. 
	 * @return */
	public DecimalNumberMatrix scalarMultiplication(double multiplicand) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.scalarMultiplication: trying to multiply multiplicand to immutable matrix.");
		for (DecimalNumberArray row:this)
			for (DecimalNumber v: row)
				v.multiply(multiplicand, true);
		return this;
	}
	
	/**
	 * Creates a new matrix C that is the entrywise (or Hadamard) product of matrices A and B. Specifically,
	 * creates a new matrix C where
	 * 
	 * C(i,j) = A(i,j) * B(i,j), for all rows i and columns j.
	 * 
	 * This does not influence matrices A or B.
	 * 
	 * Throws an IllegalArgumentException if the dimensions of A are unequal to the dimensions of C
	 * @param A
	 * @param B
	 * @return
	 */
	public static DecimalNumberMatrix entrywiseMultiplication(DecimalNumberMatrix  A, DecimalNumberMatrix B) {
		if (A.ncol != B.ncol || A.nrow != B.nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.entrywiseMultiplication: the dimensions of A do not match the dimensions of B" );
		
		DecimalNumberMatrix C = new DecimalNumberMatrix(A.nrow, A.ncol);
		for (int r = 0; r < A.nrow; r++)
			for (int c= 0; c< A.ncol; c++) 
				C.setValueAt(r, c, A.getValueAt(r, c).multiply(B.getValueAt(r, c), false));
			
		return C;
	}
	
	/**
	 * Multiplies all values in this matrix in an entrywise (or Hadamard) fashion with the elements in othermatrix. Formally:
	 * 
	 * this(i,j) = this(i,j) * otherMatrix(i,j), for all rows i and columns j.
	 * 
	 * Throws an IllegalArgumentException if the dimensions of A are unequal to the dimensions of C.
	 * Throws an UnsupportedOperationException if this matrix is immutable.
	 * 
	 * Returns this.
	 * @param A
	 * @param B
	 * @return 
	 * @return
	 */
	public DecimalNumberMatrix entrywiseMultiplication(DecimalNumberMatrix  otherMatrix) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.entrywiseMultiplication: trying to add to immutable matrix.");
		
		if (ncol != otherMatrix.ncol || nrow != otherMatrix.nrow)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.entrywiseMultiplication: the dimensions of this matrix do not match the dimensions of other matrix" );
		
		for (int r = 0; r < nrow; r++)
			for (int c= 0; c< ncol; c++)
				matrix[r].get(c).set( matrix[r].get(c).multiply(otherMatrix.getRow(r).get(c), false));
		
		return this;
	}
	
	/** Returns a new matrix B where all entries are:
	 * B(i,j) = A(i,j) + augend.
	 * 
	 * Note that this operation does not change values in A.
	 * @param A
	 * @param augend
	 */
	public static DecimalNumberMatrix scalarAddition(DecimalNumberMatrix A, DecimalNumber augend) {
		DecimalNumberMatrix B = A.clone();
		for (DecimalNumberArray row: B)
			for (DecimalNumber v: row)
				v.add(augend, true);
		return B;
	}
	
	/** Returns a new matrix B where all entries are:
	 * B(i,j) = A(i,j) + augend.
	 * 
	 * Note that this operation does not change values in A.
	 * @param A
	 * @param augend
	 */
	public static DecimalNumberMatrix scalarAddition(DecimalNumberMatrix A, double augend) {
		DecimalNumberMatrix B = A.clone();
		for (DecimalNumberArray row: B)
			for (DecimalNumber v: row)
				v.add(augend, true);
		return B;
	}
	
	/** Sets all values in this matrix such that: 
	 * this(i,j) = this(i,j) + augend.
	 * 
	 * Note that this operation does not change values in A.
	 * Throws UnsupportedOperationException if the matrix is immutable
	 * 
	 * Returns this.
	 * @param A
	 * @param augend
	 * @return 
	 */
	public DecimalNumberMatrix scalarAddition(DecimalNumber augend)	{
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.scalarAddition: trying to add augend to immutable matrix.");
		for (DecimalNumberArray row:this)
			for (DecimalNumber v:row)
				v.add(augend, true);
		return this;
	}
	
	/** Sets all values in this matrix such that: 
	 * this(i,j) = this(i,j) + augend.
	 * 
	 * Note that this operation does not change values in A.
	 * Throws UnsupportedOperationException if the matrix is immutable
	 * 
	 * Returns this.
	 * @param A
	 * @param augend
	 * @return 
	 */
	public DecimalNumberMatrix scalarAddition(double augend)	{
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.scalarAddition: trying to add augend to immutable matrix.");
		for (DecimalNumberArray row:this)
			for (DecimalNumber v:row)
				v.add(augend, true);
		return this;
	}

	/** Adds matrix A (with dimensions n*m) to matrix B (with dimensions
	 * n*m). The result is a matrix C with dimensions n*m, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = A(i,j) + B(i,j) forall k in [1, m].
	 * 
	 * if the dimensions of A and B do not match an
	 * illegalArgumentException is thrown.
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static DecimalNumberMatrix matrixAddition (DecimalNumberMatrix A, DecimalNumberMatrix B)  {
		if (A.nrow != B.nrow || A.ncol!= B.ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.matrixAddition: trying a add two matrix with unequal dimensions.");

		DecimalNumberMatrix C = A.clone();

		// Fill the matrix with dot products between
		for (int r = 0; r < C.nrow; r++)
			for (int c = 0; c < C.ncol; c++) {
				DecimalNumber sum = A.getRow(r).get(c).add(B.getRow(r).get(c), false);
				C.getRow(r).get(c).set(sum);
			}

		return C;
	}
	
	/** Adds the other matrix (with dimensions n*m) to this matrix (with dimensions
	 * n*m) as follows:
	 * 
	 * this(i, j) = this(i,j) + otherMatrix(i,j) forall k in [1, m].
	 * 
	 * if the dimensions of A and B do not match an
	 * illegalArgumentException is thrown.
	 * If this matrix is immutable, an UnsupportedOperationException is thrown.
	 * 
	 * returns this.
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public DecimalNumberMatrix matrixAddition (DecimalNumberMatrix otherMatrix)  {
		if (nrow != otherMatrix.nrow || ncol!= otherMatrix.ncol)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.matrixAddition: trying a add two matrix with unequal dimensions.");
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.matrixAddition: trying to add multiplicand to immutable matrix.");

		// Fill the matrix with dot products between
		for (int r = 0; r < nrow; r++)
			for (int c = 0; c < ncol; c++) {
				matrix[r].get(c).add(otherMatrix.getValueAt(r, c), true);
			}
		return this;
	}
	
	/** Applies the function to all DecimalNumber in the matrix. Throws UnsupportedComputationException if the matrix is immutable. Returns this 
	 * @return */
	public DecimalNumberMatrix apply(TransformationFunctionDecimalNumber function ) {
		if (immutable) throw new UnsupportedOperationException("Exception in DecimalNumberMatrix.apply: trying to apply function to immutable matrix.");
		for (int r = 0; r < nrow; r++)
			for (int c= 0; c< ncol; c++)
				matrix[r].get(c).set( function.function(matrix[r].get(c)));
		return this;
	}
	
	/** Creates a new matrix B, where
	 * B(i,j) = function(A(i,j)).
	 * 
	 * A remains unchanged. 
	 */
	public static DecimalNumberMatrix apply(DecimalNumberMatrix A, TransformationFunctionDecimalNumber function ) {
		DecimalNumberMatrix B = A.clone();
		for (DecimalNumberArray row: B)
			for (DecimalNumber v: row)
				v.set(function.function(v));
		return B;
		
	}
	
	 
	/////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	Other operations on this matrix 	/////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////

	/** 
	 * Returns a new DecimalNumberMatrix, which is the transpose of this matrix. This new DecimalNumberMatrix is a shallow copy.
	 * @return
	 */
	public DecimalNumberMatrix transpose() {
		// first, vectorize this array by row
		DecimalNumberArray vector = this.vectorize(true);
		
		// Create a new matrix, adding the values by column
		DecimalNumberMatrix newMatrix = new DecimalNumberMatrix(ncol, nrow, false, vector);
		
		// Set the col and row names
		newMatrix.columnNames = rowNames;
		newMatrix.rowNames = columnNames;
		
		return newMatrix;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	Matrix properties 	/////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Checks if value exists in the matrix. If approximately is true, values within 0.0000001 are counted as equal.
	 */
	public boolean contains (DecimalNumber value, boolean approximately)
	{
		for (DecimalNumberArray array: matrix)
			if (array.contains(value, approximately))
				return true;
		return false;
	}

	/**
	 * Checks if value exists in the matrix. If approximately is true, values within 0.0000001 are counted as equal.
	 */
	public boolean contains (double value, boolean approximately)
	{
		for (DecimalNumberArray array: matrix)
			if (array.contains(value, approximately))
				return true;
		return false;
	}
	
	/** Returns the sum of all DecimalNumbers in this matrix */
	public DecimalNumber sum() {
		DecimalNumber sum = new DecimalNumber(0);
		
		for (DecimalNumberArray row:this)
			sum.add(row.sum());
		return sum;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	////////////////// 	Subsets, reduction, combination, clone   /////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	
	/** Turns the matrix into a single array (shallow copy). If byrow, the array is ordered by row. If not byrow, the matrix is ordered by column */ 
	public DecimalNumberArray vectorize(boolean byrow) {
		DecimalNumberArray array = new DecimalNumberArray(ncol*nrow);
		if (byrow) {
			for (int r = 0; r < nrow; r++)
				for (int c = 0; c < ncol; c++)
					array.set(r*ncol+c, matrix[r].get(c));
		}
		else {
			for (int c = 0; c < ncol; c++)
				for (int r = 0; r < nrow; r++) 
					array.set(c*nrow+r, matrix[r].get(c));
		
		}
		return array;
	}
	
	/** Returns a DEEP clone (i.e., all DecimalNumberArrays and DecimalNumber's in
	 * this matrix are cloned as well). All cloned values are mutable.
	 */
	@Override
 	public DecimalNumberMatrix clone ()
	{
		DecimalNumberArray[] newMatrix = new DecimalNumberArray[nrow];
		for (int r=0;r<nrow;r++)
			newMatrix[r] = matrix[r].clone();
		
		DecimalNumberMatrix clone = new DecimalNumberMatrix(newMatrix);
		if (this.getColumnNames() != null) {
			String[] cloneNames = new String[columnNames.length];
			System.arraycopy( columnNames, 0, cloneNames, 0, columnNames.length );
			clone.setColumnNames(cloneNames);
		}
		
		if (this.getRowNames() != null) {
			String[] cloneNames = new String[rowNames.length];
			System.arraycopy( rowNames, 0, cloneNames, 0, rowNames.length );
			clone.setRowNames(cloneNames);
		}
		return clone;
	}
	
	/** Returns a SHALLOW clone of the the DecimalNumberMatrix. This means that
	 * the objects in the clone are the same references as the objects in the
	 * original - changes in the shallow clone result in changes of the deep clone.
	 * @return
	 */
	public DecimalNumberMatrix shallowClone() {
		DecimalNumberArray[] clonedMatrix = new DecimalNumberArray[nrow];
		for (int r=0;r<nrow;r++)
			clonedMatrix[r] = matrix[r].shallowClone();
		
		DecimalNumberMatrix clone = new DecimalNumberMatrix(clonedMatrix);
		clone.setColumnNames(columnNames);
		clone.setRowNames(rowNames);
		return clone;
	}

	/**
	 * Get a new DecimalNumberMatrix that houses a subset of all rows in the BigDecimalTable
	 * @param from
	 * @param to
	 * @return
	 */
	public DecimalNumberMatrix subsetRangeOfRows(int from, int to)
	{
		DecimalNumberMatrix result = new DecimalNumberMatrix(Arrays.copyOfRange(matrix, from, to));
		result.setColumnNames(columnNames);
		result.setRowNames(rowNames);
		return result;
	}
	
	/**
	 * Get a new DecimalNumberMatrix that houses a subset of all rows in the BigDecimalTable
	 * @param from
	 * @param to
	 * @return
	 */
	public DecimalNumberMatrix subsetRows(int... rowsToKeep)
	{
		DecimalNumberArray[] newMatrix = new DecimalNumberArray[rowsToKeep.length];
		for (int i = 0; i < rowsToKeep.length; i ++)
			newMatrix[i] = matrix[rowsToKeep[i]];
		
		DecimalNumberMatrix result = new DecimalNumberMatrix(newMatrix);
		result.setColumnNames(columnNames);
		result.setRowNames(rowNames);
		return result;
	}
	
	/**
	 * Returns a SHALLOW copy with the specified entries in the specified column either removed (if removeListed is true),
	 * or returns a copy with only the specified entries (if removeListed = false)
	 * 
	 * If approximate is set to true, values that are 0.00000001 or closer are considered to be equal (use this to
	 * combat rounding issues)
	 * @param columnNameToSelect
	 * @param entries
	 * @param removeListed
	 * @return
	 */
	public DecimalNumberMatrix reduce(String columnToSelect, DecimalNumberArray entries, boolean removeListed, boolean approximately)
	{
		int columnIndex = Helper.indexOf(columnToSelect, columnNames);
		if (columnIndex == -1) return null;
	
		ArrayList<DecimalNumberArray> newMatrixArrayList = new ArrayList<>();
		ArrayList<String> newRowNames = new ArrayList<>();
		if (removeListed)
			for (DecimalNumberArray array: matrix)
				for (int i = 0; i < entries.length(); i++)
					if (!array.contains(entries.get(i), approximately)) {
						newMatrixArrayList.add(array);
						newRowNames.add(rowNames[i]);
						}
		
		if (!removeListed)
			for (DecimalNumberArray array: matrix)
				for (int i = 0; i < entries.length(); i++)
					if (array.contains(entries.get(i), approximately)) {
						newMatrixArrayList.add(array);
						newRowNames.add(rowNames[i]);
						}
		
		DecimalNumberArray[] newArray = new DecimalNumberArray[newMatrixArrayList.size()];
		newArray = newMatrixArrayList.toArray(newArray);
		
		String[] namesArray = newRowNames.toArray(new String[newRowNames.size()]);
		
		DecimalNumberMatrix newMatrix = new DecimalNumberMatrix(newArray);
		newMatrix.setRowNames(namesArray);
		newMatrix.setColumnNames(columnNames);
		return newMatrix;
	}
	
	/**
	 * Combine the rows of two DecimalNumberMatrix's into a single, new table. Can only combine tables that have the same
	 * number of columns and have the same columnNames names. If only one, but not the other, matrix has specified row
	 * names, new row names with 0 characters (i.e., "") will be created for the not-specified names.
	 * @param table1
	 * @param table2
	 * @return
	 */
	public static DecimalNumberMatrix rowBind(DecimalNumberMatrix matrix1, DecimalNumberMatrix matrix2){
		if (matrix1.ncol != matrix2.ncol)
			throw new IllegalArgumentException("Trying to row bind two matrices with an unequal number of columns");
		
		if (matrix1.columnNames != null || matrix2.columnNames != null)
			for (int s = 0; s < matrix1.columnNames.length; s++)
				if (matrix1.columnNames[s].compareTo(matrix2.columnNames[s])!=0)
					throw new ComputationException("Trying to row bind two matrices with non-matching column names: at place " + s + " the column name of matrix 1 is \"" + matrix1.columnNames[s] + "\" while the column name of matrix 2 is \"" + matrix2.columnNames[s] + "\"." );


		int newNRow = matrix1.nrow+matrix2.nrow;
		DecimalNumberArray[] newMatrix = Arrays.copyOfRange(matrix1.matrix, 0, newNRow);
		System.arraycopy(matrix2.matrix, 0, newMatrix, matrix1.matrix.length, matrix2.matrix.length);
		DecimalNumberMatrix newDNM =  new DecimalNumberMatrix(newMatrix);
		
		// Adding col names (optional)
		if (matrix1.columnNames != null) {
			String[] newColumnNames = matrix1.columnNames;		
			newDNM.setColumnNames(newColumnNames);
		}
		
		// Adding row names (optional)
		if (matrix1.rowNames != null || matrix2.rowNames != null) {
			
			// Add or create the names from the first matrix (creating "" string if no names are specified).
			String[] newRowNames;
			if (matrix1.rowNames != null)
				newRowNames = Arrays.copyOf(matrix1.rowNames, newMatrix.length);
			else {
				newRowNames = new String[matrix1.nrow];
				for (int i = 0 ; i < matrix1.nrow; i ++)
					newRowNames[i] = "";
			}
			
			// Add or create the names from the second matrix (creating "" string if no names are specified). 
			if (matrix2.rowNames != null)
				for (int i = 0; i < matrix2.nrow; i++)
					newRowNames[matrix1.nrow + i] = matrix2.rowNames[i];
			else
				for (int i = matrix1.nrow; i < newDNM.nrow; i++)
					newRowNames[i] ="";
			newDNM.setRowNames(newRowNames);	
		}

		return newDNM;
			
	}
	
	 /** Combines the column vectors into a single, new table. Throws an IllegalArgumentException if the vectors are of unequal length
	 * The new matrix does not have row or column names.
	 * Changes in the new matrix will result in changes in the arrays, and vice versa.
	 * @param table1
	 * @param table2
	 * @return
	 */
	public static DecimalNumberMatrix columnBind(DecimalNumberArray... columnVectors) 	{
		if (columnVectors.length < 2)
			throw new IllegalArgumentException("Error in columnBind: trying to combine less than 2 vectors");
		
		int rows = columnVectors[0].length();
		for (DecimalNumberArray dna: columnVectors)
			if (dna.length() != rows)
				throw new IllegalArgumentException("Error in columnBind: trying to combine vectors of unequal length.");
		
		DecimalNumberMatrix matrix = new DecimalNumberMatrix(rows, columnVectors.length);
		for (int r = 0; r < rows; r++)
			for (int c = 0; c < columnVectors.length; c++) 
				matrix.setValueAt(r, c, columnVectors[c].get(r));

		return matrix;
		
	}
	
	
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Miscellaneous 	/////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////// 
	/** Creates new a new DecimalNumberArray that contains the values of this matrix per column, for each column. 
	 * These values are references, not copies (i.e., changes in the array results in changes in the matrix and 
	 * vice versa). Because under the hood the matrix is an array of row vectors, this is a relatively expensive 
	 * operation. It is advised not to use this function too often. If you have to use this function repeatedly, 
	 * consider transposing the matrix once, and using the rows instead. */
	public DecimalNumberArray[] toColumnVectors() {
		DecimalNumberArray[] columnVectors = new DecimalNumberArray[ncol];
		for (int c = 0; c < ncol; c++)
		{
			DecimalNumberArray columnVector = new DecimalNumberArray(nrow);
			for (int r = 0; r < nrow; r++)
				columnVector.set(r, matrix[r].get(c));
			columnVectors[c] = columnVector;
		}
		return columnVectors;
		
	}
	/** Two matrices are equal if and only if all entries have the same value. */
	public boolean equals (Object other) {
		if (!(other instanceof DecimalNumberMatrix))
			return false;
		
		DecimalNumberMatrix otherMatrix = (DecimalNumberMatrix) other;
		if (otherMatrix.nrow != nrow)
			return false;
		if (otherMatrix.ncol != ncol)
			return false;
		
		for (int row = 0; row < nrow; row++)
			for (int col = 0; col < ncol; col++)
				if (!matrix[row].get(col).equals(otherMatrix.getValueAt(row, col)))
					return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n" + Helper.repString("=", 100) + "\n");
		// print the column names, adding an empty column at the start if there are row names specified
		if (columnNames != null) {
			if (rowNames != null)
				sb.append(Helper.repString(" ", 24)+"\t");

			for (String h:columnNames)
			{
				sb.append(h);
				sb.append(Helper.repString(" ", 22 - h.length()));
				sb.append("\t");
			}
			sb.append("\n");
			if (rowNames != null)
				sb.append(Helper.repString("-", 24)+"\t");
			for (String s:columnNames) sb.append(Helper.repString("-", 22)+"\t");
			sb.append("\n");
		}
		
		//Print the body
		// If row names are specified, print those first
		for (int r = 0; r < matrix.length; r++)
		{
			if (rowNames!= null)
				sb.append(rowNames[r] + Helper.repString(" ", 22 - rowNames[r].length()) + "||\t");
			for (int c=0; c <ncol; c++)
			{
				String s;
				DecimalNumber entry = matrix[r].get(c);
				if (entry == null)							s = "NULL";
				else if (entry == DecimalNumber.NaN)		s = "NaN";
				else if (entry == DecimalNumber.NULL)		s = "Null";
				else if (entry == DecimalNumber.POSITIVE_INFINITY) s = "Inf";
				else if (entry == DecimalNumber.NEGATIVE_INFINITY) s = "-Inf";
				else {
					s =  df.format( matrix[r].get(c));
					if ( matrix[r].get(c).hasSpecifiedRange()) s = s + "'";
					if ( matrix[r].get(c).isImmutable()) s = s + "*";
				}
			
				
				String sSpaced = s + Helper.repString(" ", 22 - s.length());
				sb.append(sSpaced + "\t");
			}
			if (matrix[r].isImmutable()) sb.append("\t(*)");
			sb.append("\n");

		}
		
		sb.append("\n" + Helper.repString("-", 100) + "\n");
		sb.append("(* indicates immutable values; ' indicates values with specified range.)");
		if (immutable)
			sb.append("\n(!!! Warning: this matrix is immutable - the rows and column cannot be changed. !!!)");
		sb.append("\n" + Helper.repString("=", 100) + "\n");
		return sb.toString();

	}
	
	/**
	 * 
	 * @param significantDigits
	 * @param colwidth
	 * @return
	 */
	public String toString(int significantDigits, int colwidth)
	{
		DecimalFormat df = new DecimalFormat("0." + Helper.repString("#", significantDigits));
		StringBuilder sb = new StringBuilder();
		sb.append("\n" + Helper.repString("=", 100) + "\n");
		
		// Replace empty names with [,] formats
		boolean replacedColumnNames = false;
		if (columnNames == null) {
			replacedColumnNames = true;
			setColumnNames(createGenericColumnNames(ncol));
		}
		
		boolean replacedRowNames = false;
		if (rowNames == null) {
			replacedRowNames = true;
			setRowNames(createGenericRowNames(nrow));
		}
			
		// print the column names, adding an empty column at the start if there are row names specified
		sb.append(Helper.repString(" ", colwidth+2)+"  ");

		for (String h:columnNames){
			sb.append(h);
			sb.append(Helper.repString(" ", colwidth - h.length()));
			sb.append("  ");
		}
		sb.append("\n");
		if (rowNames != null)
			sb.append(Helper.repString("-", colwidth)+"  ");
		for (String s:columnNames) sb.append(Helper.repString("-", colwidth)+"  ");
		sb.append("\n");


		//Print the body
		// If row names are specified, print those first
		for (int r = 0; r < matrix.length; r++)
		{
			if (rowNames!= null)
				sb.append(rowNames[r] + Helper.repString(" ", colwidth - rowNames[r].length()) + "||\t");
			for (int c=0; c <ncol; c++)
			{
				String s;
				DecimalNumber entry = matrix[r].get(c);
				if (entry == null)							s = "NULL";
				else if (entry == DecimalNumber.NaN)		s = "NaN";
				else if (entry == DecimalNumber.NULL)		s = "Null";
				else if (entry == DecimalNumber.POSITIVE_INFINITY) s = "Inf";
				else if (entry == DecimalNumber.NEGATIVE_INFINITY) s = "-Inf";
				else {
					s =  df.format( matrix[r].get(c));
					if ( matrix[r].get(c).hasSpecifiedRange()) s = s + "'";
					if ( matrix[r].get(c).isImmutable()) s = s + "*";
				}
			
				
				String sSpaced = s + Helper.repString(" ", colwidth - s.length());
				sb.append(sSpaced + "  ");
			}
			if (matrix[r].isImmutable()) sb.append("\t(*)");
			sb.append("\n");

		}
		sb.append("\n" + Helper.repString("=", 100) + "\n");
		
		if (replacedColumnNames) columnNames = null;
		if (replacedRowNames) rowNames = null;
		return sb.toString();

	}

	public static String[] createGenericColumnNames(int ncol) {
		String[] names = new String[ncol];
		for (int i = 0; i < ncol; i++)
			names[i] = "[ ," + i + "]";
		return names;
	}
	
	public static String[] createGenericRowNames(int nrow) {
		String[] names = new String[nrow];
		for (int i = 0; i < nrow; i++)
			names[i] = "[" + i + ", ]";
		return names;
	}
	
	/**
	 * Transforms the BigDecimalTable to a string that can be read into R.
	 * That is, a string of the format:
	 * <p>dataFrameName = as.data.frame(matrix(0,ncol=0,nrow=0));
	 * <p>dataFrameName$variableName = c(BigDecimal1, BigDecimal2, ..., BigDecimalN);
	 * <p>colnames(dataFrameName) = c("varName1", "varName2", ..., "varNamen");
	 * <p> Note: characters (, ), -, " ","|" are replaced with "_"
	 *
	 * @param dataFrameName
	 * @return
	 */
	public String toDataFrame(String dataFrameName)
	{
		StringBuilder dataFrame = new StringBuilder();
		dataFrame.append(dataFrameName + "<- as.data.frame(matrix(0, ncol=0,nrow="+nrow+"));\n");
		for (int c = 0; c<ncol;c++)
		{
			DecimalNumberArray var = this.getColumn(c);
			String varName =  columnNames[c];
			varName = varName.replace("(", "_");
			varName = varName.replace(")", "_");
			varName = varName.replace("-", "minus");
			varName = varName.replace(" ", "_");
			varName = varName.replace("|", "_");
			varName = varName.replace("+", "plus");
			dataFrame.append(dataFrameName+"$" +varName+"<-");
			dataFrame.append(var.concatenateRStyle() + ";\n");
		}

		return dataFrame.toString();

	}
	
	public String toCSV(String delimiter)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(columnNames[0]);
		for (int i=1;i<columnNames.length;i++)
			sb.append(delimiter + columnNames[i]);
		sb.append("\n");
		for (int row = 0; row<this.nrow; row++)
		{
			sb.append(matrix[row].get(0));
			for (int col = 1; col < this.ncol; col++)
				sb.append(delimiter + matrix[row].get(col));
			sb.append("\n");
		}
		return sb.toString();
	}

	public boolean sort(int columnToSortOn, boolean ascending) {
		if (columnToSortOn < 0 || columnToSortOn >= ncol || immutable)
			return false;
		
		final Comparator<DecimalNumberArray> comparator = new Comparator<DecimalNumberArray>() {

			@Override
			public int compare(DecimalNumberArray o1, DecimalNumberArray o2) {
				return o1.get(columnToSortOn).compareTo(o2.get(columnToSortOn));
			}
		};
		
		if (ascending)
			Arrays.sort(matrix, comparator);
		else
			Arrays.sort(matrix, Collections.reverseOrder(comparator));
		return true;
	}
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(matrix);
		result = prime * result + ((df == null) ? 0 : df.hashCode());
		result = prime * result + Arrays.hashCode(columnNames);
		result = prime * result + ncol;
		result = prime * result + nrow;
		return result;
	}

	@Override
	public Iterator<DecimalNumberArray> iterator() {
		return new Iterator<DecimalNumberArray>() {
			int currentIndex = 0;
			
			@Override
			public boolean hasNext() {
				return currentIndex < nrow && matrix[currentIndex] != null;
			}

			@Override
			public DecimalNumberArray next() {
				return matrix[currentIndex++];
			}
			
		};
	}

	
	/////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	Double only analogues 	/////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////
	/** <pre> DOUBLE ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED.
	 * 
	 * Multiplies double[][] A (with dimensions n*m) with double[][] B (with dimensions
	 * m * p). The result is a matrix C with dimensions n*p, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = sum ( A(i,k) * B(k,j) ) forall k in [1, m].
	 * 
	 * if the number of columns in A does not match the number of rows in B (i.e.,
	 * ( dimension(A) = [n,m1], dimension(B) = [m2, p], and m1 != m2), an
	 * illegalArgumentException is thrown.
	 * 
	 * This method has both input and output of form double[][]. The results from this method are 
	 * NOT free of floating point issues - specifically, results cannot be trusted after
	 * the 9th decimal point or so. 
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static double[][] DOUBLE_matrixMultiplicationIKJ (double[][] A, double[][] B)  {
		int nrowA = A.length;
		int nrowB = B.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(): argument A has rows of unequal sizes.");
		int ncolB = B[0].length; 
		for (int i = 0; i < B.length; i++) if (B[i].length!= ncolB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(): argument B has rows of unequal sizes.");
		
		if (ncolA != nrowB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(): trying a multiply two matrix of size n * m1 with a matrix of size m2 * p, but m1 is not equal to m2.");
		
		
		double[][] C  = new double[nrowA][ncolB];
		for (int i = 0; i < nrowA; i++) 
	        for (int k = 0; k < ncolA; k++) 
	            for (int j = 0; j < ncolB; j++) 
	            	C[i][j] += A[i][k] * B[k][j];
		return C;
	}
	
	/** <pre> DOUBLE ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Creates a new double[][] where all entries are the scalar product of the values in double[][] A and the multiplicand. 
	 */
	public static double[][] DOUBLE_scalarMultiplication(double[][] A, double multiplicand) {
		int nrowA = A.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_scalarMultiplication(): argument A has rows of unequal sizes.");
		
		double[][] C = new double[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c = 0; c< ncolA; c++)
				C[r][c] = A[r][c]*multiplicand;
	
		return C;
	}
	
	/** <pre> DOUBLE ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Creates a new matrix C that is the entrywise (or Hadamard) product of matrices A and B. Specifically,
	 * creates a new matrix C where
	 * 
	 * C(i,j) = A(i,j) * B(i,j), for all rows i and columns j.
	 * 
	 * This does not influence matrices A or B.
	 * 
	 * Throws an IllegalArgumentException if the dimensions of A are unequal to the dimensions of C
	 * @param A
	 * @param B
	 * @return
	 */
	public static double[][] DOUBLE_matrixAddition(double[][]  A, double[][] B) {
		int nrowA = A.length;
		int nrowB = B.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): argument A has rows of unequal sizes.");
		int ncolB = B[0].length; 
		for (int i = 0; i < B.length; i++) if (B[i].length!= ncolB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): argument B has rows of unequal sizes.");
		
		if (ncolA != ncolB|| nrowA != nrowB)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_matrixAddition(): the dimensions of A do not match the dimensions of B" );
		
		double[][] C = new double[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c= 0; c< ncolA; c++) 
				C[r][c] = A[r][c] + B[r][c];
			
		return C;
	}
	
	/** <pre> DOUBLE ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Creates a new matrix C that is the entrywise (or Hadamard) product of matrices A and B. Specifically,
	 * creates a new matrix C where
	 * 
	 * C(i,j) = A(i,j) * B(i,j), for all rows i and columns j.
	 * 
	 * This does not influence matrices A or B.
	 * 
	 * Throws an IllegalArgumentException if the dimensions of A are unequal to the dimensions of C
	 * @param A
	 * @param B
	 * @return
	 */
	public static double[][] DOUBLE_entrywiseMultiplication(double[][]  A, double[][] B) {
		int nrowA = A.length;
		int nrowB = B.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): argument A has rows of unequal sizes.");
		int ncolB = B[0].length; 
		for (int i = 0; i < B.length; i++) if (B[i].length!= ncolB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): argument B has rows of unequal sizes.");
		
		if (ncolA != ncolB|| nrowA != nrowB)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): the dimensions of A do not match the dimensions of B" );
		
		double[][] C = new double[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c= 0; c< ncolA; c++) 
				C[r][c] = A[r][c] * B[r][c];
			
		return C;
	}
	
	/** <pre> DOUBLE ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Returns a new matrix B where all entries are:
	 * B(i,j) = A(i,j) + augend.
	 * 
	 * Note that this operation does not change values in A.
	 * @param A
	 * @param augend
	 */
	public static double[][] DOUBLE_scalarAddition(double[][] A, double augend) {
		int nrowA = A.length;

		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_scalarAddition(): argument A has rows of unequal sizes.");

		double[][] C = new double[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c = 0; c< ncolA; c++)
				C[r][c] = A[r][c]+augend;

		return C;
	}
	
	public static double[][] DOUBLE_unityMatrix(int nrow, int ncol) {
		double[][] unity = new double[nrow][ncol];
		for (int r = 0; r < nrow; r++)
			for (int c = 0; c < ncol; c++)
				unity[r][c] = 1;
		return unity;
	}
	
	public static double[][] DOUBLE_transpose(double[][] A){
		int nrowA = A.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_transpose(): argument A has rows of unequal sizes.");
		
		double[][] transpose = new double[ncolA][nrowA];
		for (int rC = 0; rC < ncolA; rC ++)
			for (int cC = 0; cC < nrowA; cC++)
				transpose[rC][cC] = A[cC][rC];
		return transpose;
	}
	
	public static double[][] DOUBLE_clone(double[][] A){
		double [][] result = new double[A.length][];
		for(int r = 0; r < A.length; r++)		{
		  double[] oldRow = A[r];
		  double[] newRow  = new double[oldRow.length];
		  System.arraycopy(oldRow, 0, newRow, 0, oldRow.length);
		  result[r] = newRow;
		}
		return result;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	Integer only analogues 	/////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////
	/** <pre> INTEGER ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED.
	 * 
	 * Multiplies int[][] A (with dimensions n*m) with int[][] B (with dimensions
	 * m * p). The result is a matrix C with dimensions n*p, where the entry at row
	 * i and column j is:
	 * 
	 * C(i, j) = sum ( A(i,k) * B(k,j) ) forall k in [1, m].
	 * 
	 * if the number of columns in A does not match the number of rows in B (i.e.,
	 * ( dimension(A) = [n,m1], dimension(B) = [m2, p], and m1 != m2), an
	 * illegalArgumentException is thrown.
	 * 
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static int[][] INTEGER_matrixMultiplicationIKJ (int[][] A, int[][] B)  {
		int nrowA = A.length;
		int nrowB = B.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(): argument A has rows of unequal sizes.");
		int ncolB = B[0].length; 
		for (int i = 0; i < B.length; i++) if (B[i].length!= ncolB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(): argument B has rows of unequal sizes.");
		
		if (ncolA != nrowB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(): trying a multiply two matrix of size n * m1 with a matrix of size m2 * p, but m1 is not equal to m2.");
		
		
		int[][] C  = new int[nrowA][ncolB];
		for (int i = 0; i < nrowA; i++) 
	        for (int k = 0; k < ncolA; k++) 
	            for (int j = 0; j < ncolB; j++) 
	            	C[i][j] += A[i][k] * B[k][j];
		return C;
	}
	
	/** <pre> INTEGER ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Creates a new int[][] where all entries are the scalar product of the values in int[][] A and the multiplicand. 
	 */
	public static int[][] INTEGER_scalarMultiplication(int[][] A, int multiplicand) {
		int nrowA = A.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_scalarMultiplication(): argument A has rows of unequal sizes.");
		
		int[][] C = new int[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c = 0; c< ncolA; c++)
				C[r][c] = A[r][c]*multiplicand;
	
		return C;
	}
	
	/** <pre> INTEGER ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Creates a new matrix C that is the entrywise (or Hadamard) product of matrices A and B. Specifically,
	 * creates a new matrix C where
	 * 
	 * C(i,j) = A(i,j) * B(i,j), for all rows i and columns j.
	 * 
	 * This does not influence matrices A or B.
	 * 
	 * Throws an IllegalArgumentException if the dimensions of A are unequal to the dimensions of C
	 * @param A
	 * @param B
	 * @return
	 */
	public static int[][] INTEGER_entrywiseMultiplication(int[][]  A, int[][] B) {
		int nrowA = A.length;
		int nrowB = B.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): argument A has rows of unequal sizes.");
		int ncolB = B[0].length; 
		for (int i = 0; i < B.length; i++) if (B[i].length!= ncolB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): argument B has rows of unequal sizes.");
		
		if (ncolA != ncolB|| nrowA != nrowB)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(): the dimensions of A do not match the dimensions of B" );
		
		int[][] C = new int[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c= 0; c< ncolA; c++) 
				C[r][c] = A[r][c] * A[r][c];
			
		return C;
	}
	
	/** <pre> INTEGER ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Returns a new matrix B where all entries are:
	 * B(i,j) = A(i,j) + augend.
	 * 
	 * Note that this operation does not change values in A.
	 * @param A
	 * @param augend
	 */
	public static int[][] INTEGER_scalarAddition(int[][] A, int augend) {
		int nrowA = A.length;

		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_scalarAddition(): argument A has rows of unequal sizes.");

		int[][] C = new int[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c = 0; c< ncolA; c++)
				C[r][c] = A[r][c]+augend;

		return C;
	}
	
	/** <pre> INTEGER ONLY - DOES NOT USE DECIMALNUMBERMATRIX. RESULTS CANNOT ALWAYS BE TRUSTED (especially after 9th decimal).
	 * Creates a new matrix C that is the entrywise (or Hadamard) product of matrices A and B. Specifically,
	 * creates a new matrix C where
	 * 
	 * C(i,j) = A(i,j) * B(i,j), for all rows i and columns j.
	 * 
	 * This does not influence matrices A or B.
	 * 
	 * Throws an IllegalArgumentException if the dimensions of A are unequal to the dimensions of C
	 * @param A
	 * @param B
	 * @return
	 */
	public static int[][] INTEGER_matrixAddition(int[][]  A, int[][] B) {
		int nrowA = A.length;
		int nrowB = B.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.INTEGER_matrixAddition(): argument A has rows of unequal sizes.");
		int ncolB = B[0].length; 
		for (int i = 0; i < B.length; i++) if (B[i].length!= ncolB) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.INTEGER_matrixAddition(): argument B has rows of unequal sizes.");
		
		if (ncolA != ncolB|| nrowA != nrowB)
			throw new IllegalArgumentException("Exception in DecimalNumberMatrix.INTEGER_matrixAddition(): the dimensions of A do not match the dimensions of B" );
		
		int[][] C = new int[nrowA][ncolA];
		for (int r = 0; r < nrowA; r++)
			for (int c= 0; c< ncolA; c++) 
				C[r][c] = A[r][c] + B[r][c];
			
		return C;
	}
	
	
	
	public static int[][] INTEGER_unityMatrix(int nrow, int ncol) {
		int[][] unity = new int[nrow][ncol];
		for (int r = 0; r < nrow; r++)
			for (int c = 0; c < ncol; c++)
				unity[r][c] = 1;
		return unity;
	}
	
	public static int[][] INTEGER_transpose(int[][] A){
		int nrowA = A.length;
		
		int ncolA = A[0].length; 
		for (int i = 0; i < A.length; i++) if (A[i].length!= ncolA) throw new IllegalArgumentException("Exception in DecimalNumberMatrix.DOUBLE_transpose(): argument A has rows of unequal sizes.");
		
		int[][] transpose = new int[ncolA][nrowA];
		for (int rC = 0; rC < ncolA; rC ++)
			for (int cC = 0; cC < nrowA; cC++)
				transpose[rC][cC] = A[cC][rC];
		return transpose;
	}
	
	public static int[][] INTEGER_clone(int[][] A){
		int[][] result = new int[A.length][];
		for(int r = 0; r < A.length; r++)		{
		  int[] oldRow = A[r];
		  int[] newRow  = new int[oldRow.length];
		  System.arraycopy(oldRow, 0, newRow, 0, oldRow.length);
		  result[r] = newRow;
		}
		return result;
	}
}

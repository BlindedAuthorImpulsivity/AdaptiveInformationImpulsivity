package decimalNumber;

public class TransformationFunction {
	/** Specifies a transformation function for a single DecimalNumber.
	 * Formally, specifies a function of the form:
	 * 
	 * input ( DecimalNumber) |-> output ( DecimalNumber )
	 * 
	 * This interface is used when transforming all values in a DecimalNumberArray
	 * or DecimalNumberMatrix, and can be used in an anonymous class when
	 * calling applyFunction() */
	public interface TransformationFunctionDecimalNumber {
		public DecimalNumber function(DecimalNumber argument);
	}
	
	/** Specifies a transformation function for a single DecimalArray.
	 * Formally, specifies a function of the form:
	 * 
	 * input ( DecimalNumberArray) |-> output ( DecimalNumber )
	 * 
	 * This interface is used when transforming all values in a DecimalNumberArray
	 * or DecimalNumberMatrix, and can be used in an anonymous class when
	 * calling applyFunction() */
	public interface TransformationFunctionDecimalNumberArray {
		public DecimalNumber function(DecimalNumberArray argument);

	}

	
}

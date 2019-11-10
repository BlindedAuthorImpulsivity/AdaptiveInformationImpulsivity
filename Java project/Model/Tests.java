import java.util.HashMap;

import org.junit.jupiter.api.Test;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import defaultAndHelper.JavaFXHelper;

class Tests {


	DecimalNumber lookupDecimalNumberMatrix(DecimalNumberMatrix valueFunction, DecimalNumber budget) {
		return valueFunction.getRowWhereColumnIs("Budget", budget).get(1).clone();
	}
	
	DecimalNumber lookupDecimalHashMap(HashMap<DecimalNumber, DecimalNumber> valueFunction, DecimalNumber budget) {
		return valueFunction.get(budget).clone();
	}
	
	void testReadRandomValueFunction(double max, double stepSize, int valueFunctionsToTest, long getsToTest) {
		long startTime, estimatedTime;
		
		DecimalNumberArray budgets = DecimalNumberArray.sequence(0, max, stepSize);
		DecimalNumberArray values = budgets.clone().scale(2);
		
		DecimalNumberMatrix matrix = new DecimalNumberMatrix(budgets, values).transpose();
		matrix.setColumnNames("Budget", "Value");
		
		HashMap<DecimalNumber, DecimalNumber> map = new HashMap<>();
		for (int i = 0; i < budgets.length(); i++)
			map.put(budgets.get(i), values.get(i));
		
		startTime = System.nanoTime();    
		for (int i = 0; i < getsToTest; i++) {
			int loc = (int) (Math.random()*budgets.length());
			lookupDecimalNumberMatrix(matrix, budgets.get(loc).clone());
		}
		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for DecimalNumberMatrix:             \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

		
		startTime = System.nanoTime();    
		for (int i = 0; i < getsToTest; i++) {
			int loc = (int) (Math.random()*budgets.length());
			lookupDecimalHashMap(map, budgets.get(loc).clone());
		}
		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for HashMap:                         \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

	
			
	}
	
	
	
	
	
	@Test
	void test() {
		
		testReadRandomValueFunction(1000, 1, 100, 1000000);
		
	}

}

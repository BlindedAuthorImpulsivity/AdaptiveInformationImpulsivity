package decimalNumber;

import java.text.DecimalFormat;

import helper.Helper;
import javafx.util.StringConverter;

/** A safe converter from DecimalNumbers to Strings and vice versa. Will not throw errors with invalid input.
 *
 */
public class SafeDecimalNumberToStringConverter extends StringConverter<DecimalNumber>
{

	public final DecimalNumber 	minimum, maximum;
	public final boolean 		rangeSpecified;
	private DecimalFormat df;

	public SafeDecimalNumberToStringConverter(double minimum, double maximum)
	{
		this.minimum = new DecimalNumber(minimum, true);
		this.maximum = new DecimalNumber(maximum, true);
		this.rangeSpecified = true;
	}

	public SafeDecimalNumberToStringConverter()
	{
		this.rangeSpecified = false;
		this.minimum = DecimalNumber.ZERO;
		this.maximum = DecimalNumber.ZERO;
	}

	/**
	 * Parses a String object to a BigDecimal object. If the String is not a valid number (i.e., it does not have the shape
	 * of x or x.x), a BigDecimal.ONE is returned instead.
	 *
	 * If a range is specified, the value is automatically encoded to be in this interval.
	 */
	public DecimalNumber fromString(String value)
	{
		if (Helper.isDouble(value))
		{
			DecimalNumber result = new DecimalNumber(value);
			if (rangeSpecified)
			{
				if (result.compareTo(minimum)==-1)
					result = minimum;
				if (result.compareTo(maximum)==1)
					result = maximum;
			}
			return result;
		}

		return DecimalNumber.NaN;
	}

	public SafeDecimalNumberToStringConverter setSignificantDigits(int n)
	{
		this.df = new DecimalFormat("0." + Helper.repString("#", n));
		return this;
	}

	public String toString(DecimalNumber value)
	{
		if (value == null)
			return "NULL POINTER";
		if (this.df != null)
			return df.format(value.doubleValue());
		return value.toString();
	}

}


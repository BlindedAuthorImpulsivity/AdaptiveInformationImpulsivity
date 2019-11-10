package helper;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;

/** A wrapper for an Array that only has getters, no setters.
 * Hence, if this array is declared final, neither it, nor the
 * objects within it are mutable. 
 *
 */
public class  ImmutableArray<T extends Serializable> extends AbstractCollection<T> implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1272547775956313837L;
	private final T[] array;
	private final int size;
	
	public ImmutableArray(T... elements){
		this.array = Arrays.copyOf(elements, elements.length);
		this.size = array.length;
	}
	
	public ImmutableArray<T> toImmutableArray(T... elements){
		return new ImmutableArray<T>(elements);
	}
	
	
	public T get(int index)	{
		return array[index];
	}
	
	public int length() {
		return size;
	}
	
	public int size() {
		return size;
	}
	
	public T[] mutableCopy() {
		return Arrays.copyOf(array, size);
	}

	public int indexOf (T element) {
		for (int i = 0; i < size; i++)
			if (element.equals(array[i]))
				return i;
		return -1;
	}
	
	@Override
	public String toString() {
		return Helper.arrayToString(array);
	}
	
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int currentIndex = 0;
			
			@Override
			public boolean hasNext() {
				return currentIndex < length() && array[currentIndex] != null;
			}

			@Override
			public T next() {
				return array[currentIndex++];
			}
			
		};
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
		result = prime * result + size;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImmutableArray<?> other = (ImmutableArray<?>) obj;
		if (!Arrays.equals(array, other.array))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

}

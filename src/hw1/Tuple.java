package hw1;

import java.sql.Types;
import java.util.HashMap;

/**
 * This class represents a tuple that will contain a single row's worth of information
 * from a table. It also includes information about where it is stored
 * @author Sam Madden modified by Doug Shook
 *
 */
public class Tuple {
	
	private int pid;
	private int id;
	private TupleDesc td;
	private Field[] dataArray;
	
	/**
	 * Creates a new tuple with the given description
	 * @param t the schema for this tuple
	 */
	
	public Tuple(TupleDesc t) {
		//your code here
		this.td = t;
		dataArray = new Field[this.td.numFields()];
	}
	
	public TupleDesc getDesc() {
		//your code here
		return td;
	}
	
	/**
	 * retrieves the page id where this tuple is stored
	 * @return the page id of this tuple
	 */
	public int getPid() {
		//your code here
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
		//your code here
	}

	/**
	 * retrieves the tuple (slot) id of this tuple
	 * @return the slot where this tuple is stored
	 */
	public int getId() {
		//your code here
		return id;
	}

	public void setId(int id) {
		//your code here
		this.id = id;
	}
	
	public void setDesc(TupleDesc td) {
		this.td = td;
	}
	
	/**
	 * Stores the given data at the i-th field
	 * @param i the field number to store the data
	 * @param v the data
	 */
	public void setField(int i, Field v) {
		this.dataArray[i] = v;
	}
	
	public Field getField(int i) {
		//your code here
		return this.dataArray[i];
	}
	
	/**
	 * Creates a string representation of this tuple that displays its contents.
	 * You should convert the binary data into a readable format (i.e. display the ints in base-10 and convert
	 * the String columns to readable text).
	 */
	public String toString() {
		String s = new String();
		for (int i = 0; i < this.dataArray.length; i++) {
			Type type = td.getType(i);
			String fieldName = td.getFieldName(i);
			Field field = this.getField(i);
			String toAdd = new String();
			if (type == Type.STRING) {
				StringField newString = new StringField(field.toByteArray());
				toAdd = newString.toString();
			}
			else if (type == Type.INT) {
				IntField newInt = new IntField(field.toByteArray());
				toAdd = newInt.toString();
			}
			s = s + "Position: " + i + ", Type: " + String.valueOf(type) + ", Fieldname: " + fieldName + ", Field Content: " + toAdd + "\n";
		}
		return s;
	}
}
	
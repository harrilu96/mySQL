package hw2;

import java.util.ArrayList;

import hw1.Field;
import hw1.IntField;
import hw1.RelationalOperator;
import hw1.StringField;
import hw1.Tuple;
import hw1.TupleDesc;
import hw1.Type; //imported because need to use 

/**
 * This class provides methods to perform relational algebra operations. It will be used
 * to implement SQL queries.
 * @author Doug Shook
 *
 */
public class Relation {

	private ArrayList<Tuple> tuples;
	private TupleDesc td;
	
	public Relation(ArrayList<Tuple> l, TupleDesc td) {
		this.tuples = l;
		this.td = td;
	}
	
	/**
	 * This method performs a select operation on a relation
	 * @param field number (refer to TupleDesc) of the field to be compared, left side of comparison
	 * @param op the comparison operator
	 * @param operand a constant to be compared against the given column
	 * @return
	 */
	public Relation select(int field, RelationalOperator op, Field operand) {
		ArrayList<Tuple> tupleList = new ArrayList<Tuple>(this.tuples);
		Type type = this.td.getType(field);
		if (type == Type.INT) {
			IntField toCompare = (IntField) operand;
			for (Tuple tuple : this.tuples) {
				if (!tuple.getField(field).compare(op, toCompare)) {
					tupleList.remove(tuple);
				}
			}
		}
		if (type == Type.STRING) {
			StringField toCompare = (StringField) operand;
			for (Tuple tuple : this.tuples) {
				if (!tuple.getField(field).compare(op, toCompare)) {
					tupleList.remove(tuple);
				}
			}
		}
		return new Relation(tupleList, td);
	}
	
	/**
	 * This method performs a rename operation on a relation
	 * @param fields the field numbers (refer to TupleDesc) of the fields to be renamed
	 * @param names a list of new names. The order of these names is the same as the order of field numbers in the field list
	 * @return
	 */
	public Relation rename(ArrayList<Integer> fields, ArrayList<String> names) {
		Type[] copyType = new Type[this.td.numFields()];
		String[] newField = new String[this.td.numFields()];
		for (int i = 0; i < this.td.numFields(); i++) {
			copyType[i] = this.td.getType(i);
			if (fields.contains(i)) {
				newField[i] = names.get(i);
			}
			else {
				newField[i] = this.td.getFieldName(i);
			}
		}
		TupleDesc newTD = new TupleDesc(copyType, newField);
		return new Relation(this.tuples, newTD);
	}
	
	/**
	 * This method performs a project operation on a relation
	 * @param fields a list of field numbers (refer to TupleDesc) that should be in the result
	 * @return
	 */
	public Relation project(ArrayList<Integer> fields) {
		Type[] newType = new Type[fields.size()];
		String[] newField = new String[fields.size()];
		ArrayList<Tuple> tupleList = new ArrayList<Tuple>();
		for (int i = 0; i < fields.size(); i++) {
			newType[i] = this.td.getType(fields.get(i));
			newField[i] = this.td.getFieldName(fields.get(i));
		}
		for (Tuple tuple : this.tuples) {
			Tuple toAppend = new Tuple(new TupleDesc (newType, newField));
			for (int i = 0; i < fields.size(); i++) {
				toAppend.setField(i, tuple.getField(fields.get(i)));
			}
			tupleList.add(toAppend);
		}
		return new Relation(tupleList, new TupleDesc(newType, newField));
	}
	
	/**
	 * This method performs a join between this relation and a second relation.
	 * The resulting relation will contain all of the columns from both of the given relations,
	 * joined using the equality operator (=)
	 * @param other the relation to be joined
	 * @param field1 the field number (refer to TupleDesc) from this relation to be used in the join condition
	 * @param field2 the field number (refer to TupleDesc) from other to be used in the join condition
	 * @return
	 */
	public Relation join(Relation other, int field1, int field2) throws Exception {
		//check to make sure the field type is the same
		if (this.td.getType(field1) != other.td.getType(field2)) {
			throw new Exception("Field types not equal");
		}
		
		//create new tuple description joining tds in both columns
		Type[] newType = new Type[(this.td.numFields() + other.td.numFields())];
		String[] newField = new String[(this.td.numFields() + other.td.numFields())];
		for (int i = 0; i < this.td.numFields(); i++) {
			newType[i] = this.td.getType(i);
			newField[i] = this.td.getFieldName(i);
		}
		for (int i = 0; i < other.td.numFields(); i++) {
			newType[this.td.numFields() + i] = other.td.getType(i);
			newField[this.td.numFields() + i] = other.td.getFieldName(i);
		}
		
		//create new tuples
		TupleDesc newTupleDesc = new TupleDesc(newType, newField);
		ArrayList<Tuple> newTuples = new ArrayList<Tuple>();
		for (Tuple tuple : this.tuples) {
			for (Tuple otherTuple : other.tuples) {
				if (tuple.getField(field1).compare(RelationalOperator.EQ, otherTuple.getField(field2))) {
					System.out.println("SUCCESS");
					Tuple newTuple = new Tuple(newTupleDesc);
					for (int i = 0; i < this.td.numFields(); i++) {
						newTuple.setField(i, tuple.getField(i));
					}
					for (int i = 0; i < other.td.numFields(); i++) {
						newTuple.setField(i + this.td.numFields(),  otherTuple.getField(i));
					}
					newTuples.add(newTuple);
				}
			}
		} 
		return new Relation(newTuples, newTupleDesc);
	}
	
	
	/**
	 * Performs an aggregation operation on a relation. See the lab write up for details.
	 * @param op the aggregation operation to be performed
	 * @param groupBy whether or not a grouping should be performed
	 * @return
	 * @throws Exception 
	 */
	public Relation aggregate(AggregateOperator op, boolean groupBy) throws Exception {
		Aggregator aggregator = new Aggregator(op, groupBy, this.td);
		if (groupBy) {
			for (Tuple tuple : this.tuples) {
				aggregator.merge(tuple);
			}
		}
		else {
			for (Tuple tuple : this.tuples) {
				aggregator.merge(tuple);
			}
		}
		Relation toReturn = new Relation(aggregator.getResults(), this.td);
		return toReturn;
	}
	
	public TupleDesc getDesc() {
		return this.td;
	}
	
	public ArrayList<Tuple> getTuples() {
		return this.tuples;
	}
	
	/**
	 * Returns a string representation of this relation. The string representation should
	 * first contain the TupleDesc, followed by each of the tuples in this relation
	 */
	public String toString() {
		String s = this.td.toString() + " ";
		for(int i = 0; i < this.tuples.size(); i++) {
			if(i+1 == this.tuples.size()) {
				s += this.tuples.get(i).toString();
			}
			s += this.tuples.get(i).toString() + ", ";
		}
		return s;
	}
}

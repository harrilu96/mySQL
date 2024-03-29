package hw1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import hw4.Permissions;

// QUESTIONS: addTuple(), deleteTuple() how to check for "structure"?

public class HeapPage {

	private int id;
	private byte[] header;
	private Tuple[] tuples; // length: numSlots, one slot = one tuple
	private TupleDesc td;
	private int numSlots;
	private int tableId;


	public HeapPage(int id, byte[] data, int tableId) throws IOException {
		this.id = id;
		this.tableId = tableId;

		this.td = Database.getCatalog().getTupleDesc(this.tableId);
		this.numSlots = getNumSlots();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
		
		// allocate and read the header slots of this page
		header = new byte[getHeaderSize()];
		for (int i=0; i<header.length; i++)
			header[i] = dis.readByte();

		try{
			// allocate and read the actual records of this page
			tuples = new Tuple[numSlots];
			for (int i=0; i<tuples.length; i++)
				tuples[i] = readNextTuple(dis,i);
		}catch(NoSuchElementException e){
			e.printStackTrace();
		}
		dis.close();
	}

	public int getId() {
		return this.id;
	}

	/**
	 * Computes and returns the total number of slots that are on this page (occupied or not).
	 * Must take the header into account!
	 * @return number of slots on this page
	 */
	public int getNumSlots() {
		return (int) (8 * HeapFile.PAGE_SIZE) / ((8 * td.getSize()) + 1);
	}

	/**
	 * Computes the size of the header. Headers must be a whole number of bytes (no partial bytes)
	 * @return size of header in bytes
	 */
	private int getHeaderSize() {
		return (int) Math.ceil((this.getNumSlots() / 8.0));
	}

	/**
	 * Checks to see if a slot is occupied or not by checking the header
	 * @param s the slot to test
	 * @return true if occupied
	 */
	public boolean slotOccupied(int s) {
		// s is a bit in the header
		int i = (int) (s / 8);
		byte b = this.header[i];
		int position = s - (8 * i);
		return ((b >> position) & 1) == 1;
	}

	/**
	 * Sets the occupied status of a slot by modifying the header
	 * @param s the slot to modify
	 * @param value its occupied status
	 */
	public void setSlotOccupied(int s, boolean value) {
		int i = (int) (s / 8);
		int position = s - (8 * i);
		this.header[i] = value ? (byte) (this.header[i] | (1 << position)) : (byte) (this.header[i] & ~(1 << position));
	}
	
	/**
	 * Adds the given tuple in the next available slot. Throws an exception if no empty slots are available.
	 * Also throws an exception if the given tuple does not have the same structure as the tuples within the page.
	 * @param t the tuple to be added.
	 * @throws Exception
	 */
	public void addTuple(Tuple t) throws Exception {
		TupleDesc td = t.getDesc();
		// checking for structure of tuple
		if(!td.equals(this.td)) {
			throw new Exception("Tuple structure does not match the tuples in the heap page");
		}
		// first check if there are available slots
		boolean full = true;
		int unoccupiedSlot = 0;
		for(int i = 0; i < this.getNumSlots(); i++) {
			if(!this.slotOccupied(i)) {
				full = false;
				unoccupiedSlot = i;
				break;
			}
		}
		if(full) {
			throw new Exception("Heap Page full");
		}
		this.setSlotOccupied(unoccupiedSlot, true);
		this.tuples[unoccupiedSlot] = t;
		t.setPid(this.id);
//		for(int i = 0; i < this.getNumSlots(); i++) {
//			if(!this.slotOccupied(i)) {
//				this.setSlotOccupied(i, true);
//				this.tuples[i] = t;
//				t.setPid(this.id);
//			}
//		}
	}

	/**
	 * Removes the given Tuple from the page. If the page id from the tuple does not match this page, throw
	 * an exception. If the tuple slot is already empty, throw an exception
	 * @param t the tuple to be deleted
	 * @throws Exception
	 */
	public void deleteTuple(Tuple t) throws Exception{
		TupleDesc td = t.getDesc();
		if(t.getPid() != this.id) {
			throw new Exception("Tuple page id does not match the heap page's id");
		}
		
		for(int i = 0; i < this.getNumSlots(); i++) {
			if(td.equals(this.tuples[i].getDesc())) {
				if(!this.slotOccupied(i)) {
					throw new Exception("Tuple slot is already empty");
				}
				this.setSlotOccupied(i, false);
				this.tuples[i] = null;
				return;
			}
		}
	}
	
	/**
     * Suck up tuples from the source file.
     */
	private Tuple readNextTuple(DataInputStream dis, int slotId) {
		// if associated bit is not set, read forward to the next tuple, and
		// return null.
		if (!slotOccupied(slotId)) {
			for (int i=0; i<td.getSize(); i++) {
				try {
					dis.readByte();
				} catch (IOException e) {
					throw new NoSuchElementException("error reading empty tuple");
				}
			}
			return null;
		}

		// read fields in the tuple
		Tuple t = new Tuple(td);
		t.setPid(this.id);
		t.setId(slotId);

		for (int j=0; j<td.numFields(); j++) {
			if(td.getType(j) == Type.INT) {
				byte[] field = new byte[4];
				try {
					dis.read(field);
					t.setField(j, new IntField(field));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				byte[] field = new byte[129];
				try {
					dis.read(field);
					t.setField(j, new StringField(field));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}


		return t;
	}

	/**
     * Generates a byte array representing the contents of this page.
     * Used to serialize this page to disk.
	 *
     * The invariant here is that it should be possible to pass the byte
     * array generated by getPageData to the HeapPage constructor and
     * have it produce an identical HeapPage object.
     *
     * @return A byte array correspond to the bytes of this page.
     */
	public byte[] getPageData() {
		int len = HeapFile.PAGE_SIZE;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
		DataOutputStream dos = new DataOutputStream(baos);

		// create the header of the page
		for (int i=0; i<header.length; i++) {
			try {
				dos.writeByte(header[i]);
			} catch (IOException e) {
				// this really shouldn't happen
				e.printStackTrace();
			}
		}

		// create the tuples
		for (int i=0; i<tuples.length; i++) {

			// empty slot
			if (!slotOccupied(i)) {
				for (int j=0; j<td.getSize(); j++) {
					try {
						dos.writeByte(0);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
				continue;
			}
			
			// non-empty slot
			for (int j=0; j<td.numFields(); j++) {
				Field f = tuples[i].getField(j);
				try {
					dos.write(f.toByteArray());

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// padding
		int zerolen = HeapFile.PAGE_SIZE - (header.length + td.getSize() * tuples.length); //- numSlots * td.getSize();
		byte[] zeroes = new byte[zerolen];
		try {
			dos.write(zeroes, 0, zerolen);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return baos.toByteArray();
	}

	/**
	 * Returns an iterator that can be used to access all tuples on this page. 
	 * @return
	 */
	public Iterator<Tuple> iterator() {
		ArrayList<Tuple> iteratorList = new ArrayList<Tuple>();
		for(int i = 0; i < this.tuples.length; i++) {
			if (this.tuples[i] != null) {
				iteratorList.add(this.tuples[i]);
			}
		}
		return iteratorList.iterator();
	}
}

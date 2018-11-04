/**
 * Contains the individual cache block with its valid and dirty bit
 * @author Shekhar
 *
 */
public class CacheBlock {
	
	private boolean valid;
	private boolean dirty;
	private boolean exclusive;
	private int tag;	
	
	public CacheBlock(boolean v, boolean d, boolean e,  int t) {
		valid = v;
		dirty = d;
		exclusive = e;
		tag = t;
	}
	
	public boolean getValidBit() {
		return valid;
	}
	
	public boolean getDirtyBit() {
		return dirty;
	}
	
	public boolean getExclusiveBit() {
		return exclusive;
	}
	
	public int getTag() {
		return tag;
	}
	
	public void setValidBit(boolean v) {
		valid = v;
	}
	
	public void setDirtyBit(boolean d) {
		dirty = d;
	}
	
	public void setExclusiveBit(boolean e) {
		exclusive = e;
	}
	
	public void setTag(int t) {
		tag = t;
	}
	
	public String toString() {
		return "[" + valid + " " + dirty + " " + exclusive + " " + tag + "]";
	}
	
}

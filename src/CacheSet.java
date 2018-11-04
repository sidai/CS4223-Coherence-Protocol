import java.util.Map;

/**
 * Data structure to represent the cache row, consisting of a cache blocks
 * (number based on associativity) and LRU bits
 * 
 * @author Shekhar
 *
 */
public class CacheSet {

	private LRUList<Integer, CacheBlock> blocks;

	public CacheSet(int associativity) {
		blocks = new LRUList<Integer, CacheBlock>(associativity);
	}

	public CacheBlock getBlockForTag(int tag) {
		CacheBlock found = blocks.get(tag);
		if (found == null) {
			return null;
		} else {
			return found;
		}
	}
	
	/**
	 * Allows to remove block from cache set
	 * @param block
	 */
	public void remove(CacheBlock block) {
		blocks.remove(block.getTag());
	}

	public void installBlock(CacheBlock newBlock) {
		blocks.put(newBlock.getTag(), newBlock);
	}

	public String toString() {
		String out = "{";

		for (Map.Entry<Integer, CacheBlock> e : blocks.getAll()) {
			out += e.getValue().toString();
		}

		out += " }";

		return out;
	}
}

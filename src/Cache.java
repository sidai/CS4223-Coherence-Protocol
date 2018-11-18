import javafx.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Cache {
	private final int size;
	private final List<CacheSet> lines;
	private final int offsetBits;
	private final int indexBits;

	public Cache(int cacheSize, int blockSize, int associativity, String defaultState) {
		size = cacheSize / (blockSize * associativity);
		offsetBits = (int) (Math.log(blockSize) / Math.log(2));
		indexBits = (int) (Math.log(size) / Math.log(2));
		lines = new ArrayList<>(size);
		for (int i = 0; i < size; ++i) {
			lines.add(new CacheSet(associativity, defaultState));
		}
	}

	public int contains(int address) {
		return lines.get(getIndexBits(address)).contains(getTagBits(address));
	}

	public Pair<String, Integer> add(int address, String state) {
		Pair<CacheLine, Integer> removedBlock = lines.get(getIndexBits(address)).add(new CacheLine(state, getTagBits(address)));
		String removedState = removedBlock.getKey().getState();
		int removedBlockIdentifier = getBlockIdentifier(removedBlock.getKey().getTag(), removedBlock.getValue());

		return new Pair<>(removedState, removedBlockIdentifier);
	}

	public String getState(int address, int index) {
		return lines.get(getIndexBits(address)).getState(index);
	}
	
	public void update(int address, int index, String state) {
		lines.get(getIndexBits(address)).update(index, state);
	}

	public int getIndexBits(int address) {
		int up = address << (32 - (indexBits + offsetBits));  //remove tag bits
		return up >>> 32 - indexBits;        //remove indexBits with pending 0 due to removal of tag bits. 
	}

	public int getTagBits(int address) {
		return address >>> indexBits + offsetBits;
	}

	public int getBlockIdentifier(int tag, int index) {
		return ((tag << indexBits) + index) << offsetBits;
	}
}

class CacheSet {
	private final int size;
	private final LinkedList<Integer> LRU;
	private final List<CacheLine> block;

	public CacheSet(int s, String defaultState) {
		size = s;
		LRU = new LinkedList<>();
		block = new ArrayList<>(size);
		for (int i = 0; i < size; ++i) {
			LRU.add(i);
			block.add(new CacheLine(defaultState, 0));
		}
	}

	public int contains(int tag) {
		int index = -1;
		for (int i = 0; i < size; ++i) {
			if (block.get(i).getTag() == tag) {
				index = i;
				break;
			}
		}
		return index;
	}

	public Pair<CacheLine, Integer> add(CacheLine l) {
		int index = -1;
		for (int j = 0; j < size; ++j) {
			if (block.get(j).getState() == "I") {
				index = j;
				break;
			}
		}
		if (index >= 0) LRU.removeFirstOccurrence(index);
		else index = LRU.removeLast();
		CacheLine removed = block.get(index);
		block.set(index, l);
		LRU.addFirst(index);
		return new Pair<>(removed, index);
	}

	public String getState(int index) {
		return block.get(index).getState();
	}

	public void update(int index, String state) {
		LRU.removeFirstOccurrence(index);
		LRU.addFirst(index);
		block.get(index).setState(state);
	}
}

class CacheLine {
	private String state;
	private int tag;

	public CacheLine(String state, int tag) {
		this.state = state;
		this.tag = tag;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}
}
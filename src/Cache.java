import java.util.ArrayList;

enum CacheState {
	MODIFIED(0), EXCLUSIVE(1), SHARED(2), INVALID(3);

	private int type;

	private CacheState(int input) {
		type = input;
	}

	public int getType() {
		return type;
	}
}

enum BusState {
	NONE(0), BUSRD(4), BUSRDX(5), BUSUPGR(7);

	private int type;

	private BusState(int input) {
		type = input;
	}

	public int getType() {
		return type;
	}
}

enum Action {
	READ(0), WRITE(1), BUS_RD(2), BUS_RDX(3), READ_E(4), BUS_UPGR(5);

	private int type;

	private Action(int input) {
		type = input;
	}

	public int getType() {
		return type;
	}

}

public class Cache {

	// Protocol constants
	private static final String PROTOCOL_MESI = "MESI";
	private static final String PROTOCOL_MSI = "MSI";
	private static final int WORD_SIZE = 2;

	private ArrayList<CacheSet> dataCache;
	private int offsetBits;
	private int indexBits;
	private int tagBits;
	public int counter;
	private String protocol;

	/**
	 * Initializes the cache
	 *
	 * @param protocol
	 * @param cache_size
	 * @param block_size
	 * @param associativity
	 */
	public Cache(String protocol, int cacheSize, int blockSize,
			int associativity) {
		this.dataCache = new ArrayList<CacheSet>();

		// Calculate number of sets
		int numSets = cacheSize / (associativity * blockSize);

		// Create the cache structure
		for (int i = 0; i < numSets; i++) {
			dataCache.add(new CacheSet(associativity));
		}

		calculateBitLengths(blockSize, numSets);

		this.protocol = protocol;
		this.counter = 0;
	}

	/**
	 * Calculate bit lengths of tag, index and offset
	 *
	 * @param blockSize
	 * @param numSets
	 */
	private void calculateBitLengths(int blockSize, int numSets) {
		// Set bit lengths
		this.indexBits = (int) (Math.log(numSets) / Math.log(2));
		this.offsetBits = (int) (Math.log(blockSize / WORD_SIZE) / Math.log(2));
		this.tagBits = 32 - this.indexBits - this.offsetBits;
	}

	/**
	 * Calculates nextState of cache entry and updates cache given actions
	 * 1.read 2.write super.setFlag(1); Please set Flag in case of cache hit
	 *
	 * @param address
	 * @param action
	 * @return bus action that is performed 0=none/flush 1=busRead 2= busReadEx
	 */
	public int getNextBusState(long address, int action) {

		// Init
		BusState busState = BusState.NONE;

		CacheBlock matchingBlock = findHitCacheBlock(address);

		CacheState currentState = null;

		// Calculate current state
		if (matchingBlock != null) {

			// TODO:SET FLAG IF HIT
			if (protocol.equals(PROTOCOL_MSI)) {
				currentState = getCurrentStateMSI(matchingBlock);
			} else if (protocol.equals(PROTOCOL_MESI)) {
				currentState = getCurrentStateMESI(matchingBlock);
			} else {
				System.out.println("Ooops, wrong protocol!");
			}

		} else {
			// Block does not exist in cache
			currentState = CacheState.INVALID;
		}

		// Get next bus state
		if (protocol.equals(PROTOCOL_MSI)) {
			busState = getNextBusStateMSI(currentState, Action.values()[action - 2]);
		} else if (protocol.equals(PROTOCOL_MESI)) {
			busState = getNextBusStateMESI(currentState,
					Action.values()[action - 2]);
		} else {
			System.out.println("Ooops, wrong protocol!");
		}

//		System.out.println("Next BUS State:" + busState.name());

		return busState.getType();
	}

	/**
	 * Finds a matching cache block in the cache
	 * 
	 * @param address
	 * @param action
	 */
	private CacheBlock findHitCacheBlock(long address) {
		int index = getAddressIndexValue(address);
		int tag = getAddressTagValue(address);

		// Get cache block for given address
		CacheBlock matchingBlock = dataCache.get(index).getBlockForTag(tag);
		return matchingBlock;
	}

	/**
	 * Moves the given cache block to the next state
	 * 
	 * @param address
	 * @param action
	 */
	public void updateToNextState(long address, int action) {

		int index = getAddressIndexValue(address);
		int tag = getAddressTagValue(address);

		
		// Get cache block for given address
		CacheBlock matchingBlock = dataCache.get(index).getBlockForTag(tag);

		CacheState currentState = null;
		CacheState nextState = null;

		// Calculate current state
		if (matchingBlock != null) {

			// TODO:SET FLAG IF HIT
			if (protocol.equals(PROTOCOL_MSI)) {
				currentState = getCurrentStateMSI(matchingBlock);
			} else if (protocol.equals(PROTOCOL_MESI)) {
				currentState = getCurrentStateMESI(matchingBlock);
			} else {
				System.out.println("Ooops, wrong protocol!");
			}
		} else {
			// Block does not exist in cache
			currentState = CacheState.INVALID;
		}

		// Get next cache state
		if (protocol.equals(PROTOCOL_MSI)) {
			nextState = getNextStateMSI(currentState, Action.values()[action - 2]);
		} else if (protocol.equals(PROTOCOL_MESI)) {
			nextState = getNextStateMESI(currentState, Action.values()[action - 2]);
		} else {
			System.out.println("Ooops, wrong protocol!");
		}
		
		updateCacheBlock(matchingBlock, index, tag, nextState);
	}

	/**
	 * Update the cache
	 * 
	 * @param address
	 * @param action
	 */
	private void updateCacheBlock(CacheBlock block, int index, int tag,
			CacheState next) {
			
		boolean createdBlock = false;

		if (block == null) {
			block = new CacheBlock(false, false, false, tag);
			createdBlock = true;
		}
		
		//set bits based on next state
		switch (next) {
		case MODIFIED:
			block.setValidBit(true);
			block.setDirtyBit(true);
			block.setExclusiveBit(false);
			break;
		case EXCLUSIVE:
			block.setValidBit(true);
			block.setDirtyBit(false);
			block.setExclusiveBit(true);
			break;
		case SHARED:
			block.setValidBit(true);
			block.setDirtyBit(false);
			block.setExclusiveBit(false);
			break;
		case INVALID:
			dataCache.get(index).remove(block);
			break;
		}

		if(createdBlock && next != CacheState.INVALID) {
			dataCache.get(index).installBlock(block);
			this.counter++;
		}
	}

	/**
	 * Get the next state of the bus for the MSI protocol
	 * 
	 * @param current
	 * @param action
	 * @return new BusState
	 */
	private BusState getNextBusStateMSI(CacheState current, Action action) {
		switch (current) {
		case MODIFIED:
			switch (action) {
			case READ:
				return BusState.NONE;
			case WRITE:
				return BusState.NONE;
			case BUS_RD:
				return BusState.NONE;// Need to flush
			case BUS_RDX:
				return BusState.NONE;// Need to flush
			}
		case SHARED:
			switch (action) {
			case READ:
				return BusState.NONE;
			case WRITE:
				return BusState.BUSRDX;// Need to send BusRd_Ex
			case BUS_RD:
				return BusState.NONE;
			case BUS_RDX:
				return BusState.NONE;// Need to flush
			}
		case INVALID:
			switch (action) {
			case READ:
				return BusState.BUSRD;
			case WRITE:
				return BusState.BUSRDX;
			case BUS_RD:
				return BusState.NONE;
			case BUS_RDX:
				return BusState.NONE;
			}
		}

		return null;
	}

	/**
	 * Get the next state of the bus for the MESI protocol
	 * 
	 * @param current
	 * @param action
	 * @return new BusState
	 */
	private BusState getNextBusStateMESI(CacheState current, Action action) {
		switch (current) {
		case MODIFIED:
			switch (action) {
			case READ:
				return BusState.NONE;
			case READ_E:
				return BusState.NONE;
			case WRITE:
				return BusState.NONE;
			case BUS_RD:
				return BusState.NONE;// Need to flush
			case BUS_RDX:
				return BusState.NONE;// Need to flushs	
			}
		case SHARED:
			switch (action) {
			case READ:
				return BusState.NONE;
			case READ_E:
				return BusState.NONE;
			case WRITE:
				return BusState.BUSUPGR;// Send BUS UPGRD
			case BUS_RD:
				return BusState.NONE;
			case BUS_RDX:
				return BusState.NONE;// Need to flush
			}
		case EXCLUSIVE:
			switch (action) {
			case READ:
				return BusState.NONE;
			case READ_E:
				return BusState.NONE;
			case WRITE:
				return BusState.NONE;// Need to send BusRd_Ex
			case BUS_RD:
				return BusState.NONE;
			case BUS_RDX:
				return BusState.NONE;// Need to flush
			}
		case INVALID:
			switch (action) {
			case READ:
				return BusState.BUSRD;
			case READ_E:
				return BusState.BUSRD;
			case WRITE:
				return BusState.BUSRDX;
			case BUS_RD:
				return BusState.NONE;
			case BUS_RDX:
				return BusState.NONE;
			}
		}

		return null;
	}

	/**
	 * Return next state for MSI
	 * 
	 * @param current
	 * @param action
	 * @return
	 */
	private CacheState getNextStateMSI(CacheState current, Action action) {
		switch (current) {
		case MODIFIED:
			switch (action) {
			case READ:
				return CacheState.MODIFIED;
			case WRITE:
				return CacheState.MODIFIED;
			case BUS_RD:
				return CacheState.SHARED;// Need to flush
			case BUS_RDX:
				return CacheState.INVALID;// Need to flush
			}
		case SHARED:
			switch (action) {
			case READ:
				return CacheState.SHARED;
			case WRITE:
				return CacheState.MODIFIED;// Need to send BusRd_Ex
			case BUS_RD:
				return CacheState.SHARED;
			case BUS_RDX:
				return CacheState.INVALID;// Need to flush
			}
		case INVALID:
			switch (action) {
			case READ:
				return CacheState.SHARED;
			case WRITE:
				return CacheState.MODIFIED;
			case BUS_RD:
				return CacheState.INVALID;
			case BUS_RDX:
				return CacheState.INVALID;
			}
		}

		return null;
	}

	/**
	 * Return next state for MESI
	 * 
	 * @param current
	 * @param action
	 * @return
	 */
	private CacheState getNextStateMESI(CacheState current, Action action) {
		switch (current) {
		case MODIFIED:
			switch (action) {
			case READ:
				return CacheState.MODIFIED;
			case READ_E:
				return CacheState.MODIFIED;
			case WRITE:
				return CacheState.MODIFIED;
			case BUS_RD:
				return CacheState.SHARED;// Need to flush
			case BUS_RDX:
				return CacheState.INVALID;// Need to flush
			case BUS_UPGR:
				return CacheState.INVALID;
			}
		case EXCLUSIVE:
			switch (action) {
			case READ:
				return CacheState.EXCLUSIVE;
			case READ_E:
				return CacheState.EXCLUSIVE;
			case WRITE:
				return CacheState.MODIFIED;
			case BUS_RD:
				return CacheState.SHARED;
			case BUS_RDX:
				return CacheState.INVALID;// Need to flush
			case BUS_UPGR:
				return CacheState.INVALID;
			}
		case SHARED:
			switch (action) {
			case READ:
				return CacheState.SHARED;
			case READ_E:
				return CacheState.SHARED;
			case WRITE:
				return CacheState.MODIFIED;// Need to send BusRd_Ex
			case BUS_RD:
				return CacheState.SHARED;
			case BUS_RDX:
				return CacheState.INVALID;// Need to flush
			case BUS_UPGR:
				return CacheState.INVALID;
			}
		case INVALID:
			switch (action) {
			case READ:
				return CacheState.SHARED;
			case READ_E:
				return CacheState.EXCLUSIVE;
			case WRITE:
				return CacheState.MODIFIED;
			case BUS_RD:
				return CacheState.INVALID;
			case BUS_RDX:
				return CacheState.INVALID;
			case BUS_UPGR:
				return CacheState.INVALID;
			}
		}

		return null;
	}

	/**
	 * Current state based on MSI valid and dirty bit
	 * 
	 * @param matchingBlock
	 * @return current state
	 */
	private CacheState getCurrentStateMSI(CacheBlock matchingBlock) {
		CacheState current_state;

		if (matchingBlock.getValidBit() && matchingBlock.getDirtyBit()) {
			current_state = CacheState.MODIFIED;
		} else if (matchingBlock.getValidBit() && !matchingBlock.getDirtyBit()) {
			current_state = CacheState.SHARED;
		} else {
			current_state = CacheState.INVALID;
		}

		return current_state;
	}

	/**
	 * Current state based on MESI valid and dirty bit
	 * 
	 * @param matchingBlock
	 * @return current state
	 */
	private CacheState getCurrentStateMESI(CacheBlock matchingBlock) {
		CacheState current_state;

		if (matchingBlock.getValidBit() && !matchingBlock.getDirtyBit()
				&& matchingBlock.getExclusiveBit()) {
			current_state = CacheState.EXCLUSIVE;
		} else if (matchingBlock.getValidBit() && matchingBlock.getDirtyBit()
				&& !matchingBlock.getExclusiveBit()) {
			current_state = CacheState.MODIFIED;
		} else if (matchingBlock.getValidBit() && !matchingBlock.getDirtyBit()
				&& !matchingBlock.getExclusiveBit()) {
			current_state = CacheState.SHARED;
		} else {
			current_state = CacheState.INVALID;
		}

		return current_state;
	}

	/**
	 * Get tag value
	 * 
	 * @param address
	 * @return tag value
	 */
	private int getAddressTagValue(long address) {
		// Create mask for getting index bits
		long cacheTag = address >> (offsetBits + indexBits);
		return (int) cacheTag;
	}

	/**
	 * Returns the cache block index with the matching tag
	 *
	 * @param address
	 * @return matching Cache Block or nil if no match
	 */
	private int getAddressIndexValue(long address) {
		// Create mask for getting index bits
		int mask = (int) Math.pow(2, indexBits) - 1;
		long cacheIndex = (address >> offsetBits) & mask;
		return (int) cacheIndex;
	}

	/**
	 * Function that checks if there is a valid value in this cache for given
	 * address
	 * 
	 * @param address
	 *            of memory location
	 * @return true if hit, false if miss
	 */
	public boolean isHit(long address) { // TODO

		CacheState currentState = null;

		// Get cache block for given address
		CacheBlock matchingBlock = findHitCacheBlock(address);

		// Calculate current state
		if (matchingBlock != null) {

			if (protocol.equals(PROTOCOL_MSI)) {
				currentState = getCurrentStateMSI(matchingBlock);
			} else if (protocol.equals(PROTOCOL_MESI)) {
				currentState = getCurrentStateMESI(matchingBlock);
			} else {
				System.out.println("Ooops, wrong protocol!");
			}

		} else {
			// Block does not exist in cache
			currentState = CacheState.INVALID;
		}

		// hit miss check
		if (currentState == CacheState.INVALID) {
			return false;
		} else {
			return true;
		}

	}

	public String toString() {
		String out = "------\n";

		for (CacheSet set : dataCache) {
			out += set.toString() + "\n";
		}

		out += "------";

		return out;
	}

}

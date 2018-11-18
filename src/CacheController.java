public abstract class CacheController {
	protected Cache cache;
	protected Processor processor;
	protected int hit;
	protected int miss;
	protected int privateAccess;
	protected int publicAccess;

	public CacheController(Processor processor, int cacheSize, int associativity, int blockSize) throws Exception {
		this.processor = processor;
		cache = new Cache(cacheSize, blockSize, associativity, "I");
		hit = 0;
		miss = 0;
		privateAccess = 0;
		publicAccess = 0;
	}

	public abstract void prRd(int address);
	public abstract void prWr(int address);
	public abstract void unstall(Transaction t);
	public abstract boolean snoop(Transaction t);

	public double getMissRate() {
		return 100.0 * miss / getTotalMemOp();
	}

	public int getTotalMemOp() {
		return hit + miss;
	}

	public int getHit() {
		return hit;
	}
	
	public int getMiss() {
		return miss;
	}

	public int getTotalMemAccess() {
		return privateAccess + publicAccess;
	}

	public double getPrivatePercentage() {
		return 100.0 * privateAccess / getTotalMemAccess();
	}

	public double getPublicPercentage() {
		return 100.0 * publicAccess / getTotalMemAccess();
	}
}

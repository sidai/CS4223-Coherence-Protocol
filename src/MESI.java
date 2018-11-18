import javafx.util.Pair;

public class MESI extends CacheController {
	public MESI(Processor processor, int cacheSize, int associativity, int blockSize) throws Exception {
		super(processor, cacheSize, associativity, blockSize);
	}

	@Override
	//Bus complete the transaction, continue to keep state of cache line updated.
	//Possible transaction: BusRd, BusRdX, BusUpgr, WB
	public void unstall(Transaction t) {
		Transaction.Type type = t.getType();
		// no status update required for WB action
		// for BusUpgr, we have preset the state to 'M' before snoop the bus, as BusUpgr won't
		// evict any cache line, no further processing here.
		if (Transaction.Type.WB.equals(type) || Transaction.Type.BusUpgr.equals(type)) {
			return;
		}

		String unstallState = "";
		if (Transaction.Type.BusRd.equals(type)) {
			unstallState = t.isShared() ? "S" : "E";
		} else if (Transaction.Type.BusRdX.equals(type)){
			unstallState = "M";
		}

		int index = cache.contains(t.getAddress());
		if (index >= 0) {
			cache.update(t.getAddress(), index, unstallState);
		} else {
			Pair<String, Integer> removedBlock = cache.add(t.getAddress(), unstallState);
			// need to write back to main memory due to evicted "M" cache line
			if (removedBlock.getKey() == "M") {
				processor.addTransaction(Transaction.Type.WB, removedBlock.getValue());
			}
		}
	}

	@Override
	//Snoop transaction request from bus.
	//Possible transaction: BusRd, BusRdX, BusUpgr(only to invalidate shared copy)
	public boolean snoop(Transaction t) {
		boolean canProvide = false;
		int index = cache.contains(t.getAddress());
		if (index >= 0) {
			switch (cache.getState(t.getAddress(), index)) {
			case "M":
				switch (t.getType()) {
					case BusRd:
						cache.update(t.getAddress(), index, "S");
						//we need to write back the cache line to keep main memory updated
						processor.addTransaction(Transaction.Type.WB, t.getAddress());
						break;
					case BusRdX:
						//for BusRdx, we know that there is still latest copy in cache, we don't need to write back to main memory
						cache.update(t.getAddress(), index, "I");
						break;
					default:
						break;
				}
				canProvide = true;
				break;

			case "E":
				switch (t.getType()) {
				case BusRd:
					cache.update(t.getAddress(), index, "S");
					break;
				case BusRdX:
					cache.update(t.getAddress(), index, "I");
					break;
				default:
					break;
				}
				canProvide = true;
				break;

			case "S":
				switch (t.getType()) {
				case BusRd:
					cache.update(t.getAddress(), index, "S");
					break;
				case BusRdX:
				case BusUpgr:
					cache.update(t.getAddress(), index, "I");
					break;
				default:
					break;
				}
				canProvide = true;
				break;

			case "I": //nothing happens for 'I' cache.
				break;

			default:
				break;
			}
		}
		return canProvide;
	}

	@Override
	public void prRd(int address) {
		int index = cache.contains(address);
		// if contained, check status
		if (index >= 0) {
			switch (cache.getState(address, index)) {
			case "I":
				miss++;
				processor.addTransaction(Transaction.Type.BusRd, address);
				break;

			case "S":
				hit++;
				publicAccess++;
				cache.update(address, index, "S");
				break;

			case "E":
				hit++;
				privateAccess++;
				cache.update(address, index, "E");
				break;

			case "M":
				hit++;
				privateAccess++;
				cache.update(address, index, "M");
				break;

			default:
				break;
			}
		} else { // cold miss
			miss++;
			processor.addTransaction(Transaction.Type.BusRd, address);
		}
	}

	@Override
	public void prWr(int address) {
		int index = cache.contains(address);
		if (index >= 0) {
			switch (cache.getState(address, index)) {
			case "S":
				publicAccess++;
				hit++;
				cache.update(address, index, "M");
				processor.addTransaction(Transaction.Type.BusUpgr, address);
				break;
			case "I":
				miss++;
				processor.addTransaction(Transaction.Type.BusRdX, address);
				break;

			case "M":
			case "E":
				privateAccess++;
				hit++;
				cache.update(address, index, "M");
				break;

			default:
				break;
			}
		} else { // cold miss
			miss++;
			processor.addTransaction(Transaction.Type.BusRdX, address);
		}
	}
}

import javafx.util.Pair;

public class Dragon extends CacheController {
	boolean pendingWriteAction = false;
	public Dragon(Processor processor, int cacheSize, int associativity, int blockSize) throws Exception {
		super(processor, cacheSize, associativity, blockSize);
	}

	@Override
	//Bus complete the transaction, continue to keep state of cache line updated.
	//Possible transaction: BusRd, BusUpd, WB
	public void unstall(Transaction t) {
		Transaction.Type type = t.getType();
		if (Transaction.Type.WB.equals(type)) {
			return;        // no status update required for write back memory action
		}

		String unstallState = "";
		if (Transaction.Type.BusRd.equals(type)) {    //BusRd could be generated due to PrRdMiss or PrWrMiss
			if(pendingWriteAction) {      //due to PrWrMiss, update other processor after read.
				processor.addTransaction(Transaction.Type.BusUpd, t.getAddress());
				pendingWriteAction = false;
				return;
			} else {                      //due to PrRdMiss, update the cache line state.
				unstallState = t.isShared() ? "Sc" : "E";
			}
		} else if (Transaction.Type.BusUpd.equals(type)){
			unstallState = t.isShared() ? "Sm" : "M";
		}

		int index = cache.contains(t.getAddress());
		if (index >= 0) {
			cache.update(t.getAddress(), index, unstallState);
		}
		else {
			Pair<String, Integer> removedBlock = cache.add(t.getAddress(), unstallState);
			// need to write back to main memory due to evicted "M" cache line
			if (removedBlock.getKey() == "M" || removedBlock.getKey() == "Sm") {
				processor.addTransaction(Transaction.Type.WB, removedBlock.getValue());
			}
		}
	}

	@Override
	//Snoop transaction request from bus.
	//Possible transaction: BusRd, BusUpd(only for shared copy to update)
	public boolean snoop(Transaction t) {
		boolean shared = false;
		int index = cache.contains(t.getAddress());
		if (index >= 0) {
			switch (cache.getState(t.getAddress(), index)) {
			case "E":
				if (t.getType() == Transaction.Type.BusRd)
					cache.update(t.getAddress(), index, "Sc");
				shared = true;
				break;

			case "Sc":
				if (t.getType() == Transaction.Type.BusRd)
					cache.update(t.getAddress(), index, "Sc");
				if (t.getType() == Transaction.Type.BusUpd)
					cache.update(t.getAddress(), index, "Sc");
				shared = true;
				break;

			case "Sm":
				switch (t.getType()) {
				case BusRd:
					cache.update(t.getAddress(), index, "Sm");
					break;
				case BusUpd:
					cache.update(t.getAddress(), index, "Sc");
					break;

				default:
					break;
				}
				shared = true;
				break;

			case "M":
				if (t.getType() == Transaction.Type.BusRd) {
					cache.update(t.getAddress(), index, "Sm");
				}
				shared = true;
				break;
			default:
				break;
			}
		}
		return shared;
	}

	@Override
	public void prRd(int address) {
		int index = cache.contains(address);
		if (index >= 0) {
			switch (cache.getState(address, index)) {
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
			case "Sc":
				hit++;
				publicAccess++;
				cache.update(address, index, "Sc");
				break;
			case "Sm":
				hit++;
				publicAccess++;
				cache.update(address, index, "Sm");
				break;

			default:
				break;
			}
		} else {
			miss++;
			//PrRdMiss, need to read from bus.
			processor.addTransaction(Transaction.Type.BusRd, address);
		}
	}

	@Override
	public void prWr(int address) {
		int index = cache.contains(address);
		if (index >= 0) {
			switch (cache.getState(address, index)) {
			case "Sc":
			case "Sm":
				publicAccess++;
				hit++;
				//PrWr hit in shared cache, need to update other shared copy.
				processor.addTransaction(Transaction.Type.BusUpd, address);
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
		} else {
			miss++;
			//PrWrMiss, need to load from bus then write the value after loading back the result.
			processor.addTransaction(Transaction.Type.BusRd, address);
			pendingWriteAction = true;
		}
	}
}

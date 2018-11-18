import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Bus {
	//parameter for statistic record.
	private static final int MISS = 100;
	private static final int WB = 100;
	private static final int UPDATE = 2;
	private static int BLOCKSIZE;
	private static final int WORDSIZE = 4;
	private long dataTraffic;
	private int invOrUpd;
	private int wb;

	private Queue<Transaction> transactionQueue;
	private List<Processor> snoopers;
	private Transaction next;
	private int waitCounter;

	public Bus(int blockSize) {
		transactionQueue = new LinkedList<>();
		BLOCKSIZE = blockSize;
		snoopers = new ArrayList<>();
		waitCounter = 0;
		dataTraffic = 0;
		invOrUpd = 0;
		wb = 0;
	}

	public void addTransaction(Transaction t) {
		transactionQueue.add(t);
	}

	public void nextTick() {
		if (waitCounter > 1) {
			waitCounter--;
			if(waitCounter == 0) {
                snoopers.get(next.getId()).unstall(next);
            }
			return;
		}

		next = transactionQueue.poll();
		if (next != null) {
			process(next);
		}
	}

	private void process(Transaction t) {
        boolean canProvide = false;
		int id = t.getId();
		for (int i = 0; i < snoopers.size(); ++i) {
			//snoop all other processors and check if anyone has a last valid copy.
			//only load from memory if none of them has valid copy
            if (i != id && snoopers.get(i).snoop(t)) {
                canProvide = true;
            }
        }

		switch (t.getType()) {
		case BusRd:
			if (canProvide) {
                waitCounter = 2 * BLOCKSIZE / WORDSIZE;
                next.setShared(true);      //indicate other processes has a valid copy after current transaction
            } else {
                waitCounter = MISS;
            }
			dataTraffic += BLOCKSIZE;
			break;
		case BusRdX:
			if (canProvide)
				waitCounter = 2 * BLOCKSIZE / WORDSIZE;
			else
				waitCounter = MISS;
			dataTraffic += BLOCKSIZE;
			invOrUpd++;
			break;

		case BusUpgr:
			invOrUpd++;
			break;

		case WB:
			waitCounter = WB;
			dataTraffic += BLOCKSIZE;
			wb++;
			break;

		case BusUpd:
			if (canProvide) {
				next.setShared(true);     //indicate other processes has a valid copy after current transaction
			}
			waitCounter = UPDATE;
			dataTraffic += WORDSIZE;
			invOrUpd++;
			break;
		default:
			break;
		}
	}

	public void addProcessor(List<Processor> processors) {
		snoopers.addAll(processors);
	}

	public void summary() {
		System.out.println("----------------------------------------Bus Summary--------------------------------------");
		System.out.println("#DataTraffic : " + dataTraffic + " bytes");
		System.out.println("#Invalidations or Update : " + invOrUpd);
		System.out.println("#Write Back : " + wb);
	}
}

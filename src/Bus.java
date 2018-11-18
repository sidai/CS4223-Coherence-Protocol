import java.io.PrintWriter;
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
//		System.out.println("Bus received: " + t.getId() + ", " + t.getType() + ", " + t.getAddress());
	}

	public void nextTick() {
		if (waitCounter > 0) {
			waitCounter--;
			if(waitCounter == 0) {
                snoopers.get(next.getId()).unstall(next);
//				System.out.println("unstall: " + next.getId() + ", " + next.getType() + ", " + next.getAddress());
            }
			return;
		}

		next = transactionQueue.poll();
		if (next != null) {
//			System.out.println("process: " + next.getId() + ", " + next.getType() + ", " + next.getAddress());
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

	public void summary(PrintWriter pr) {
		pr.println("----------------------------------------Bus Summary--------------------------------------");
		pr.println("#DataTraffic : " + dataTraffic + " bytes");
		pr.println("#Invalidations or Update : " + invOrUpd);
		pr.println("#Write Back : " + wb);
		pr.println();
	}
}

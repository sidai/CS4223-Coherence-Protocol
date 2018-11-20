import java.io.PrintWriter;
import java.text.DecimalFormat;

public class Processor {
	private TraceReader trace;
	private Bus bus;
	private CacheController controller;
	private boolean stall, done;
	private int counter;
	private int cycle;

	private int computeCycle;
	private int idleCycle;
	private int id;

	public Processor(int id, String protocol, String file, int cacheSize,
					 int associativity, int blockSize, Bus bus) throws Exception {
		if("MESI".equalsIgnoreCase(protocol)) {
			initProcessor(id, file, new MESI(this, cacheSize, associativity, blockSize), bus);
		} else if("DRAGON".equalsIgnoreCase(protocol)) {
			initProcessor(id, file, new Dragon(this, cacheSize, associativity, blockSize), bus);
		} else if("MOESI".equalsIgnoreCase(protocol)) {
			initProcessor(id, file, new MOESI(this, cacheSize, associativity, blockSize), bus);
		}
	}

	public void initProcessor(int id, String file, CacheController controller, Bus bus) throws Exception {
		trace = new TraceReader(file + "_" + id + ".data");
		cycle = 0;
		this.controller = controller;
		stall = false;
		done = false;
		counter = 0;
		this.id = id;
		this.bus = bus;
	}

	public boolean done() {
		return done;
	}

	public void nextTick() {
		if (done) {
			return;
		}
		cycle++;
		if (!stall && counter == 0) {
			Command c = trace.getNextCommand();
			int value = c.getValue();
			switch (c.getType()) {
				case LOAD:
					controller.prRd(value);
					break;
				case STORE:
					controller.prWr(value);
					break;
				case COMPUTE:
					computeCycle += c.getValue();
					counter = c.getValue();
					break;
				case EOF:
					done = true;
					break;
				default:
					break;
			}
		} else if (!stall && counter > 0) {
			counter--;
		} else {
			idleCycle++;      //the process is idle for I/O operation
		}
	}

	public int getComputeCycle() {
		return computeCycle;
	}

	public int getIdleCycle() {
		return idleCycle;
	}

	public int getId() {
		return id;
	}

	public void close() {
		trace.close();
	}


	public void addTransaction(Transaction.Type type, int address) {
//		System.out.println("add: " + id + ", " + type + ", " + address);
		if (type.equals(Transaction.Type.BusRd) || type.equals(Transaction.Type.BusRdX)) {
			bus.addTransaction(new Transaction(type, id, address));
			stall = true;
		} else if (type.equals(Transaction.Type.BusUpgr) || type.equals(Transaction.Type.BusUpd)
				|| type.equals(Transaction.Type.WB)) {
			bus.addTransaction(new Transaction(type, id, address));
			//don't stall for upgrade & wb process.
		}
	}

	public boolean snoop(Transaction transaction) {
//		System.out.println("snoop: " + transaction.getId() + ", " + transaction.getType() + ", " + transaction.getAddress());
		return controller.snoop(transaction);
	}

	public void unstall(Transaction transaction) {
//		System.out.println("unstall: " + transaction.getId() + ", " + transaction.getType() + ", " + transaction.getAddress());
		stall = false;    //unstall the process if it is stalled due to I/O as I/O transaction completed.
		controller.unstall(transaction);
	}

	public void summary(PrintWriter pr) {
		pr.println("----------------------------------------Processor #" + id + " Summary--------------------------------------");
		pr.println("#Total cycles: " + cycle + ", #Compute cycles: " + computeCycle + ", #Idle cycles: " + idleCycle);

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		pr.println("#Line of Code: " + trace.getLineOfCode() + ", #Loads: " + trace.getNumOfLoad() + ", #Stores: "
				   + trace.getNumOfStore() + ", #Computes: " + trace.getNumOfCompute());
		pr.println("#Loads & Stores: " + controller.getTotalMemOp() + ", #Hits: " + controller.getHit() +
		           ", #Misses: " + controller.getMiss() + ", %Cache Miss Rate: " + df.format(controller.getMissRate()));
		pr.println("#Accesses: " + controller.getTotalMemAccess() + ", %Public accesses: " + df.format(controller.getPublicPercentage()) +
		           ", #Private Accesses: " + df.format(controller.getPrivatePercentage()));
		pr.println();
	}

	public void shortSummary(PrintWriter pr) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		pr.print(cycle + "," + computeCycle + "," + controller.getTotalMemOp() + "," + idleCycle + "," + df.format(controller.getMissRate()) + "," +
				controller.getTotalMemAccess() + "," + df.format(controller.getPublicPercentage()) + "," + df.format(controller.getPrivatePercentage()));
	}
}

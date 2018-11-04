import java.io.IOException;
import java.util.LinkedList;

public class TraceReader {
	private static String path = "/home/user/git/Cache-Simulator/";
	private static int blockTime;
	private static int coreCount;
	public static int done = 0;
	static int accesses = 0;
	static int busAccesses = 0;

	/*
	 * Actions: 0: Fetch Instruction 1: Nothing 2: PrRead 3: PrWrite 4: BusRead
	 * 5: BusReadEx 6: PrRead(Exclusive) (Just for special case PrRead from
	 * invalid)
	 */
	public static void main(String[] args) throws IOException {
		
		long start = System.nanoTime();
		System.out.println("Protocol: " + args[0]);
		System.out.println("Benchmark: " + args[1]);
		System.out.println("Cores: " + args[2]);
		System.out.println("Cache Size: " + args[3]);
		System.out.println("Associativity: " + args[4]);
		System.out.println("Block Size: " + args[5]);
		coreCount = Integer.parseInt(args[2]); // number of cores
		Processor[] processorArray = new Processor[coreCount];
		// Initialize cores
		String coreString;
		switch (coreCount) {
		case 1:
			coreString = "Unicore/";
			break;
		case 2:
			coreString = "Dualcore/";
			break;
		case 4:
			coreString = "Quadcore/";
			break;
		case 8:
			coreString = "OctaCore/";
			break;
		default:
			System.out.println("Invalid number of cores!");
			return;
		}
		// arguments: protocol(0), benchmark(1), cores(2), cache size(3),
		// associativity(4), block size(5)
		for (int i = 0; i < coreCount; i++) {
			processorArray[i] = new Processor(path + coreString + args[1]
					+ (i + 1) + ".prg", i, args[0], Integer.parseInt(args[3]),
					Integer.parseInt(args[5]), Integer.parseInt(args[4]));
		}

		String s;
		String[] split = new String[2];
		long address;
		long cycles = 0;
		long busCount = 0;
		int busNotUsed = 0;
		int action;
		int busAction;
		int blockSize = Integer.parseInt(args[5]);
		Processor p;
		Quadrupel q = null;
		LinkedList<Quadrupel> busList = new LinkedList<Quadrupel>();
		
		while (true) {
			
			for (int j = 0; j < coreCount; j++) {
				if (processorArray[j].done)
					done++;
			}
			if (done == coreCount) {
				System.out.println("---------Time Taken: "
						+ (System.nanoTime() - start) / 1000000000
						+ "s------------");
				printResults(cycles, processorArray, busCount, busNotUsed);
				return;// All traces have been processed
			}
			
			cycles++;

			for (int i = 0; i < coreCount; i++) {// Process cycle operations for
													// all cores
				p = processorArray[i];
				if ((!p.inQueue) || (p.done)) {
					// Processor not blocked by being in BusQueue
					p.cycles++;
					s = p.getCycle();
					split = s.split(" ");
					action = Integer.parseInt(split[0]);
					if (action != 0) { // Action is read or write
						accesses++;
						address = Long.parseLong(split[1], 16);
						//address = address >> 8;
						busAction = p.cache.getNextBusState(address, action);
						if (p.cache.isHit(address)) {
							p.hits++;
						} else {
							p.misses++;
						}
						if (busAction == 0) {
							// Read or Write doesn't require bus
							p.cache.updateToNextState(address, action);
						} else {// Read or write requires bus
							busList.add(new Quadrupel(p, address, busAction,
									action));
							p.inQueue = true;
							busAccesses++;
						}
					}
				}
			}
			// Process bus
			boolean hitFlag = false;
			if (blockTime > 0) { // Bus blocked by data transfer
				blockTime--;
				if (blockTime == 0) {
					q.p.cache.updateToNextState(q.address, q.action);
					q.p.inQueue = false;
				}
			} else {
				q = busList.pollFirst();
				if (q != null) {
					busCount = busCount + blockSize;
					for (int i = 0; i < coreCount; i++) {
						if (i != q.p.id) {
							if (processorArray[i].cache.isHit(q.address)) { // Other cache has needed data												
								blockTime = 1;
								hitFlag = true; // Accessing shared data
								processorArray[i].cache.updateToNextState(
										q.address, q.busAction);
							}
						}
					}
					if (q.busAction == 5) { // BusReadEX
						blockTime = 10;
					} else if (q.busAction == 4) { // BusRead
						if (!hitFlag) {
							blockTime = 10; // No cache has needed data
							if (args[0].equals("MESI"))
								q.action = 6; // Read of exclusive data
						} else {
							blockTime = 1;
						}
					} else {
						System.out.println("Bus action invalid, aborting!");
						return;
					}
					hitFlag = false;
				} else {
					busNotUsed++;
				}

			}
		}
	}

	public static Processor getNextProcessor(Processor[] p, int id) {
		for (int i = id; i < coreCount; i++) {
			if (p[i].done == false) {
				return p[i];
			} else {
				done++;
			}
		}
		return null;
	}

	public static void printResults(long cycles, Processor[] processors,
			long busCount, int busNotUsed) {
		int length = processors.length;
		long misses = 0;
		long hits = 0;
		System.out.println("Cycles taken: " + cycles);
		System.out.println("Bytes transfered on bus: " + busCount);
		System.out.println("Cycles in which bus was not used: " + busNotUsed);
		System.out.println(" ");

		for (int i = 1; i <= length; i++) {
			System.out.println("Number of execution Cycles Processor " + i
					+ ": " + processors[i - 1].cycles);
			System.out.println("Number of cache hits Processor " + i + ": "
					+ processors[i - 1].hits);
			System.out.println("Number of cache misses Processor " + i + ": "
					+ processors[i - 1].misses);
			System.out.println(" ");
			misses = misses + processors[i - 1].misses;
			hits = hits + processors[i - 1].hits;

		}
		System.out
				.println("Miss Rate: " + 100 * misses / (misses + hits) + "%");
		System.out.println("Memory Accesses: " + accesses);
		System.out.println("Bus Accesses: " + busAccesses);

		System.out.println("------------------");
	}
}

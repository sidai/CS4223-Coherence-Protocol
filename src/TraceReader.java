import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TraceReader {
	private final BufferedReader br;
	private final String PATH = "../data/";
	private int lineOfCode;
	private int numOfLoad;
	private int numOfStore;
	private int numOfCompute;


//	public static void main(String[] arg) throws Exception {
//		String fileName = "b_0.data";
//		TraceReader reader = new TraceReader(fileName);
//		Command command = reader.getNextCommand();
//		while(!Command.Type.EOF.equals(command.getType())) {
//			System.out.println(command.getType() + ", " + command.getValue());
//			command = reader.getNextCommand();
//		}
//	}

	public TraceReader(String fileName) throws FileNotFoundException {
		br = new BufferedReader(new FileReader(PATH + fileName));
	}
	
	public Command getNextCommand() {
		try {
			String instruction = br.readLine();
			if (instruction == null) {
				return new Command(3, 0);
			} else {
				String[] splitted = instruction.split(" ");
				record(Integer.parseInt(splitted[0]));
				return new Command(Integer.parseInt(splitted[0]), Integer.decode(splitted[1]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Command(3, 0);
		}

	}

	private void record(int command) {
		lineOfCode++;
		if (command == 0) {
			numOfLoad++;
		} else if (command == 1) {
			numOfStore++;
		} else if (command == 2) {
			numOfCompute++;
		}
	}

	public int getLineOfCode() {
		return lineOfCode;
	}

	public int getNumOfLoad() {
		return numOfLoad;
	}

	public int getNumOfStore() {
		return numOfStore;
	}

	public int getNumOfCompute() {
		return numOfCompute;
	}

	public void close() {
		try {
			br.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
}

class Command {
	private Type type;
	private int value;

	public Command(int type, int value) {
		this.type = Type.fromInt(type);
		this.value = value;
	}

	public Type getType() {
		return type;
	}

	public int getValue() {
		return value;
	}

	enum Type {
		LOAD(0), STORE(1), COMPUTE(2), EOF(3);

		private int type;

		private Type(int input) {
			type = input;
		}

		public int getType() {
			return type;
		}

		public static Type fromInt(int id) {
			return values()[id];
		}
	}
}

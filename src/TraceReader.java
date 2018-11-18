import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TraceReader {
	private final BufferedReader br;
	private final String PATH = "./data/";
	
	public TraceReader(String fileName) throws FileNotFoundException {
		br = new BufferedReader(new FileReader(PATH + fileName));
	}
	
	public Command getNextCommand() {
		try {
			String instruction = br.readLine();
			if (instruction == null) return new Command(3, 0);
			else {
				String[] splitted = instruction.split(" ");
				return new Command(Integer.parseInt(splitted[0]), Integer.parseInt(splitted[1], 16));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Command(3, 0);
		}

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

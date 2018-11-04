import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Processor {
	private String[] s;
	public BufferedReader trace;
	public Cache cache;
	public int hitFlag;
	public boolean inQueue;
	public boolean done;
	public int id;
	public long misses;
	public long hits;
	public long cycles;


	public Processor(String dir, int id, String protocol, int cacheSize,
			int blockSize, int associativity) {
		File file = new File(dir);
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);

			BufferedInputStream bis = new BufferedInputStream(fis);
			this.trace = new BufferedReader(new InputStreamReader(bis));
			this.s = new String[2];
			this.id = id;
			s[0] = this.trace.readLine();
			s[1] = this.trace.readLine();
			this.hitFlag = 0;
			this.inQueue = false;
			this.done = false;
			this.cache = new Cache(protocol, cacheSize, blockSize,
					associativity);
			this.misses = 0;
			this.hits = 0;
			this.cycles = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setFlag(int i) {
		hitFlag = i;
	}

	public int getFlag() {
		return hitFlag;
	}

	public BufferedReader getTrace() {
		return this.trace;
	}

	public String getCycle() {
		String result = null;
		try {
			if (s[1] == null) {
				result = s[0];
				this.done = true;
			} else {
				if (s[0].startsWith("0")
						&& (s[1].startsWith("2") || s[1].startsWith("3"))) {
					result = s[1];
					s[0] = this.trace.readLine();
					s[1] = this.trace.readLine();
				} else {
					result = s[0];
					s[0] = s[1];
					s[1] = this.trace.readLine();
				}
				if (s[0] == null && s[1] == null) {
					this.done = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
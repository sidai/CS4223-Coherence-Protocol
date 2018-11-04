import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class Test {

	public static void main(String[] args) {
		File file = new File("/home/user/git/Cache-Simulator/Unicore/WEATHER1.prg");
		FileInputStream fis;
		ArrayList<String> str = new ArrayList<String>();
		try {
			fis = new FileInputStream(file);

			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedReader trace = new BufferedReader(new InputStreamReader(bis));
			String s;
			long a = Long.MAX_VALUE;
			long z = 0;
			while(trace.ready()){
				s=trace.readLine();
				s=s.split(" ")[1];
				if(Long.parseLong(s, 16)>z){
					z = Long.parseLong(s, 16);
				}
				if(Long.parseLong(s, 16)<a){
					a = Long.parseLong(s, 16);
				}
			}
			System.out.println("Min: " + a);
			System.out.println("Max: " + Long.toHexString(z));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

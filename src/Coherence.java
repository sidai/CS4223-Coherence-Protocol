import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Coherence {
    public static void main(String[] args) throws Exception{
        if (!(args.length == 2 || args.length == 5)) {
            System.err.println("----------------------------------------Wrong Argument--------------------------------------");
            System.err.println("Please follow the format below to type in your command");
            System.err.println("coherence protocol input_file cache_size associativity block_size");
            System.err.println("coherence protocol input_file");
            System.exit(-1);
        }

        String protocol = args[0];
        String inputFile = args[1];
        int cacheSize, associativity, blockSize;
        if (args.length > 2) {
            cacheSize = Integer.parseInt(args[2]);
            associativity = Integer.parseInt(args[3]);
            blockSize = Integer.parseInt(args[4]);
        } else {
            cacheSize = 4096;
            associativity = 2;
            blockSize = 32;
        }

        Bus bus = new Bus(blockSize);
        List<Processor> processors = new ArrayList<>();
        for (int id = 0; id < 4; id++) {
            processors.add(new Processor(id, protocol, inputFile, cacheSize, associativity, blockSize, bus));
        }
        bus.addProcessor(processors);

        // Main Loop
        long cycle = 0;
        while (!checkDone(processors)) {
            cycle++;
            if(cycle % 1000000 == 0) {
                System.out.println("#cycle past: " + cycle);
            }
            for (Processor p: processors) {
                if(!p.done()) {
                    p.nextTick();
                }
            }
            bus.nextTick();
        }

        // Close all input file
        for (Processor p: processors) {
            p.close();
        }

        PrintWriter pr = getPrinter(protocol, inputFile, cacheSize, associativity, blockSize);
        // Record Down All Statistics
        // Run Configuration
        pr.println("----------------------------------------Run Config--------------------------------------");
        pr.println("Protocol: " + protocol + ", Input file: " + inputFile);
        pr.println("Caches size: " + cacheSize + ", associativity: " + associativity + ", block size: " + blockSize);
        pr.println("Overall Execution Time: " + cycle);
        pr.println();

        // Statistic for overall runtime and bus
        bus.summary(pr);

        // Statistic for each process
        for(Processor p: processors) {
            p.summary(pr);
        }

        pr.close();
    }

    private static boolean checkDone(List<Processor> processors) {
        boolean done = true;
        for (Processor p : processors) {
            if (!p.done()) {
                done = false;
                break;
            }
        }
        return done;
    }

    private static PrintWriter getPrinter(String protocol, String inputFile, int cacheSize,
                                          int associativity, int blockSize) throws IOException {
        String fileName = protocol + "_" + inputFile + "_" + cacheSize + "_" + associativity + "_" + blockSize + ".txt";
        String path = "../result/" + fileName;
        Path pathToFile = Paths.get(path);
        if(!Files.exists(pathToFile)) {
            Files.createDirectories(pathToFile.getParent());
            Files.createFile(pathToFile);
        }
        return new PrintWriter(new BufferedWriter(new FileWriter(path)));
    }
}

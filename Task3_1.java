import java.util.*;
import java.util.concurrent.*;

public class Task3_1 {

    public static final double[] fracAddRange = {0.1, 0.5};
    public static final double[] fracRemoveRange = {0.1, 0.5};

    public static double fracAdd;
    public static double fracRemove;

    public static final int[] threadNumList = {2, 4, 6, 8};
    public static final int INT_MIN = 0;
    public static final int INT_MAX = (int) 1e7;
    public static final int INT_MEAN = (int) 5e6;
    public static final int INT_VAR = (int) 5e6 / 3;
    public static final int OPS_NUM = (int) 1e6;

    public static int threadNum;
    public static ExecutorService exec;

    static class OpsTask implements Callable<Void> {
        private LockedLFSkipList skipList;
        private Population pop;
        private int opsNum;

        private boolean recordLog = false;

        private ConcurrentLinkedQueue<Log> log;

        public OpsTask(int opsNum, LockedLFSkipList skipList, Population pop) {
            this.opsNum = opsNum;
            this.skipList = skipList;
            this.pop = pop;
        }

        public OpsTask(int opsNum, LockedLFSkipList skipList, Population pop, ConcurrentLinkedQueue<Log> log) {
            this.opsNum = opsNum;
            this.skipList = skipList;
            this.pop = pop;
            this.recordLog = true;
            this.log= log;
        }

        public Void call() {
            for (int i = 0; i < opsNum; i++) {
                double rr = Math.random();
                Log oneLog;

                if (rr < fracAdd) {
                    oneLog = this.skipList.add(pop.getSample());
                } else if (rr < (fracAdd + fracRemove)) {
                    oneLog = this.skipList.remove(pop.getSample());
                } else {
                    oneLog = this.skipList.contains(pop.getSample());
                }

                if (recordLog) {
                    log.add(oneLog);
                }
            }
            return null;
        }
    }

    public static void runOps(LockedLFSkipList skipList, String MODE, int testTimes) {
        List<Callable<Void>> tasks = new ArrayList<>();
        exec = Executors.newFixedThreadPool(threadNum);


        Population pop;
        long totalTime = 0;

        for (int cnt = 0; cnt < testTimes; cnt++) {
            for (int i = 0; i < threadNum; i++) {
                if (MODE.equals("uniform")) {
                    pop = new UniformPopulation(123, INT_MIN, INT_MAX);
                } else {
                    pop = new NormalPopulation(123, INT_MIN, INT_MAX, INT_MEAN, INT_VAR);
                }
                Task3_1.OpsTask task = new Task3_1.OpsTask((int) OPS_NUM / threadNum, skipList, pop);
                tasks.add(task);
            }
        }

        try {
            long t1 = System.nanoTime();
            exec.invokeAll(tasks);
            long t2 = System.nanoTime();
            totalTime += (t2 - t1);
            System.out.println("time spent: " + totalTime / 1e9 + "s\n");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exec.shutdown();
    }

    public static List<ConcurrentLinkedQueue<Log>> runOpsWithLogs(LockedLFSkipList skipList, String MODE, int testTimes) {
        List<Callable<Void>> tasks = new ArrayList<>();
        exec = Executors.newFixedThreadPool(threadNum);
        List<ConcurrentLinkedQueue<Log>> logList = new ArrayList<>();


        Population pop;
        long totalTime = 0;

        for (int cnt = 0; cnt < testTimes; cnt++) {
            for (int i = 0; i < threadNum; i++) {
                ConcurrentLinkedQueue<Log> log = new ConcurrentLinkedQueue<>();
                logList.add(log);
                if (MODE.equals("uniform")) {
                    pop = new UniformPopulation(123, INT_MIN, INT_MAX);
                } else {
                    pop = new NormalPopulation(123, INT_MIN, INT_MAX, INT_MEAN, INT_VAR);
                }
                Task3_1.OpsTask task = new Task3_1.OpsTask((int) OPS_NUM / threadNum, skipList, pop, log);
                tasks.add(task);
            }
        }

        try {
            long t1 = System.nanoTime();
            exec.invokeAll(tasks);
            long t2 = System.nanoTime();
            totalTime += (t2 - t1);
            System.out.println("time spent: " + totalTime / 1e9 + "s\n");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exec.shutdown();

        try {
            exec.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return logList;
    }

    private static void checkLogs (LinkedList<Integer> originalList, TreeMap<Long, Log> log) {
        int num = 0;
        int wrongOps = 0;
        Set<Integer> originalSet = new HashSet<>(originalList);
        for(Map.Entry<Long, Log> onelog : log.entrySet()) {
            num ++;
            Log realLog = onelog.getValue();
            boolean result = true;
            if (realLog.timeStamp != 0) {
                if (realLog.op.equals("add")) {
                    result = originalSet.add(realLog.num);
                } else if (realLog.op.equals("rmv")) {
                    result = originalSet.remove(realLog.num);
                } else if (realLog.op.equals("contains")) {
                    result = originalSet.contains(realLog.num);
                }
                if (!(result == realLog.success)) {
                    wrongOps ++;
                    System.out.println(num + ": " + realLog.op + " " + realLog.num + " " + realLog.timeStamp + " " + realLog.success);
                }
            }
        }
        if (wrongOps == 0)
            System.out.println("All correct!");
        else
            System.out.println(wrongOps + " wrong operations!");
    }


    public static void main(String [] args) {
        LockedLFSkipList uniSkipList = new LockedLFSkipList(false);
        Task1.populateLockedList(uniSkipList, "uniform");

        // Test the running time of the locked skiplist
//        for (int n = 0; n < threadNumList.length; n++) {
//            threadNum = threadNumList[n];
//            System.out.println("thread: " + threadNum);
//            for (int i = 0; i < fracAddRange.length; i++) {
//                fracAdd = fracAddRange[i];
//                fracRemove = fracRemoveRange[i];
//                System.out.println("add frac: " + fracAdd);
//                System.out.println("remove frac: " + fracRemove);
//                runOps(uniSkipList, "uniform", 5);
//            }
//        }

        //Test 3.2


        // Test if the skiplist meets sequential specification
        LinkedList<Integer> uniLinkedList = uniSkipList.toList();
        threadNum = 4;
        System.out.println("thread: " + threadNum);
        fracAdd = 0.5;
        fracRemove = 0.5;
        System.out.println("add frac: " + fracAdd);
        System.out.println("remove frac: " + fracRemove);
        List<ConcurrentLinkedQueue<Log>> logList = runOpsWithLogs(uniSkipList, "uniform", 1);
        TreeMap<Long, Log> completeLog = new TreeMap<>();
        for (ConcurrentLinkedQueue<Log> log : logList) {
            for (Log oneLog : log) {
                completeLog.put(oneLog.timeStamp, oneLog);
            }
        }
        checkLogs(uniLinkedList, completeLog);

    }




}

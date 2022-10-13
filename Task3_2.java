import java.util.*;
import java.util.concurrent.*;

public class Task3_2 {

    public static final int N = (int) 1e5;  // TODO: switch to 1e7
    public static final int nOps = (int) 1e4;  // TODO: switch to 1e6
    public static int nThreads = 48;
    public static int INT_STD = (int) 5e6 / 3;  // 5e6 / 3
    public static double fracContains = 0.8;




    public static final double[] fracAddRange = {0.1, 0.5};
    public static final double[] fracRemoveRange = {0.1, 0.5};

    public static double fracAdd = 0.1;
    public static double fracRemove = 0.1;

    public static final int[] threadNumList = {2, 4, 6, 8};
    public static final int INT_MIN = 0;
    public static  int INT_MAX = (int) 1e5;
    public static  int INT_MEAN = (int) 5e4;
    public static final int INT_VAR = (int) 5e4 / 3;
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
//        LockedLFSkipList uniSkipList = new LockedLFSkipList(false);
//        Task1.populateLockedList(uniSkipList, "uniform");
//
//        // Test the running time of the locked skiplist
////        for (int n = 0; n < threadNumList.length; n++) {
////            threadNum = threadNumList[n];
////            System.out.println("thread: " + threadNum);
////            for (int i = 0; i < fracAddRange.length; i++) {
////                fracAdd = fracAddRange[i];
////                fracRemove = fracRemoveRange[i];
////                System.out.println("add frac: " + fracAdd);
////                System.out.println("remove frac: " + fracRemove);
////                runOps(uniSkipList, "uniform", 5);
////            }
////        }
//
//        // Test if the skiplist meets sequential specification
//        LinkedList<Integer> uniLinkedList = uniSkipList.toList();
//        threadNum = 48;
//        System.out.println("thread: " + threadNum);
//        fracAdd = 0.4;
//        fracRemove = 0.4;
//        System.out.println("add frac: " + fracAdd);
//        System.out.println("remove frac: " + fracRemove);
//        List<ConcurrentLinkedQueue<Log>> logList = runOpsWithLogs(uniSkipList, "uniform", 1);
//        TreeMap<Long, Log> completeLog = new TreeMap<>();
//        for (ConcurrentLinkedQueue<Log> log : logList) {
//            for (Log oneLog : log) {
//                completeLog.put(oneLog.timeStamp, oneLog);
//            }
//        }
//        checkLogs(uniLinkedList, completeLog);

        // Create normal distribution skip list.
        LockedLFSkipList skipListNormal = new LockedLFSkipList(false);
        Task1.populateLockedList(skipListNormal, "normal");

        // Create uniform distribution skip list.
        LockedLFSkipList skipListUniform = new LockedLFSkipList(false);
        Task1.populateLockedList(skipListUniform, "uniform");

        // Mixed operation test.
        System.out.println("\nStarting consistency test.\n");
        consistencyTest(skipListUniform, skipListNormal);
        INT_MAX = (int) 1e6; INT_MEAN = (int) 5e5; INT_STD = (int) 5e5 / 3;
        consistencyTest(skipListUniform, skipListNormal);
        INT_MAX = (int) 1e5; INT_MEAN = (int) 5e4; INT_STD = (int) 5e4 / 3;
        consistencyTest(skipListUniform, skipListNormal);
        System.out.println("Finished testing.");

    }

    private static void consistencyTest(LockedLFSkipList skipListUniform, LockedLFSkipList skipListNormal) {
        LinkedList<Integer> uniformList = skipListUniform.toList();
        LinkedList<Integer> normalList = skipListNormal.toList();
        List<LogWrapper> logListUniform = testOps(skipListUniform, "uniform", 1);
        List<LogWrapper> logListNormal = testOps(skipListNormal, "normal", 1);
        TreeMap<Long, Log> completeUniformLog = new TreeMap<Long, Log>();
        TreeMap<Long, Log> completeNormalLog = new TreeMap<Long, Log>();
        for(LogWrapper log : logListUniform) {
            completeUniformLog.putAll(log.toTreeMap());
        }
        for(LogWrapper log : logListNormal) {
            completeNormalLog.putAll(log.toTreeMap());
        }
        int errCnt;
        errCnt = LogChecker.checkLogs(uniformList, completeUniformLog);
        System.out.println("Local log uniform error count: " + errCnt);
        errCnt = LogChecker.checkLogs(normalList, completeNormalLog);
        System.out.println("Local log normal error count: " + errCnt);
    }

    private static List<LogWrapper> testOps(LockedLFSkipList skipList, String mode, int nTests) {
        exec = Executors.newFixedThreadPool(nThreads);
        List<LogWrapper> logList = new ArrayList<>();
        for(int i = 0; i < nTests; i++) {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int j = 0; j < nThreads; j++) {
                TreeMap<Long, Log> log = new TreeMap<Long, Log>();
                LogWrapper logWrapper = new LogWrapper(log);
                logList.add(logWrapper);
                OpsTask1 task = new OpsTask1(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains,
                        INT_MIN, INT_MAX, INT_MEAN, INT_STD, mode, logWrapper, true);
                tasks.add(task);
            }
            try {
                exec.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        exec.shutdown();
        try {
            exec.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return logList;
    }


}

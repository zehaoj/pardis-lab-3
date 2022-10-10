import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Task2 {
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
        private LockFreeSkipList skipList;
        private Population pop;
        private int opsNum;

        public OpsTask(int opsNum, LockFreeSkipList skipList, Population pop) {
            this.opsNum = opsNum;
            this.skipList = skipList;
            this.pop = pop;
        }

        public Void call() {
            for (int i = 0; i < opsNum; i++) {
                double rr = Math.random();

                if (rr < fracAdd) {
                    this.skipList.add(pop.getSample());
                } else if (rr < (fracAdd + fracRemove)) {
                    this.skipList.remove(pop.getSample());
                } else {
                    this.skipList.contains(pop.getSample());
                }
            }
            return null;
        }
    }
    public static void runOps(LockFreeSkipList skipList, String MODE) {
        List<Callable<Void>> tasks = new ArrayList<>();
        exec = Executors.newFixedThreadPool(threadNum);


        Population pop;
        long totalTime = 0;

        for (int cnt = 0; cnt < 5; cnt++) {
            for (int i = 0; i < threadNum; i++) {
                if (MODE.equals("uniform")) {
                    pop = new UniformPopulation(123, INT_MIN, INT_MAX);
                } else {
                    pop = new NormalPopulation(123, INT_MIN, INT_MAX, INT_MEAN, INT_VAR);
                }
                OpsTask task = new OpsTask((int) OPS_NUM / threadNum, skipList, pop);
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


    public static void main(String [] args) {
        LockFreeSkipList uniSkipList = new LockFreeSkipList();
        Task1.populate(uniSkipList, "uniform");
        for (int n = 0; n < threadNumList.length; n++) {
            threadNum = threadNumList[n];
            System.out.println("thread: " + threadNum);
            for (int i = 0; i < fracAddRange.length; i++) {
                fracAdd = fracAddRange[i];
                fracRemove = fracRemoveRange[i];
                System.out.println("add frac: " + fracAdd);
                System.out.println("remove frac: " + fracRemove);
                runOps(uniSkipList, "uniform");
            }
        }
    }
}

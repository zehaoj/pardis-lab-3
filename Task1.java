import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Task1 {
    public static final int INT_MIN = 0;
    public static final int INT_MAX = (int) 1e7;
    public static final int INT_MEAN = (int) 5e6;
    public static final int INT_VAR = (int) 5e6 / 3;

    public static final int N = (int) 1e6;
    public static final int threadNum = (int) 8;

    public static LockFreeSkipList populate(LockFreeSkipList skipList, String MODE) {
        ExecutorService exec = Executors.newFixedThreadPool(threadNum);
        List<Callable<Void>> tasks = new ArrayList<>();
        Population pop;
        if (MODE.equals("uniform")) {
            pop = new UniformPopulation(123, INT_MIN, INT_MAX);
        } else {
            pop = new NormalPopulation(123, INT_MIN, INT_MAX, INT_MEAN, INT_VAR);
        }
        for (int i = 0; i < threadNum; i++) {
            PopulateTask task = new PopulateTask(skipList, (int) N / threadNum, pop);
            tasks.add(task);
        }
        try {
            exec.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        exec.shutdown();
        return skipList;
    }

    public static LockedLFSkipList populateLockedList(LockedLFSkipList skipList, String MODE) {
        ExecutorService exec = Executors.newFixedThreadPool(threadNum);
        List<Callable<Void>> tasks = new ArrayList<>();
        Population pop;
        if (MODE.equals("uniform")) {
            pop = new UniformPopulation(123, INT_MIN, INT_MAX);
        } else {
            pop = new NormalPopulation(123, INT_MIN, INT_MAX, INT_MEAN, INT_VAR);
        }
        for (int i = 0; i < threadNum; i++) {
            PopulateLockedTask task = new PopulateLockedTask(skipList, (int) N / threadNum, pop);
            tasks.add(task);
        }
        try {
            exec.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        exec.shutdown();
        return skipList;
    }

    static class PopulateTask implements Callable<Void>{
        private int iterNum;
        private LockFreeSkipList skipList;
        private Population pop;

        public PopulateTask(LockFreeSkipList skipList, int N, Population pop) {
            this.iterNum = N;
            this.skipList = skipList;
            this.pop = pop;
        }

        public Void call() {
            for (int i = 0; i < iterNum; i++) {
                skipList.add(pop.getSample());
            }
            return null;
        }
    }

    static class PopulateLockedTask implements Callable<Void>{
        private int iterNum;
        private LockedLFSkipList skipList;
        private Population pop;

        public PopulateLockedTask(LockedLFSkipList skipList, int N, Population pop) {
            this.iterNum = N;
            this.skipList = skipList;
            this.pop = pop;
        }

        public Void call() {
            for (int i = 0; i < iterNum; i++) {
                skipList.add(pop.getSample());
            }
            return null;
        }
    }


    public static void main(String [] args) {
        LockFreeSkipList skipListTest = new LockFreeSkipList();
        populate(skipListTest, "normal");
        LinkedList<Integer> listTest = skipListTest.toList();
        int count = (int) listTest.parallelStream().count();
        System.out.println(count);
        long sum = listTest.parallelStream().mapToLong(i -> i).sum();
        double mean = sum/count;
        double var = listTest.parallelStream().mapToDouble(i -> (Math.pow(i - mean, 2.))).sum()/count;
        System.out.println("Mean: " + mean + ", variance: " + var);




    }
}

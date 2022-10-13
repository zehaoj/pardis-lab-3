import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public final class LockedLFSkipList<T> {
    /* Number of levels */
    private static final int MAX_LEVEL = 16;

    /* RNG for randomLevel() function */
    /* Random is thread safe! :) */
    private static final Random rng = new Random();

    final ReentrantLock linearizationLock;

    private final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    private final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

    private final boolean addLock;

    public LockedLFSkipList(boolean addLock) {
        this.addLock = addLock;
        linearizationLock = new ReentrantLock();
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<LockedLFSkipList.Node<T>>(tail, false);
        }
    }

    private static final class Node<T> {
        final T value;
        final int key;
        final AtomicMarkableReference<Node<T>>[] next;
        private final int topLevel;

        @SuppressWarnings("unchecked")
        public Node(int key) {
            value = null;
            this.key = key;
            next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[MAX_LEVEL + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = MAX_LEVEL;
        }

        @SuppressWarnings("unchecked")
        public Node(T x, int height) {
            value = x;
            key = x.hashCode();
            next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[height + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = height;
        }
    }

    private class TimeStampedBool {
        long linTime;
        boolean find;

        public TimeStampedBool (long linTime, boolean find) {
            this.linTime = linTime;
            this.find = find;
        }
    }

    /* Returns a level between 0 to MAX_LEVEL,
     * P[randomLevel() = x] = 1/2^(x+1), for x < MAX_LEVEL.
     */
    private static int randomLevel() {
        int r = rng.nextInt();
        int level = 0;
        r &= (1 << MAX_LEVEL) - 1;
        while ((r & 1) != 0) {
            r >>>= 1;
            level++;
        }
        return level;
    }

    @SuppressWarnings("unchecked")
    public Log add(T x) {
        long lintime = 0;
        int topLevel = randomLevel();
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        while (true) {
            TimeStampedBool timeStampedBool = find(x, preds, succs); // Linearization point when failed. Inside find() method.
            Boolean found = timeStampedBool.find;
            lintime = timeStampedBool.linTime;
            if (found) {
                return new Log("add", x.hashCode(), false, lintime);
            } else {
                Node<T> newNode = new Node(x, topLevel);
                for (int level = bottomLevel; level <= topLevel; level++) {
                    Node<T> succ = succs[level];
                    newNode.next[level].set(succ, false);
                }
                Node<T> pred = preds[bottomLevel];
                Node<T> succ = succs[bottomLevel];

                if (addLock)
                    linearizationLock.lock();
                if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) { // Linearization point when succeeded.
                    if (addLock)
                        linearizationLock.unlock();
                    continue;
                }
                lintime = System.nanoTime();
                if (addLock)
                    linearizationLock.unlock();

                for (int level = bottomLevel + 1; level <= topLevel; level++) {
                    while (true) {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, newNode, false, false))
                            break;
                        find(x, preds, succs);
                    }
                }
                return new Log("add", x.hashCode(), true, lintime);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Log remove(T x) {
        long lintime = 0;
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> succ;
        while (true) {
            TimeStampedBool timeStampedBool = find(x, preds, succs);
            boolean found = timeStampedBool.find;
            lintime = timeStampedBool.linTime;
            if (!found) {
                return new Log("rmv", x.hashCode(), false, lintime);
            } else {
                Node<T> nodeToRemove = succs[bottomLevel];
                for (int level = nodeToRemove.topLevel; level >= bottomLevel+1; level --) {
                    boolean[] marked = {false};
                    succ = nodeToRemove.next[level].get(marked);
                    while (!marked[0]) {
                        nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
                        succ = nodeToRemove.next[level].get(marked);
                    }
                }
                boolean[] marked = {false};
                succ = nodeToRemove.next[bottomLevel].get(marked);
                while (true) {

                    if (addLock)
                        linearizationLock.lock();
                    boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true); // Linearization point when succeeded.
                    lintime = System.nanoTime();
                    if (addLock)
                        linearizationLock.unlock();

                    succ = succs[bottomLevel].next[bottomLevel].get(marked);
                    if (iMarkedIt) {
                        find(x, preds, succs);
                        /*size.getAndDecrement();*/
                        return new Log("rmv", x.hashCode(), true, lintime);
                    } else if (marked[0]) {
                        return new Log("rmv", x.hashCode(), false, 0);
                    }
                }
            }
        }
    }


    private TimeStampedBool find(T x, Node<T>[] preds, Node<T>[] succs) {
        long lintime = 0;
        int bottomLevel = 0;
        int key = x.hashCode();
        boolean[] marked = {false};
        boolean snip;
        Node<T> pred = null;
        Node<T> curr = null;
        Node<T> succ = null;
        retry:
        while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
//                if (level == bottomLevel)
                if (addLock)
                    linearizationLock.lock();
                curr = pred.next[level].getReference(); // Linearization point 1
//                if (level == bottomLevel)
                lintime = System.nanoTime();
                if (addLock)
                    linearizationLock.unlock();
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        snip = pred.next[level].compareAndSet(curr, succ, false, false);
                        if (!snip) continue retry;

                        if (addLock)
                            linearizationLock.lock();
                        curr = pred.next[level].getReference(); // Linearization point 2. Is it?
                        lintime = System.nanoTime();
                        if (addLock)
                            linearizationLock.unlock();

                        succ = curr.next[level].get(marked);
                    }
                    if (curr.key < key) {
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }
                preds[level] = pred;
                succs[level] = curr;
            }
            return new TimeStampedBool(lintime, (curr.key == key));
            // Different objects may have the same hash.
            // return (curr.key == key) && x.equals(curr.value);
        }
    }

    public Log contains(T x) {
        long lintime = 0;
        int bottomLevel = 0;
        int v = x.hashCode();
        boolean[] marked = {false};
        Node<T> pred = head;
        Node<T> curr = null;
        Node<T> succ = null;
        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {

            if (addLock)
                linearizationLock.lock();
            curr = pred.next[level].getReference(); // Linearization point 1
            lintime = System.nanoTime();
            if (addLock)
                linearizationLock.unlock();

            while (true) {
                succ = curr.next[level].get(marked);
                while (marked[0]) {

                    if (addLock)
                        linearizationLock.lock();
                    curr = succ; /* Same as, curr.next[level].getReference() */ // Linearization point 2
                    lintime = System.nanoTime();
                    if (addLock)
                        linearizationLock.unlock();

                    succ = curr.next[level].get(marked);
                }
                if (curr.key < v) {
                    pred = curr;
                    curr = succ;
                } else {
                    break;
                }
            }
        }
        return new Log("contains", v, (curr.key == v), lintime);
        // Different objects may have the same hash.
        // return (curr.key == key) && x.equals(curr.value);
    }

    public LinkedList<Integer> toList() {
        LinkedList<Integer> list = new LinkedList<Integer>();
        Node currNode = head.next[0].getReference();
        while(currNode != null && currNode.key != Integer.MAX_VALUE) {
            list.add(currNode.key);
            currNode = (Node) currNode.next[0].getReference();
        }
        return list;
    }
}

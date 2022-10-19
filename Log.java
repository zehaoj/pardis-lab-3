public class Log {

    public String op;
    public int num;
    public boolean success;
    public long timeStamp;
    public long timeBefore;

    public Log (String op, int num, boolean success, long timeStamp, long timeBefore) {
        this.op = op;
        this.num = num;
        this.success = success;
        this.timeStamp = timeStamp;
        this.timeBefore = timeBefore;
    }

    public int compareTo (long anotherTS) {
        if (anotherTS < timeStamp)
            return 1;
        else if (anotherTS > timeStamp)
            return -1;
        else
            return 0;
    }
}

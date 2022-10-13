public class Log {

    public String op;
    public int num;
    public boolean success;
    public long timeStamp;

    public Log (String op, int num, boolean success, long timeStamp) {
        this.op = op;
        this.num = num;
        this.success = success;
        this.timeStamp = timeStamp;
    }
}

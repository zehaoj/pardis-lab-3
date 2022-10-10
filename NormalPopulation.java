import java.util.Random;

public class NormalPopulation implements Population {

    private static Random r;
    private double var_;
    private double mean_;
    private int min_;
    private int max_;
    private int cnt;

    public NormalPopulation(int seed, int min, int max, double mean, double var) {
        r = new Random();
        var_ = var;
        mean_ = mean;
        min_ = min;
        max_ = max;
        cnt = 0;
    }

    public int getCount() {
        return cnt;
    }

    public int getSample() {
        int next;
        while (true) {
            next = (int) (r.nextGaussian() * Math.sqrt(var_) + mean_);
            if(min_ <= next && next <= max_) {
                break;
            }
        }
        cnt += 1;
        return next;
    }
}

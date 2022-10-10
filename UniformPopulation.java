import java.util.Random;

public class UniformPopulation implements Population {
    private static Random r;
    private int cnt;

    private int max_;

    public UniformPopulation(int seed, int min, int max) {
        r = new Random();
        max_ = max;
        cnt = 0;
    }

    public int getCount() {
        return cnt;
    }

    public int getSample() {
        cnt += 1;
        return r.nextInt(max_);
    }
}

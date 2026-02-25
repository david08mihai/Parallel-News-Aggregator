public class CountWords implements Comparable<CountWords>{
    public String word;
    public Integer count;
    public CountWords(String word, Integer count) {
        this.word = word;
        this.count = count;
    }

    @Override
    public int compareTo(CountWords o) {
        if (Integer.compare(this.count, o.count) != 0) {
            return Integer.compare(o.count, this.count);
        }
        return word.compareTo(o.word);
    }
}

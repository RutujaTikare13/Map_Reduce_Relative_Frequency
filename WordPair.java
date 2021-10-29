package main.java.edu.njit.bd.hw2.bkup;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class WordPair implements Writable, WritableComparable<WordPair> {
    private Text wordOne;
    private Text wordTwo;

    public WordPair(Text wordOne, Text wordTwo) {
        this.wordOne = wordOne;
        this.wordTwo = wordTwo;
    }

    public WordPair(String wordOneStr, String wordTwoStr) {
        this.wordOne = new Text(wordOneStr);
        this.wordTwo = new Text(wordTwoStr);
    }

    public WordPair() {
        this.wordOne = new Text();
        this.wordTwo = new Text();
    }

    public Text getWordOne() {
        return wordOne;
    }

    public Text getWordTwo() {
        return wordTwo;
    }

    public void setWordOne(Text wordOne) {
        this.wordOne = wordOne;
    }

    public void setWordTwo(Text wordTwo) {
        this.wordTwo = wordTwo;
    }

    /**
     * We will be using wordOne to compare two WordPairs's. This is done so that all words will be sorted by first word.
     * This is done so that when sorted words with second word as '*' will always be the first entry.
     * @param o The WordPair to compare this WordPair
     * @return a value less than 0 if this wordOne is lexicographically less than the argument's wordOne;
     * and a value greater than 0 if this wordOne is lexicographically greater than the argument's wordOne.
     */
    @Override
    public int compareTo(WordPair o) {
        int returnVal = this.getWordOne().compareTo(o.getWordOne());
        // If this wordOne and arguments wordOne are different return returnVal.
        if(returnVal != 0){
            return returnVal;
        }
        // If this wordTwo has a '*' it has lower priority. We do this so that shuffler always places WordPairs with '*' at the top.
        if(this.getWordTwo().toString().equals("*")){
            return -1;
        }else if(o.getWordTwo().toString().equals("*")){
            return 1;
        }
        // If word ones are same use word two to compare.
        return this.getWordTwo().compareTo(o.getWordTwo());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        wordOne.write(out);
        wordTwo.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        wordOne.readFields(in);
        wordTwo.readFields(in);
    }

    @Override
    public String toString() {
        return (wordOne.toString() + " " + wordTwo.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WordPair wordPair = (WordPair) o;

        if (wordTwo != null ? !wordTwo.equals(wordPair.wordTwo) : wordPair.wordTwo != null) {
            return false;
        }
        if (wordOne != null ? !wordOne.equals(wordPair.wordOne) : wordPair.wordOne != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = wordOne != null ? wordOne.hashCode() : 0;
        result = 163 * result + (wordTwo != null ? wordTwo.hashCode() : 0);
        return result;
    }
}

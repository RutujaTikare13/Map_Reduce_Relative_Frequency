package main.java.edu.njit.bd.hw2.bkup;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Partitioner;

public class WordPartitioner extends Partitioner<WordPair,IntWritable> {

    @Override
    public int getPartition(WordPair wordPair, IntWritable intWritable, int numPartitions) {
        return (wordPair.getWordOne().hashCode() & Integer.MAX_VALUE ) % numPartitions;
    }
}

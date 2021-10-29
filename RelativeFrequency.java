package main.java.edu.njit.bd.hw2.bkup;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class RelativeFrequency extends Configured implements Tool {
  private static int count = 0;

  public static class TokenizerMapper 
       extends Mapper<LongWritable, Text, WordPair, IntWritable>{
    
    private final static IntWritable ONE = new IntWritable(1);
    private final static IntWritable TWO = new IntWritable(2);
    private final static Text ALL = new Text("*");
      
    public void map(LongWritable key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      String previous = null;
      boolean firstWord = true;
      WordPair wordPair;

      Set<WordPair> buffer = new HashSet<>();
      while (itr.hasMoreTokens()) {
        String currentWord = itr.nextToken();
        currentWord = currentWord.replaceAll("\\p{Punct}", "");
        if (currentWord.matches("^[A-Za-z]+$")) {
          currentWord = currentWord.replaceAll("\\W+","");
          if (StringUtils.isBlank(currentWord)) {
            continue;
          }
          currentWord = currentWord.toUpperCase();
          if (firstWord) {
            wordPair = new WordPair(new Text(currentWord), ALL);
            context.write(wordPair, ONE);
            firstWord = false;

          } else if (itr.countTokens() == 0) {
            wordPair = new WordPair(currentWord, previous);
            if (!buffer.contains(wordPair)) {
              context.write(wordPair, ONE);
              buffer.add(wordPair);
            }
            wordPair = new WordPair(previous, currentWord);
            if (!buffer.contains(wordPair)) {
              context.write(wordPair, ONE);
              buffer.add(wordPair);
            }
            context.write(new WordPair(new Text(currentWord), ALL), ONE);
          } else {
            wordPair = new WordPair(currentWord, previous);
            if (!buffer.contains(wordPair)) {
              context.write(wordPair, ONE);
              buffer.add(wordPair);
            }
            wordPair = new WordPair(previous, currentWord);
            if (!buffer.contains(wordPair)) {
              context.write(wordPair, ONE);
              buffer.add(wordPair);
            }
            context.write(new WordPair(new Text(currentWord), ALL), TWO);
          }
          previous = currentWord;
        } else {
          firstWord = true;
        }
      }
    }
  }
  
  public static class IntSumReducer 
       extends Reducer<WordPair,IntWritable,WordPair, FloatWritable> {
    private int individualWordCount = 0;
    private FloatWritable relativeFrequency = new FloatWritable();
    private final static String ALL = "*";
    private Text newWord = new Text();

    public void reduce(WordPair key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      int count = getCount(values);

      if (key.getWordTwo().toString().equals(ALL)) {
        if(key.getWordOne().equals(newWord)) {
          individualWordCount += count;
        } else {
          newWord.set(key.getWordOne());
          individualWordCount = count;
        }
      } else {
        relativeFrequency.set(((float) count) / individualWordCount);
        context.write(key, relativeFrequency);
      }
    }

    private int getCount(Iterable<IntWritable> values) {
      int count = 0;
      for (IntWritable value : values) {
        count += value.get();
      }
      return count;
    }
  }

  public static class FrequencyCalculatorMapper
          extends Mapper<Text, Text, Text, Text> {
    private IntWritable result = new IntWritable();
    private Text word = new Text();
    private Text UNIVERSAL_KEY = new Text("RESULT");

    public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
      String[] splits = key.toString().split("\\s+");
      String valueToWrite = new StringBuilder()
              .append(splits[0])
              .append(":::")
              .append(splits[1])
              .append("###")
              .append(Float.parseFloat(value.toString()))
              .toString();
      word.set(valueToWrite);
      context.write(UNIVERSAL_KEY, word);
    }
  }

  public static class FrequencyCalculatorReducer
          extends Reducer<Text, Text, WordPair, FloatWritable> {
    public void reduce(Text Text, Iterable<Text> values,
                       Context context) throws IOException, InterruptedException {
      int count = 0;
      Set<RelativeFrequencyEntry> priorityQueue =
              new TreeSet<>(new Comparator<RelativeFrequencyEntry>() {
                @Override
                public int compare(RelativeFrequencyEntry o1, RelativeFrequencyEntry o2) {
                  return Float.compare(o2.getValue(), o1.getValue());
                }
              });

      for (Text value : values) {
        String[] splits = value.toString().split("###");
        String[] words = splits[0].split(":::");
        priorityQueue.add(new RelativeFrequencyEntry(new WordPair(words[0], words[1]), Float.parseFloat(splits[1])));
      }

      for(RelativeFrequencyEntry entry : priorityQueue) {
        if (count >= 100) {
          break;
        }
        context.write(entry.getKey(), new FloatWritable(entry.getValue()));
        count++;
      }
    }
  }

  private static class RelativeFrequencyEntry {
    private WordPair key;
    private float value;

    public RelativeFrequencyEntry(WordPair key, float value) {
      this.key = key;
      this.value = value;
    }

    public WordPair getKey() {
      return key;
    }

    public void setKey(WordPair key) {
      this.key = key;
    }

    public float getValue() {
      return value;
    }

    public void setValue(float value) {
      this.value = value;
    }
  }

  public int run(String[] args) throws Exception {


    // Creating a job control
    JobControl jobControl = new JobControl("jobChain");

    // Creating Job 1
    Configuration conf1 = getConf();
    Job job1 = Job.getInstance(conf1);
    job1.setJarByClass(RelativeFrequency.class);
    job1.setJobName("Relative Frequency Tokenizer");

    FileInputFormat.setInputPaths(job1, new Path(args[0]));
    FileOutputFormat.setOutputPath(job1, new Path(args[1] + "/temp"));

    job1.setMapperClass(TokenizerMapper.class);
    job1.setReducerClass(IntSumReducer.class);
    job1.setPartitionerClass(WordPartitioner.class);

    job1.setMapOutputKeyClass(WordPair.class);
    job1.setMapOutputValueClass(IntWritable.class);
    job1.setOutputKeyClass(WordPair.class);
    job1.setOutputValueClass(FloatWritable.class);

    // Creating a controlled Job 1
    ControlledJob controlledJob1 = new ControlledJob(conf1);
    controlledJob1.setJob(job1);

    jobControl.addJob(controlledJob1);

    // Creating Job 2
    Configuration conf2 = getConf();
    Job job2 = Job.getInstance(conf2);
    job2.setJarByClass(RelativeFrequency.class);
    job2.setJobName("Relative Frequency Calculator");

    FileInputFormat.setInputPaths(job2, new Path(args[1] + "/temp"));
    FileOutputFormat.setOutputPath(job2, new Path(args[1] + "/final"));

    job2.setMapperClass(FrequencyCalculatorMapper.class);
    job2.setReducerClass(FrequencyCalculatorReducer.class);

    job2.setMapOutputKeyClass(Text.class);
    job2.setMapOutputValueClass(Text.class);
    job2.setOutputKeyClass(WordPair.class);
    job2.setOutputValueClass(FloatWritable.class);
    job2.setInputFormatClass(KeyValueTextInputFormat.class);

    ControlledJob controlledJob2 = new ControlledJob(conf2);
    controlledJob2.setJob(job2);

    // make job2 dependent on job1
    controlledJob2.addDependingJob(controlledJob1);
    // add the job to the job control
    jobControl.addJob(controlledJob2);
    Thread jobControlThread = new Thread(jobControl);
    jobControlThread.start();

    while (!jobControl.allFinished()) {
      try {
        Thread.sleep(5000);
      } catch (Exception e) {

      }

    }
    System.exit(0);
    return (job1.waitForCompletion(true) ? 0 : 1);
  }

  public static void main(String[] args) throws Exception {
    int exitCode = ToolRunner.run(new RelativeFrequency(), args);
    System.exit(exitCode);


  }
}

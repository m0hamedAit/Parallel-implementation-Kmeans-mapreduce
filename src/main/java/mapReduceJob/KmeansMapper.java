package mapReduceJob;

import entities.Pixel;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class KmeansMapper extends Mapper<LongWritable, Text, Text,Text> {
    List<Double> centroide = new ArrayList<>();

    @Override
    protected void setup(Mapper<LongWritable, Text, Text, Text>.Context context) throws IOException, InterruptedException {
        centroide.clear();
        URI uri[]= context.getCacheFiles();
        FileSystem fs=FileSystem.get(context.getConfiguration());
        InputStreamReader is=new InputStreamReader(fs.open(new Path(uri[0])));
        BufferedReader br=new BufferedReader(is);
        String line=null;

        while ((line=br.readLine())!=null){
            centroide.add(Double.parseDouble(line));
        }
    }

    @Override
    protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context) throws IOException, InterruptedException {
        Pixel p = new Pixel();
        p.lineToPixel(value.toString());
        double min=Double.MAX_VALUE,d;
        double nearest_center = 0;
        for (double c:centroide) {
            d = Math.abs(c-p.getValue());
            if (d<min){
                min=d;
                nearest_center=c;
            }
        }
        context.write(new Text(String.valueOf(nearest_center)), value); // value (p.x,p.y,p.value)
    }
}
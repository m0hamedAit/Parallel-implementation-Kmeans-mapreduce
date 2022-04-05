package mapReduceJob;

import entities.Pixel;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Iterator;

public class KmeansReducer extends Reducer<Text,Text,Text,Text> {
    @Override
    protected void reduce(Text key, Iterable<Text> values, Reducer<Text, Text, Text, Text>.Context context) throws IOException, InterruptedException {
        double somme = 0;
        StringBuilder pixels = new StringBuilder();
        int nb_points=0;
        Iterator<Text> it=values.iterator();
        while(it.hasNext()){
            String iterationVal = it.next().toString();
            pixels.append(iterationVal+"/"); //
            //
            Pixel p = new Pixel();
            p.lineToPixel(iterationVal);
            somme += p.getValue();

            nb_points++;
        }
        double mean= somme/nb_points;
        context.write(new Text(key+","+mean),new Text(pixels.toString())); // output will be ( old_centroid,new_centroide  pixels[] )        // pixels[] : pixel1/pixel2/...
    }
}


package mapReduceJob;

import entities.Centroid;
import entities.Pixel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;

public class KmeansDriver {

    private static boolean  CentroidsChanged = true;
    private static  Path file = new Path("hdfs://localhost:9000/inputs/imageData.txt");
    private static  Path output = new Path("hdfs://localhost:9000/outputs/outputMRI");

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, URISyntaxException {
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(conf);

        if ( fs.exists( file )) { fs.delete( file, true ); }
        // Get data (x, y, color) from image and save them to imageData.txt //

            //get image from hdfs
        BufferedImage image = ImageIO.read(fs.open(new Path(args[0])));
        imageToData(fs, image);

        int i=0; //iterations
        while(true){ // repeat mapReduce Job while iterations < 10 and centroids change after every job
            Job job = Job.getInstance(conf, "K-Mean MRI Job");
            job.setJarByClass(KmeansDriver.class);
            job.setMapperClass(KmeansMapper.class);
            job.setReducerClass(KmeansReducer.class);

            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(Text.class);

            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            job.setInputFormatClass(TextInputFormat.class);
            job.setOutputFormatClass(TextOutputFormat.class);

            job.addCacheFile(new URI("hdfs://localhost:9000/inputs/centersMRI.txt"));
            FileInputFormat.addInputPath(job, file); ///


            if ( fs.exists( output )) { fs.delete( output, true ); }  // delete output if exists
            FileOutputFormat.setOutputPath(job, output);

            job.waitForCompletion(true); //

            // replace centroids with new centroids from last output file after the end of every job //
            updateCentroids(fs);
            if(i>=10 || !CentroidsChanged){
                break;
            }

            i++;
        }

        // create images "gray_matter.gif", "white_matter.gif" , "cephalo_rachidien.png" from obtained output
        createImagesFromOutput(fs, image);
    }

    //extract data from IRM image
    public static void imageToData(FileSystem fs, BufferedImage image) throws IOException {

        BufferedWriter br = new BufferedWriter( new OutputStreamWriter(fs.create(file)) );
        StringBuilder data = new StringBuilder();

        // get data from image
        for(int i=0;i<image.getWidth();i++){
            for(int j=0;j<image.getHeight();j++) {
                int pixelVal = image.getRGB(i,j) & 0xFF; // not sure if this is the right way to get the value but it works, 'getRGB(i,j) & 0xFF' normally returns blue color value
                if(pixelVal!=0)   // to ignore black pixels, so they won't affect the process
                    data.append(i+","+j+","+pixelVal+"\n");
            }
        }
            // save data to imageData.txt
        br.write(data.toString());
        br.close();
    }

    // update centroids in centerMRI.txt file using last output
    public static void updateCentroids(FileSystem fs) throws IOException {
       // Path centroidsInput = new Path("hdfs://localhost:9000/inputs/centersMRI.txt");
        //if ( fs.exists( output )) { fs.delete( output, true ); }  // delete output if exists
        FSDataOutputStream out = fs.create(new Path("hdfs://localhost:9000/inputs/centersMRI.txt"), true);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));

        // get new centroids from last output
        InputStreamReader is = new InputStreamReader(fs.open(new Path("hdfs://localhost:9000/outputs/outputMRI/part-r-00000")));
        BufferedReader br = new BufferedReader(is);
        String line = null;

        StringBuilder old_centroid = new StringBuilder();
        StringBuilder new_centroid = new StringBuilder();

        while ((line = br.readLine()) != null) {
            String[] parts = line.replaceAll("\\s+", " ").split(" ");
            String[] centroids = parts[0].split(",");
            //new centroids
            new_centroid.append(centroids[1]);
            new_centroid.append("\n");
            //old centroids
            old_centroid.append(centroids[0]);
            old_centroid.append("\n");
        }

        // if old centroids == new centroids or iterations>=10 -> end while
        if(new_centroid.toString().equals(old_centroid.toString())){
            CentroidsChanged = false;
        }

        // save new centroids to centerMRI.txt
        bw.write(new_centroid.toString());
        bw.close();
        br.close();
    }

    public static void createImagesFromOutput(FileSystem fs, BufferedImage image) throws IOException {

        InputStreamReader is = new InputStreamReader(fs.open(new Path("hdfs://localhost:9000/outputs/outputMRI/part-r-00000")));  // read last output file
        BufferedReader br = new BufferedReader(is);

        String line1 = null;
        ArrayList<Centroid> centroids = new ArrayList<>();
        while ((line1 = br.readLine()) != null) {

            ArrayList<Pixel> px = new ArrayList<>();
            String[] parts = line1.replaceAll("\\s+", " ").split(" ");
            double centroid = Double.parseDouble(parts[0].split(",")[1]);
            String[] pStr = parts[1].split("/");
            for (String s : pStr) {
                Pixel p = new Pixel();
                p.lineToPixel(s);
                px.add(p);
            }

            centroids.add(new Centroid(centroid, px));
        }

        String[] imagesPath = {"cephalo_rachidien.gif", "white_matter.gif", "gray_matter.gif"};

        // sort centroids array by center attribute
        centroids.sort(Comparator.comparingDouble(Centroid::getCenter));

        // create output images
        for(int j=0;j<3;j++){
            // instanciate black image
            BufferedImage bImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Centroid c = centroids.get(j);
            for(Pixel p:c.getPixels()){
                bImage.setRGB(p.getX(), p.getY(), image.getRGB(p.getX(), p.getY()));
            }

            ImageIO.write(bImage, "gif", fs.create(new Path("hdfs://localhost:9000/outputs/"+imagesPath[j])));
        }
    }
}

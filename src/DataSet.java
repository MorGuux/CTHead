import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DataSet {
    private static float[][][] grey; //store the 3D volume data set converted to 0-1 ready to copy to the image

    //Function to return the 3D data set
    public DataSet(File file, int sliceCount) {

        try {
            readData(file, sliceCount);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error reading file");
            alert.setContentText("The file could not be read");
            alert.showAndWait();
        }

    }

    //Function to read in the data set
    public void readData(File file, int sliceCount) throws IOException {

        //Read the data quickly via a buffer
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        int i, j, k; //loop through the 3D data set

        //min value in the 3D volume data set
        short min = Short.MAX_VALUE;
        //max value in the 3D volume data set
        short max = Short.MIN_VALUE; //set to extreme values

        short read; //value read in
        int b1, b2; //data is wrong Endian (check wikipedia) for Java, so we need to swap the bytes around

        //store the 3D volume data set
        short[][][] cthead = new short[sliceCount][256][256]; //allocate the memory
        grey = new float[sliceCount][256][256];
        //loop through the data reading it in
        for (k = 0; k < sliceCount; k++) {
            for (j = 0; j < 256; j++) {
                for (i = 0; i < 256; i++) {
                    //because the endianness is wrong, it needs to be read byte at a time and swapped
                    b1 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
                    b2 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
                    read = (short) ((b2 << 8) | b1); //and swizzle the bytes around

                    if (read < min)
                        min = read; //update the minimum
                    if (read > max)
                        max = read; //update the maximum

                    cthead[k][j][i] = read; //put the short into memory
                }
            }
        }

        System.out.println(min + " " + max); //diagnostic - for CThead this should be -1117, 2248

        //(i.e. there are 3366 levels of grey, and now we will normalise them to 0-1 for display purposes
        //I know the min and max already, so I could have put the normalisation in the above loop, but I put it separate here
        for (k = 0; k < sliceCount; k++) {
            for (j = 0; j < 256; j++) {
                for (i = 0; i < 256; i++) {
                    grey[k][j][i] = ((float) cthead[k][j][i] - (float) min) / ((float) max - (float) min);
                }
            }
        }

    }

    public float[][][] getDataSet() {
        return grey;
    }

    public static float[][] getSlice(int slice) {
        return grey[slice];
    }

    //Gets an image from slice at given index
    public static Image getSlice(int sliceIndex, int size, MainController.ImageInterpolation interpolation, float gamma) {
        WritableImage image = new WritableImage(size, size);
        //Find the width and height of the image to be process
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        float val;

        //Get an interface to write to that image memory
        PixelWriter image_writer = image.getPixelWriter();

        int[] gammaLUT = new int[256];
        for (int i = 0; i < 256; i++) {
            gammaLUT[i] = (int) ((Math.pow(i / 255.0, 1 / gamma)) * 255);
        }

        //Iterate over all pixels
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                //Get resized image pixel value
                float y = (float) j * 256 / height;
                float x = (float) i * 256 / width;

                if (interpolation == MainController.ImageInterpolation.NEAREST_NEIGHBOUR) {
                    //Nearest neighbour interpolation
                    val = grey[sliceIndex][(int) y][(int) x];
                }
                else {
                    //Bilinear interpolation
                    float[] quadPixels = ImageFunctions.getQuadPixelsColours(getSlice(sliceIndex), x, y);

                    float newX = (float) (x - 0.5);
                    float a1 = newX - (int) newX;
                    float newY = (float) (y - 0.5);
                    float a2 = newY - (int) newY;

                    val = ImageFunctions.bilinearInterpolate(
                            quadPixels[0],
                            quadPixels[1],
                            quadPixels[2],
                            quadPixels[3],
                            a1, a2
                    );
                }

                //Apply gamma correction
                val = gammaLUT[Math.round(val * 255)] / 255.0f;

                //Apply the new colour
                image_writer.setColor(i, j, Color.color(val, val, val));
            }
        }

        return image;
    }
}

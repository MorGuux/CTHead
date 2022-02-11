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
    /**
     * 3D volume data set in floating point format.
     */
    private static float[][][] grey;

    /**
     * This method is used to read a dataset file.
     *
     * @param file       The file to read.
     * @param sliceCount The number of slices in the dataset.
     */
    public DataSet(final File file, final int sliceCount) {

        try {
            readData(file, sliceCount);
        }
        catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error reading file");
            alert.setContentText("The file could not be read");
            alert.showAndWait();
        }

    }

    /**
     * This method is used to get a slice of the 3D volume data set.
     *
     * @param slice The slice to get.
     * @return The slice of the 3D volume data set.
     */
    public static float[][] getSlice(final int slice) {
        return grey[slice];
    }

    /**
     * This method is used to get an image of a slice
     * from the 3D volume data set.
     *
     * @param sliceIndex    The slice to get.
     * @param size          The size of the image.
     * @param interpolation The interpolation method to use.
     * @param gamma         The gamma value to use.
     * @return The image of the slice.
     */
    public static Image getSlice(final int sliceIndex,
                                 final int size,
                                 final MainController.ImageInterpolation interpolation,
                                 final float gamma) {
        WritableImage image = new WritableImage(size, size);
        //Find the width and height of the image to be processed
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        float val;

        //Get an interface to write to that image memory
        PixelWriter imageWriter = image.getPixelWriter();

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
                } else {
                    //Bilinear interpolation
                    float[] quadPixels = ImageFunctions.getQuadPixelsColours(
                            getSlice(sliceIndex), x, y);

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
                imageWriter.setColor(i, j, Color.color(val, val, val));
            }
        }

        return image;
    }

    /**
     * This method is used to read a dataset file.
     *
     * @param file       The file to read.
     * @param sliceCount The number of slices in the dataset.
     * @throws IOException If the file cannot be read.
     */
    public void readData(final File file, final int sliceCount)
            throws IOException {

        //Read the data quickly via a buffer
        DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)));

        int i;
        int j;
        int k;

        final int sliceSize = 256;

        //min value in the 3D volume data set
        short min = Short.MAX_VALUE;
        //max value in the 3D volume data set
        short max = Short.MIN_VALUE; //set to extreme values

        short read; //value read in

        int b1;
        int b2; //data is wrong Endian, so we need to swap the bytes around

        //store the 3D volume data set
        short[][][] cthead =
                new short[sliceCount][sliceSize][sliceSize];

        //Allocate the memory
        grey = new float[sliceCount][sliceSize][sliceSize];
        //loop through the data reading it in
        for (k = 0; k < sliceCount; k++) {
            for (j = 0; j < sliceSize; j++) {
                for (i = 0; i < sliceSize; i++) {

                    b1 = ((int) in.readByte()) & 0xff;
                    b2 = ((int) in.readByte()) & 0xff;
                    read = (short) ((b2 << 8) | b1); //swizzle the bytes around

                    if (read < min) {
                        min = read;
                    }
                    if (read > max) {
                        max = read;
                    }

                    cthead[k][j][i] = read; //put the short into memory
                }
            }
        }

        //For CThead, this should be -1117, 2248
        System.out.println(min + " " + max);

        //Normalise grey values to 0-1 for display purposes
        for (k = 0; k < sliceCount; k++) {
            for (j = 0; j < sliceSize; j++) {
                for (i = 0; i < sliceSize; i++) {
                    grey[k][j][i] = ((float) cthead[k][j][i]
                            - (float) min) / ((float) max - (float) min);
                }
            }
        }

    }

    /**
     * This method is used to get the 3D volume data set.
     *
     * @return The 3D volume data set.
     */
    public float[][][] getDataSet() {
        return grey;
    }
}

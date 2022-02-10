/*
CS-255 Getting started code for the assignment
I do not give you permission to post this code online
Do not post your solution online
Do not copy code
Do not use JavaFX functions or other libraries to do the main parts of the assignment:
	1. Creating a resized image (you must implement nearest neighbour and bilinear interpolation yourself)
	2. Gamma correcting the image
	3. Creating the image which has all the thumbnails and event handling to change the larger image
All of those functions must be written by yourself
You may use libraries / IDE to achieve a better GUI
*/

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class test extends Application {
    short[][][] cthead; //store the 3D volume data set
    float[][][] grey; //store the 3D volume data set converted to 0-1 ready to copy to the image
    short min, max; //min/max value in the 3D volume data set
    ImageView TopView;
    int imageSize = 256; //size of the image
    ImageInterpolation activeInterpolation = ImageInterpolation.NEAREST_NEIGHBOUR;
    int activeSlice = 76;
    float activeGamma = 1;

    int[] gammaLUT;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws FileNotFoundException {
        stage.setTitle("CThead Viewer");

        try {
            ReadData();
        }
        catch (IOException e) {
            System.out.println("Error: The CThead file is not in the working directory");
            System.out.println("Working Directory = " + System.getProperty("user.dir"));
            return;
        }

        //int width=1024, height=1024; //maximum size of the image
        //We need 3 things to see an image
        //1. We need to create the image
        Image top_image = GetSlice(76, 256, ImageInterpolation.NEAREST_NEIGHBOUR, 1); //go get the slice image
        //2. We create a view of that image
        TopView = new ImageView(top_image); //and then see 3. below

        //Create the simple GUI
        final ToggleGroup group = new ToggleGroup();

        RadioButton rbNN = new RadioButton("Nearest neighbour");
        rbNN.setToggleGroup(group);
        rbNN.setSelected(true);

        RadioButton rbB = new RadioButton("Bilinear");
        rbB.setToggleGroup(group);

        Slider szSlider = new Slider(32, 1024, 256);

        Slider gammaSlider = new Slider(.1, 4, 1);

        //Radio button changes between nearest neighbour and bilinear
        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            public void changed(ObservableValue<? extends Toggle> ob, Toggle o, Toggle n) {

                if (rbNN.isSelected()) {
                    System.out.println("Nearest Neighbour selected");
                    activeInterpolation = ImageInterpolation.NEAREST_NEIGHBOUR;
                    updateImage();
                }
                else if (rbB.isSelected()) {
                    System.out.println("Bilinear selected");
                    activeInterpolation = ImageInterpolation.BILINEAR;
                    updateImage();
                }
            }
        });

        //Size of main image changes (slider)
        szSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number>
                                        observable, Number oldValue, Number newValue) {

                imageSize = newValue.intValue();
                System.out.println(imageSize);
                updateImage();
            }
        });

        //Gamma value changes
        gammaSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number>
                                        observable, Number oldValue, Number newValue) {

                activeGamma = newValue.floatValue();
                System.out.println(newValue.doubleValue());
                updateImage();
            }
        });

        VBox root = new VBox();

        //Add all the GUI elements
        //3. (referring to the 3 things we need to display an image)
        //we need to add it to the layout
        root.getChildren().addAll(rbNN, rbB, gammaSlider, szSlider, TopView);

        //Display to user
        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.show();

        ThumbWindow(scene.getX() + 200, scene.getY() + 200);
    }

    private void updateImage() {
        TopView.setImage(null); //clear the old image
        Image newImage = GetSlice(activeSlice, imageSize, activeInterpolation, activeGamma); //go get the slice image
        TopView.setImage(newImage); //Update the GUI so the new image is displayed
    }

    //Function to read in the cthead data set
    public void ReadData() throws IOException {
        //File name is hardcoded here - much nicer to have a dialog to select it and capture the size from the user
        File file = new File("CThead");
        //Read the data quickly via a buffer
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        //get number of slices in the scan
        int sliceCount = 113;

        int i, j, k; //loop through the 3D data set

        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE; //set to extreme values
        short read; //value read in
        int b1, b2; //data is wrong Endian (check wikipedia) for Java, so we need to swap the bytes around

        cthead = new short[sliceCount][256][256]; //allocate the memory - note this is fixed for this data set
        grey = new float[sliceCount][256][256];
        //loop through the data reading it in
        for (k = 0; k < sliceCount; k++) {
            for (j = 0; j < 256; j++) {
                for (i = 0; i < 256; i++) {
                    //because the endianness is wrong, it needs to be read byte at a time and swapped
                    b1 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
                    b2 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
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

    //Gets an image from slice at given index
    public Image GetSlice(int sliceIndex, int size, ImageInterpolation interpolation, float gamma) {
        WritableImage image = new WritableImage(size, size);
        //Find the width and height of the image to be process
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        float val;

        //Get an interface to write to that image memory
        PixelWriter image_writer = image.getPixelWriter();

        gammaLUT = new int[256];
        for (int i = 0; i < 256; i++) {
            gammaLUT[i] = (int) ((Math.pow(i / 255.0, 1 / gamma)) * 255);
        }

        //Iterate over all pixels
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                //Get resized image pixel value
                float y = (float) j * 256 / height;
                float x = (float) i * 256 / width;

                if (interpolation == ImageInterpolation.NEAREST_NEIGHBOUR) {
                    //Nearest neighbour interpolation
                    val = grey[sliceIndex][(int) y][(int) x];
                }
                else {
                    //Bilinear interpolation
                    float[] quadPixels = getQuadPixelsColours(x, y, sliceIndex);

                    float newX = (float) (x - 0.5);
                    float a1 = newX - (int) newX;
                    float newY = (float) (y - 0.5);
                    float a2 = newY - (int) newY;

                    val = bilinearInterpolate(
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

    /**
     * Gets the colour of the four pixels that are closest to the given x and y coordinates
     *
     * @param x          The x coordinate
     * @param y          The y coordinate
     * @param sliceIndex The slice index
     * @return An array of the four colours of the four pixels that are closest to the given x and y coordinates
     */
    private float[] getQuadPixelsColours(float x, float y, int sliceIndex) {
        // slice, Y, X
        if (x + 0.5 >= 256) {
            x = 255;
        }
        if (y + 0.5 >= 256) {
            y = 255;
        }
        float X1 = grey[sliceIndex][(int) (y - 0.5)][(int) (x - 0.5)];
        float X2 = grey[sliceIndex][(int) (y - 0.5)][(int) (x + 0.5)];
        float Y1 = grey[sliceIndex][(int) (y + 0.5)][(int) (x - 0.5)];
        float Y2 = grey[sliceIndex][(int) (y + 0.5)][(int) (x + 0.5)];
        return new float[]{X1, X2, Y1, Y2};
    }

    /**
     * Lerp between two values
     *
     * @param x1 first value
     * @param x2 second value
     * @param x  interpolation value
     * @return lerped value
     */
    public float lerp(float x1, float x2, float x) {
        return x1 * (1 - x) + x2 * x;
    }

    /**
     * Bilinear interpolation
     *
     * @param x1 first x value
     * @param x2 second x value
     * @param y1 first y value
     * @param y2 second y value
     * @param x  x interpolation value
     * @param y  y interpolation value
     * @return bilinear interpolated value
     */
    public float bilinearInterpolate(float x1,
                                     float x2,
                                     float y1,
                                     float y2,
                                     float x, float y) {
        float xLerp = lerp(x1, x2, x);
        float yLerp = lerp(y1, y2, x);
        return lerp(xLerp, yLerp, y);
    }

    public void ThumbWindow(double atX, double atY) {
        StackPane ThumbLayout = new StackPane();

        WritableImage thumb_image = new WritableImage(500, 500);
        ImageView thumb_view = new ImageView(thumb_image);
        ThumbLayout.getChildren().add(thumb_view);

        PixelWriter image_writer = thumb_image.getPixelWriter();

        int sliceCount = grey.length;

        int thumbnailSize = (int) (thumb_image.getWidth() / 12);
        int thumbnailGap = thumbnailSize / 10;

        int colCount = (int) (thumb_image.getWidth() / (thumbnailSize + thumbnailGap));

        /* For each slice, calculate the row and column for it, create a thumbnail using the GetSlice function,
        then draw it to the image in the correct position. */
        for (int i = 0; i < sliceCount; i++) {
            int sliceRow = i / colCount;
            int sliceCol = i % colCount;

            int imageX = sliceCol * (thumbnailSize + thumbnailGap);
            int imageY = sliceRow * (thumbnailSize + thumbnailGap);

            Image image = GetSlice(i, thumbnailSize, ImageInterpolation.BILINEAR, activeGamma);

            for (int y = 0; y < thumbnailSize; y++) {
                for (int x = 0; x < thumbnailSize; x++) {
                    image_writer.setColor(imageX + x, imageY + y, image.getPixelReader().getColor(x, y));
                }
            }
        }

        Scene ThumbScene = new Scene(ThumbLayout, thumb_image.getWidth(), thumb_image.getHeight());

        //Add mouse over handler
        thumb_view.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            int selectedSlice = (int) (event.getY() / (thumbnailSize + thumbnailGap)) * colCount
                        + (int) (event.getX() / (thumbnailSize + thumbnailGap));

            //Only update if the slice has changed
            if (selectedSlice != activeSlice) {
                System.out.println("Active Slice: " + (selectedSlice + 1));
                activeSlice = selectedSlice;
                updateImage();
            }

            event.consume();
        });

        //Build and display the new window
        Stage newWindow = new Stage();
        newWindow.setTitle("CThead Slices");
        newWindow.setScene(ThumbScene);

        // Set position of second window, related to primary window.
        newWindow.setX(atX);
        newWindow.setY(atY);

        newWindow.show();
    }

    enum ImageInterpolation {
        NEAREST_NEIGHBOUR,
        BILINEAR
    }

}
public class ImageFunctions {

    /**
     * Gets the colour of the four pixels that are closest to the given x and y coordinates
     *
     * @param grey       Greyscale pixel 2D array
     * @param x          The x coordinate
     * @param y          The y coordinate
     * @return An array of the four colours of the four pixels that are closest to the given x and y coordinates
     */
    public static float[] getQuadPixelsColours(float[][] grey, float x, float y) {
        // slice, Y, X
        if (x + 0.5 >= 256) {
            x = 255;
        }
        if (y + 0.5 >= 256) {
            y = 255;
        }
        float X1 = grey[(int) (y - 0.5)][(int) (x - 0.5)];
        float X2 = grey[(int) (y - 0.5)][(int) (x + 0.5)];
        float Y1 = grey[(int) (y + 0.5)][(int) (x - 0.5)];
        float Y2 = grey[(int) (y + 0.5)][(int) (x + 0.5)];
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
    public static float lerp(float x1, float x2, float x) {
        return x1 + (x2 - x1) * x;
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
    public static float bilinearInterpolate(float x1,
                                            float x2,
                                            float y1,
                                            float y2,
                                            float x, float y) {
        float xLerp = lerp(x1, x2, x);
        float yLerp = lerp(y1, y2, x);
        return lerp(xLerp, yLerp, y);
    }

}

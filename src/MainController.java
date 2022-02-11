import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static DataSet dataSet;
    private static float activeGamma = 1;
    private static int activeSlice = 76;
    private static int imageSize = 256; //size of the image
    private static ImageInterpolation activeInterpolation = ImageInterpolation.NEAREST_NEIGHBOUR;
    @FXML
    private ImageView imageView;
    @FXML
    private Slider gammaSlider;
    @FXML
    private Label gammaLabel;
    @FXML
    private Slider sizeSlider;
    @FXML
    private Label sizeLabel;
    @FXML
    private Label sliceLabel;
    @FXML
    private ToggleGroup interpolation;
    @FXML
    private RadioButton nearestNeighbor;
    @FXML
    private RadioButton bilinear;
    @FXML
    private Button loadButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        File file = new File("CThead");
        dataSet = new DataSet(file, 113);

        loadButton.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open CT Scan");
            File newFile = fileChooser.showOpenDialog(new Stage());
            if (newFile != null) {

                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Slice Count");
                dialog.setHeaderText("How many slices are in the scan?");
                dialog.setContentText("Enter the number of slices:");
                TextField inputTextField = new TextField();
                dialog.getDialogPane().setContent(inputTextField);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

                Optional<ButtonType> result = dialog.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    int sliceCount;
                    try {
                        sliceCount = Integer.parseInt(inputTextField.getText());
                    }
                    catch (NumberFormatException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Invalid slice count");
                        alert.setContentText("Please enter a valid number of slices");
                        alert.showAndWait();
                        return;
                    }
                    dataSet = new DataSet(newFile, sliceCount);
                }
            }
        });

        //Radio button changes between nearest neighbour and bilinear
        interpolation.selectedToggleProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue == nearestNeighbor) {
                System.out.println("Nearest Neighbour selected");
                activeInterpolation = ImageInterpolation.NEAREST_NEIGHBOUR;
                updateImage();
            }
            else if (newValue == bilinear) {
                System.out.println("Bilinear selected");
                activeInterpolation = ImageInterpolation.BILINEAR;
                updateImage();
            }
        });

        //Size slider changes the resized image size
        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            imageSize = newValue.intValue();
            sizeLabel.setText(Integer.toString(imageSize));
            updateImage();
        });

        //Gamma slider changes the gamma value of the image
        gammaSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            activeGamma = newValue.floatValue();
            DecimalFormat df = new DecimalFormat("#.##");
            gammaLabel.setText(df.format(activeGamma));
            updateImage();
        });



        Image top_image = DataSet.getSlice(76, 256, ImageInterpolation.BILINEAR, 1); //go get the slice image
        imageView.setImage(top_image); //set the image view to the slice image

        createThumbWindow(200, 200);
    }

    public void createThumbWindow(double atX, double atY) {
        StackPane ThumbLayout = new StackPane();

        WritableImage thumb_image = new WritableImage(500, 500);
        ImageView thumb_view = new ImageView(thumb_image);
        ThumbLayout.getChildren().add(thumb_view);

        PixelWriter image_writer = thumb_image.getPixelWriter();

        int sliceCount = dataSet.getDataSet().length;

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

            Image image = DataSet.getSlice(i, thumbnailSize, ImageInterpolation.BILINEAR, activeGamma);

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
                activeSlice = selectedSlice;
                sliceLabel.setText("Slice " + (activeSlice + 1) + " of " + (sliceCount));
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

    private void updateImage() {
        //Clear the old image
        imageView.setImage(null);
        //Get the slice image
        Image newImage = DataSet.getSlice(activeSlice, imageSize, activeInterpolation, activeGamma);
        //Re-size the image view to the new image dimensions
        imageView.setFitHeight(newImage.getHeight());
        imageView.setFitWidth(newImage.getWidth());
        //Set the new image
        imageView.setImage(newImage); //Update the GUI so the new image is displayed
    }

    enum ImageInterpolation {
        NEAREST_NEIGHBOUR,
        BILINEAR
    }
}
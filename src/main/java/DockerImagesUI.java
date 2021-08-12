/*
TODO: change to recall for changes or add a refresh button for image changes (or not).
TODO: Add registries configuration. (overall you need 3 client objects)
TODO: Keep localhost as an option and add it to the select registries button.
TODO: Catching the exception on deletion should raise a FORCE option
TODO: setDeleteLocalhostImage(), setDeleteDevImage(), setDeleteProdImage(), getLocalhostImage(), getDevImage()
TODO: getProdImage(), localhostConnection(), devConnection(), prodConnection()
 */
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import java.util.*;
import java.util.stream.Collectors;

public class DockerImagesUI {

    public static void main(String[] args) {
        Application.launch(ApplicationDockerImagesUI.class);
    }

    public static class ApplicationDockerImagesUI extends Application {

        Label appTitle = new Label("Docker Image Viewer & Cleaner");
        ImageView logo = new ImageView("file:./src/resources/logo.png");
        ImageView info = new ImageView("file:./src/resources/info.png");
        HBox hBoxTop = new HBox();
        HBox hBoxCenter = new HBox();
        GridPane gridPane = new GridPane();
        Button systemInfo = new Button();
        Button deleteImage = new Button("Delete...");
        BorderPane borderPane = new BorderPane();
        TableView<String> imageTable = new TableView<>();
        MenuItem selectRegistryDev = new MenuItem("Development");
        MenuItem selectRegistryProd = new MenuItem("Production");
        MenuItem selectRegistryLocalhost = new MenuItem("Localhost");
        MenuButton selectRegistryButton = new MenuButton("Select Registry...", null, selectRegistryDev, selectRegistryProd, selectRegistryLocalhost);
        ObservableList<String> testData1 = FXCollections.observableArrayList("test1", "test2");
        ObservableList<String> testData2 = FXCollections.observableArrayList("test3", "test4");
        ObservableList<String> imageNames = FXCollections.observableArrayList();

        public DockerClient localhostConnection() {

            DockerClientConfig localhostConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient localhostHttpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(localhostConfig.getDockerHost())
                    .build();

            DockerHttpClient.Request request = DockerHttpClient.Request
                    .builder()
                    .method(DockerHttpClient.Request.Method.GET)
                    .method(DockerHttpClient.Request.Method.GET)
                    .path("/_ping")
                    .build();

            DockerClient localhostDockerClient = DockerClientImpl.getInstance(localhostConfig, localhostHttpClient);

            return localhostDockerClient;
        }

        public List<Image> getLocalHostImages () {
            List<Image> localhostImages = localhostConnection().listImagesCmd().exec();
            return localhostImages;
        }

        public void setImageNames (List<Image> localhostImages) {
            for (Image image : localhostImages) {
                System.out.format("\t Image %s (%s)\n", image.getRepoTags()[0].replaceAll("(\\<|\\>)", "").replaceAll("(\\[|\\])", ""), image.getId());
                imageNames.add(String.valueOf(image.getRepoTags()[0]).replaceAll("(^\\[|\\]$)", "").replaceAll("(\\<|\\>)", ""));
//                if (imageNames.size() > 0) {
//                    imageNames.add(Arrays.toString(image.getRepoTags()).replaceAll("(^\\[|\\]|$)", ""));
//                } else {
//                    imageNames.add(image.getRepoTags()[0]);
//                }
            }
        }

        @Override
        public void start(Stage stage) {

            stage.setScene(initializeScene());
            stage.setTitle(appTitle.getText());
            stage.sizeToScene();
            stage.show();
        }

        public void infoAlert() {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, systemStats());
            alert.setTitle("System Properties");
            alert.setHeaderText("System Properties");
            alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> systemStats());
        }

        public String systemStats() {

            Map<String, String> systemStatsMap = new HashMap<>();

            systemStatsMap.put("Java Version: ", System.getProperty("java.version"));
            systemStatsMap.put("JavaFX Version: ", System.getProperty("javafx.version"));

            String systemStats = systemStatsMap
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining("\n"));

            return systemStats;
        }

        public void selectRegistryProd(){
            selectRegistryButton.setText(selectRegistryProd.getText());
            imageTable.setItems(testData1);
        }

        public void selectRegistryDev(){
            selectRegistryButton.setText(selectRegistryDev.getText());
            imageTable.setItems(testData2);
        }

        public void selectRegistryLocalhost() {
            selectRegistryButton.setText(selectRegistryLocalhost.getText());
            imageTable.setItems(imageNames);
        }

        public void setDeleteImage(ObservableList<String> selectedImages) throws DockerException{
            deleteImage.setOnAction(handler -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("WARNING");
                    alert.setHeaderText("Confirm to delete image...");
                    alert.showAndWait().filter(response -> response == ButtonType.CLOSE);
                    if (alert.getResult() == ButtonType.OK) {
                        localhostConnection().removeImageCmd(selectedImages.toString().replaceAll("(^\\[|\\]$)", "")).exec();
                        alert.setAlertType(Alert.AlertType.INFORMATION);
                        alert.setHeaderText("Image has been deleted.");
                        alert.showAndWait().filter(response -> response == ButtonType.CLOSE);
                    } else {
                        alert.setAlertType(Alert.AlertType.INFORMATION);
                        alert.setHeaderText("Image will not be deleted.");
                        alert.showAndWait().filter(response -> response == ButtonType.CLOSE);
                    }
                } catch (DockerException dockerException) {
                    System.out.println(dockerException.getMessage());
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("ERROR!");
                    alert.setHeaderText(dockerException.getMessage());
                    alert.showAndWait().filter(response -> response == ButtonType.CLOSE);
                }
            });
        }

        public void initializeTables() {

            try {
                this.setImageNames(getLocalHostImages());
            } catch (RuntimeException e) {
                if (e.getMessage() != null) {
                    System.out.println("WARNING: Is your local Docker running?\n"+e.getMessage());
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Pre-flight Check...");
                    alert.setHeaderText(e.getMessage()+"\n\nWARNING: Probably your local Docker is not running. If not, you will not be able to fetch images.");
                    alert.showAndWait().filter(response -> response == ButtonType.CLOSE);
                }
            }

            TableColumn<String, String> imageTableColumn = new TableColumn<>("Images");

            TableView.TableViewSelectionModel<String> selectionModel = imageTable.getSelectionModel();
            selectionModel.setSelectionMode(SelectionMode.SINGLE);
            ObservableList<String> selectedImages = selectionModel.getSelectedItems();

            imageTableColumn.setPrefWidth(975);

            imageTable.setPrefSize(975,900);
            imageTable.setPlaceholder(new Label("Start your local Docker and select a registry to fetch images..."));

            selectedImages.addListener((ListChangeListener<? super String>) (change) -> {
                System.out.println(change.getList().toString().replaceAll("(^\\[|\\]$)", ""));
                setDeleteImage(selectedImages);
            });

            imageTableColumn.setCellValueFactory((v) -> {
                String imageData = v.getValue();
                return new SimpleStringProperty(imageData);
            });

            imageTable.getColumns().add(imageTableColumn);

//            return selectedImages;
        }

        public void initializeMenu() {

            selectRegistryDev.setOnAction(event -> selectRegistryDev());
            selectRegistryProd.setOnAction(event -> selectRegistryProd());
            selectRegistryLocalhost.setOnAction(event -> selectRegistryLocalhost());

            systemInfo.setOnAction(event -> infoAlert());
            systemInfo.setGraphic(info);
            systemInfo.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            systemInfo.setTooltip(new Tooltip("Show system information."));

            gridPane.getChildren().addAll(selectRegistryButton,systemInfo,deleteImage);
            gridPane.setAlignment(Pos.TOP_RIGHT);
            GridPane.setMargin(systemInfo, new Insets(0, 0, 0, 200));
            GridPane.setMargin(selectRegistryButton, new Insets(0, 0, 0, 0));
            GridPane.setMargin(deleteImage, new Insets(0, 0, 0, -100));


            hBoxTop.setSpacing(100);
            hBoxTop.setPadding(new Insets(10, 10, 10, 10));
            hBoxTop.setStyle("-fx-background-color: #336699;");
            hBoxTop.getChildren().addAll(logo, gridPane);
            HBox.setHgrow(gridPane, Priority.ALWAYS);

            hBoxCenter.setSpacing(100);
            hBoxCenter.setPadding(new Insets(10, 10, 10, 10));

            hBoxCenter.getChildren().addAll(imageTable);
            hBoxCenter.setAlignment(Pos.CENTER);

            borderPane.setTop(hBoxTop);
            borderPane.setCenter(hBoxCenter);
        }

        public Scene initializeScene() {

            initializeMenu();
            initializeTables();

            Scene scene = new Scene(borderPane, 975, 840);
            return scene;
        }
    }
}

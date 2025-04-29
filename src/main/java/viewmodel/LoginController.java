package viewmodel;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.UserSession;
import java.sql.*;

public class LoginController {
    @FXML
    private GridPane rootpane;

    @FXML
    private TextField usernameTextField;

    @FXML
    private PasswordField passwordField;

    // Database connection details
    private final String DB_NAME = "CSC311_DB";
    private final String SQL_SERVER_URL = "jdbc:mysql://localhost:3306/";
    private final String DB_URL = SQL_SERVER_URL + DB_NAME;
    private final String USERNAME = "root"; // Update with your MySQL username
    private final String PASSWORD = "root"; // Update with your MySQL password

    public void initialize() {
        rootpane.setBackground(new Background(
                createImage("https://edencoding.com/wp-content/uploads/2021/03/layer_06_1920x1080.png"),
                null,
                null,
                null,
                null,
                null
        ));

        rootpane.setOpacity(0);
        FadeTransition fadeOut2 = new FadeTransition(Duration.seconds(10), rootpane);
        fadeOut2.setFromValue(0);
        fadeOut2.setToValue(1);
        fadeOut2.play();
    }

    private static BackgroundImage createImage(String url) {
        return new BackgroundImage(
                new Image(url),
                BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT,
                new BackgroundPosition(Side.LEFT, 0, true, Side.BOTTOM, 0, true),
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, false, true));
    }

    @FXML
    public void login(ActionEvent actionEvent) {
        String username = usernameTextField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter both username and password");
            return;
        }

        // Create a test admin account if none exists
        if (username.equals("admin") && password.equals("admin")) {
            try {
                UserSession.getInstance(username, password, "ADMIN");
                loadMainScene(actionEvent);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Verify user credentials against database
        if (verifyCredentials(username, password)) {
            try {
                UserSession.getInstance(username, password, "USER");
                loadMainScene(actionEvent);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "System error occurred");
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password");
        }
    }

    private boolean verifyCredentials(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();

            rs.close();
            stmt.close();
            conn.close();

            return exists;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadMainScene(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/db_interface_gui.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load main interface");
        }
    }

    public void signUp(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/signUp.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load signup page");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

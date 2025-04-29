package viewmodel;

import dao.DbConnectivityClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Person;
import service.UserSession;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static dao.DbConnectivityClass.*;

public class SignUpController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    private final DbConnectivityClass dbConnectivity = new DbConnectivityClass();

    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        if (!validateFields()) {
            return;
        }

        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            String sql = "INSERT INTO users (first_name, last_name, department, major, email, password, imageURL) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, usernameField.getText()); // using username as first name for now
            stmt.setString(2, ""); // last name
            stmt.setString(3, "New User"); // department
            stmt.setString(4, "CS"); // default major
            stmt.setString(5, usernameField.getText()); // email
            stmt.setString(6, passwordField.getText()); // password
            stmt.setString(7, ""); // no image URL

            stmt.executeUpdate();
            stmt.close();
            conn.close();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");
            goBack(actionEvent);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create account: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private boolean validateFields() {
        if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill in all fields");
            return false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showAlert(Alert.AlertType.ERROR, "Error", "Passwords do not match");
            return false;
        }

        return true;
    }

    @FXML
    public void goBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to return to login page");
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

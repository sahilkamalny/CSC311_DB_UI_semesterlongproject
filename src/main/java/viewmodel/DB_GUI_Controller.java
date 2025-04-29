package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Person;
import service.MyLogger;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class DB_GUI_Controller implements Initializable {

    @FXML
    TextField first_name, last_name, department, email, imageURL, searchField;
    @FXML
    ComboBox<Major> major_comboBox;
    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    @FXML
    private Button edit_button, delete_button, add_button;
    @FXML
    private MenuItem edit_menu_item, delete_menu_item;
    @FXML
    private Label statusLabel;

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();
    private FilteredList<Person> filteredData;

    public enum Major {
        CS("Computer Science"),
        CPIS("Computer Information Systems"),
        ENGLISH("English");

        private final String displayName;

        Major(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final class ValidationPatterns {
        static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]{2,30}$");
        static final Pattern DEPARTMENT_PATTERN = Pattern.compile("^[A-Za-z\\s&-]{2,50}$");
        static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?$");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            setupTableView();
            setupUIState();
            setupValidation();
            setupMenus();
            setupStatusBar();
            setupSearch();
            loadThemePreference();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTableView() {
        tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
        tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
        tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Enable editing
        tv.setEditable(true);
        tv_fn.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_ln.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_department.setCellFactory(TextFieldTableCell.forTableColumn());
        tv_email.setCellFactory(TextFieldTableCell.forTableColumn());

        filteredData = new FilteredList<>(data, p -> true);
        tv.setItems(filteredData);
    }

    private void setupUIState() {
        edit_button.setDisable(true);
        delete_button.setDisable(true);
        add_button.setDisable(true);
        edit_menu_item.setDisable(true);
        delete_menu_item.setDisable(true);

        major_comboBox.getItems().addAll(Major.values());
        major_comboBox.setValue(Major.CS);

        tv.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                updateUIState(newSelection));
    }

    private void setupValidation() {
        first_name.textProperty().addListener((obs, old, newValue) -> validateForm());
        last_name.textProperty().addListener((obs, old, newValue) -> validateForm());
        department.textProperty().addListener((obs, old, newValue) -> validateForm());
        email.textProperty().addListener((obs, old, newValue) -> validateForm());
        imageURL.textProperty().addListener((obs, old, newValue) -> validateForm());
        major_comboBox.valueProperty().addListener((obs, old, newValue) -> validateForm());
    }

    private void setupMenus() {
        Menu dataMenu = new Menu("Data");
        MenuItem importCSV = new MenuItem("Import CSV");
        MenuItem exportCSV = new MenuItem("Export CSV");

        importCSV.setOnAction(e -> importFromCSV());
        exportCSV.setOnAction(e -> exportToCSV());

        dataMenu.getItems().addAll(importCSV, exportCSV);
        menuBar.getMenus().add(dataMenu);
    }

    private void setupStatusBar() {
        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(person -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return person.getFirstName().toLowerCase().contains(lowerCaseFilter) ||
                        person.getLastName().toLowerCase().contains(lowerCaseFilter) ||
                        person.getEmail().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }

    private void updateUIState(Person selectedPerson) {
        boolean hasSelection = selectedPerson != null;
        edit_button.setDisable(!hasSelection);
        delete_button.setDisable(!hasSelection);
        edit_menu_item.setDisable(!hasSelection);
        delete_menu_item.setDisable(!hasSelection);

        if (hasSelection) {
            populateFields(selectedPerson);
        } else {
            clearForm();
        }
    }

    private void populateFields(Person person) {
        first_name.setText(person.getFirstName());
        last_name.setText(person.getLastName());
        department.setText(person.getDepartment());
        try {
            major_comboBox.setValue(Major.valueOf(person.getMajor().toUpperCase()));
        } catch (IllegalArgumentException e) {
            major_comboBox.setValue(Major.CS);
        }
        email.setText(person.getEmail());
        imageURL.setText(person.getImageURL());
        if (person.getImageURL() != null && !person.getImageURL().isEmpty()) {
            img_view.setImage(new Image(person.getImageURL()));
        }
    }

    @FXML
    protected void clearForm() {
        first_name.clear();
        last_name.clear();
        department.clear();
        major_comboBox.setValue(Major.CS);
        email.clear();
        imageURL.clear();
        img_view.setImage(new Image(getClass().getResourceAsStream("/images/profile.png")));
        validateForm();
        tv.getSelectionModel().clearSelection();
    }

    private void validateForm() {
        boolean isValid = isValidName(first_name.getText()) &&
                isValidName(last_name.getText()) &&
                isValidDepartment(department.getText()) &&
                isValidEmail(email.getText()) &&
                (imageURL.getText().isEmpty() || isValidURL(imageURL.getText())) &&
                major_comboBox.getValue() != null;

        add_button.setDisable(!isValid);
    }

    private boolean isValidName(String name) {
        return ValidationPatterns.NAME_PATTERN.matcher(name.trim()).matches();
    }

    private boolean isValidDepartment(String dept) {
        return ValidationPatterns.DEPARTMENT_PATTERN.matcher(dept.trim()).matches();
    }

    private boolean isValidEmail(String email) {
        return ValidationPatterns.EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isValidURL(String url) {
        return ValidationPatterns.URL_PATTERN.matcher(url.trim()).matches();
    }

    @FXML
    protected void addNewRecord() {
        if (add_button.isDisabled()) {
            return;
        }
        Person p = new Person(
                first_name.getText().trim(),
                last_name.getText().trim(),
                department.getText().trim(),
                major_comboBox.getValue().name(),
                email.getText().trim(),
                imageURL.getText().trim()
        );
        cnUtil.insertUser(p);
        p.setId(cnUtil.retrieveId(p));
        data.add(p);
        clearForm();
        showStatus("Record added successfully");
    }

    @FXML
    protected void editRecord() {
        Person selectedPerson = tv.getSelectionModel().getSelectedItem();
        if (selectedPerson != null) {
            Person updatedPerson = new Person(
                    selectedPerson.getId(),
                    first_name.getText().trim(),
                    last_name.getText().trim(),
                    department.getText().trim(),
                    major_comboBox.getValue().name(),
                    email.getText().trim(),
                    imageURL.getText().trim()
            );
            cnUtil.editUser(selectedPerson.getId(), updatedPerson);
            int index = data.indexOf(selectedPerson);
            data.set(index, updatedPerson);
            showStatus("Record updated successfully");
        }
    }

    @FXML
    protected void deleteRecord() {
        Person selectedPerson = tv.getSelectionModel().getSelectedItem();
        if (selectedPerson != null) {
            cnUtil.deleteRecord(selectedPerson);
            data.remove(selectedPerson);
            clearForm();
            showStatus("Record deleted successfully");
        }
    }

    @FXML
    protected void importFromCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showOpenDialog(tv.getScene().getWindow());
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                reader.readLine(); // Skip header
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    Person person = new Person(
                            values[0].trim(),
                            values[1].trim(),
                            values[2].trim(),
                            values[3].trim(),
                            values[4].trim(),
                            values.length > 5 ? values[5].trim() : ""
                    );
                    cnUtil.insertUser(person);
                    data.add(person);
                }
                showStatus("CSV import completed successfully");
            } catch (IOException e) {
                showStatus("Error importing CSV: " + e.getMessage());
            }
        }
    }

    @FXML
    protected void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(tv.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("FirstName,LastName,Department,Major,Email,ImageURL");
                for (Person person : data) {
                    writer.println(String.format("%s,%s,%s,%s,%s,%s",
                            person.getFirstName(),
                            person.getLastName(),
                            person.getDepartment(),
                            person.getMajor(),
                            person.getEmail(),
                            person.getImageURL()));
                }
                showStatus("CSV export completed successfully");
            } catch (IOException e) {
                showStatus("Error exporting CSV: " + e.getMessage());
            }
        }
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> statusLabel.setText("Ready"));
                }
            }, 3000);
        }
    }

    @FXML
    protected void showImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        File file = fileChooser.showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            String url = file.toURI().toString();
            imageURL.setText(url);
            img_view.setImage(new Image(url));
        }
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        Platform.exit();
    }

    @FXML
    protected void displayAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Student Registration System");
        alert.setContentText("Version 1.0\nDeveloped for CSC311");
        alert.showAndWait();
    }

    private void loadThemePreference() {
        Preferences prefs = Preferences.userRoot().node("com.studentreg.preferences");
        String theme = prefs.get("THEME", "light");
        if (theme.equals("dark")) {
            darkTheme(null);
        }
    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Preferences.userRoot().node("com.studentreg.preferences").put("THEME", "light");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
            Preferences.userRoot().node("com.studentreg.preferences").put("THEME", "dark");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

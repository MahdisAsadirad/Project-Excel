module org.example.excel {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.example.excel;


    opens org.example.excel to javafx.fxml;
    exports org.example.excel;
    exports org.example.excel.view;
    opens org.example.excel.view to javafx.fxml;
}
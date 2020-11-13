package fr.ulille.phlam.cat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.controlsfx.control.StatusBar;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private static final String CDMS_URL = "https://cdms.astro.uni-koeln.de/classic/entries/";
    private static final String JPL_URL = "https://spec.jpl.nasa.gov/ftp/pub/catalog/";

    @FXML TableView<CatTableEntry> table;
    @FXML Button getButton;
    @FXML TextField filterTextField;
    @FXML TextField massFilterField;
    @FXML StatusBar statusBar;
    ObservableList<CatTableEntry> entries = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<CatTableEntry,Integer> tagColumn = new TableColumn<>("Tag");
        tagColumn.setCellValueFactory(cell->cell.getValue().molTagProperty());
        table.getColumns().add(tagColumn);

        TableColumn<CatTableEntry,String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell->cell.getValue().molNameProperty());
        nameColumn.setPrefWidth(180);
        table.getColumns().add(nameColumn);

        TableColumn<CatTableEntry,Boolean> checkedColumn = new TableColumn<>("Get");
        checkedColumn.setCellValueFactory(cell->cell.getValue().isSelectedProperty());
        checkedColumn.setCellFactory(c->new CheckBoxTableCell<>());
        checkedColumn.setEditable(true);

        table.getColumns().add(checkedColumn);

        FilteredList<CatTableEntry> filteredEntries = new FilteredList<>(entries,p->true);
        filterTextField.textProperty().addListener((observable,oldv, newv)->
            filteredEntries.setPredicate(p->{
                if (newv==null||newv.isEmpty()) return true;
                return p.molNameProperty().getValue().contains(newv);
            }));

        massFilterField.textProperty().addListener(((observable, oldValue, newValue) ->
            filteredEntries.setPredicate(p->{
                if (newValue==null||newValue.isEmpty()) return true;
                int value = 1;
                try{
                    value = Integer.parseInt(newValue);
                } catch (NumberFormatException e){
                    e.printStackTrace();
                }
                return p.molTagProperty().getValue() / 1000 == value;
            })));

        SortedList<CatTableEntry> sortedEntries = new SortedList<>(filteredEntries);
        sortedEntries.comparatorProperty().bind(table.comparatorProperty());

        table.setItems(sortedEntries);
        table.setEditable(true);
        table.getSortOrder().add(tagColumn);

        statusBar.textProperty().setValue("");

        getButton.setOnAction(event -> {
            List<String> list = new ArrayList<>();
            Task<Void> dltask = new Task<Void>() {
                @Override
                protected Void call() {
                    sortedEntries.forEach(entry->{
                        if (entry.isSelected()){
                            String url = String.format("c%06d.cat",entry.molTagProperty().getValue());
                            updateMessage("Downloading "+url+" ...");
                            if (entry.getOrigin()>0){
                                url = CDMS_URL+url;
                            } else {
                                url = JPL_URL+url;
                            }
                            list.add(entry.molNameProperty().getValue());
                            getFile(url);
                            updateMessage("");
                        }
                    });
                    return null;
                }
            };

            dltask.setOnSucceeded(e->{
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText(null);
                alert.setContentText("Finished downloading entries: "+String.join(", ",list));
                alert.setTitle("Downloading finished");
                alert.show();
            });

            statusBar.textProperty().bind(dltask.messageProperty());

            Thread thread = new Thread(dltask);
            thread.setDaemon(true);
            thread.start();
        });

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    updateMessage("Reading CDMS data...");
                    Document cdoc = Jsoup.connect(CDMS_URL).get();
                    Elements rows = cdoc.select("tr");

                    for (Element row : rows){
                        Elements columns = row.select("td");
                        if (columns.size()>0){
                            int tag = Integer.parseInt(columns.get(0).text());
                            String name = columns.get(1).text();
                            entries.add(new CatTableEntry(tag,name,false));
                        }
                    }

                    updateMessage("Reading JPL data...");
                    Document jdoc = Jsoup.connect(JPL_URL+"catdir.html").get();
                    Elements tabs = jdoc.select("tr");
                    Elements pre = tabs.select("pre");
                    String[] ss = pre.first().html().split("\\n");

                    for  (int i=1;i<ss.length;i++){
                        String[] sf = ss[i].split("\\s+");
                        int tag;
                        String name;
                        if (sf[0].trim().isEmpty()){
                            tag = Integer.parseInt(sf[1]);
                            name = sf[2];
                        }
                        else {
                            tag = Integer.parseInt(sf[0]);
                            name = sf[1];
                        }
                        entries.add(new CatTableEntry(tag,name,false));
                    }

                    updateMessage("Done. Total entries : "+entries.size());


                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        statusBar.textProperty().bind(task.messageProperty());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    } // initialize

    private void getFile(String url){
        try {
            String home = System.getProperty("user.home");
            String[] s = url.split("/");
            String outputFileName = s[s.length-1];
            InputStream in = new URL(url).openStream();
            ReadableByteChannel readableByteChannel = Channels.newChannel(in);
            FileOutputStream fileOutputStream = new FileOutputStream(home+"/Downloads/"+outputFileName);
            fileOutputStream.getChannel()
                    .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

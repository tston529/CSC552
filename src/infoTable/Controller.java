package infoTable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a class used to manage the functions of the GUI, effectively
 * "controlling" each visual object in the program.
 */
public class Controller{
    @FXML private TableView<HealthData> table;
    private ObservableList<HealthData> hdList;

    @FXML private TableColumn idCol;

    @FXML private TableColumn nameCol;

    @FXML private TableColumn typeCol;

    @FXML private TableColumn yearCol;

    @FXML private TableColumn variableCol;

    @FXML private TableColumn valueCol;

    @FXML private RadioButton lte;

    @FXML private RadioButton equ;

    @FXML private RadioButton gte;

    @FXML private RadioButton noneRadio;

    @FXML private ToggleGroup radioToggle = new ToggleGroup();

    @FXML private ChoiceBox variableDropdown;

    @FXML private ComboBox stateCombo;

    @FXML private Slider valueSlider;

    private final String apiBase = "https://odn.data.socrata.com/resource/csbu-um39.json?";

    private final List<String> states = new ArrayList<>(Arrays.asList(
            "(State)", "AL", "AK", "AR", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS",
            "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC",
            "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    ));

    public  Controller(){}

    /**
     * Performs tasks on startup: links radio buttons together in a group,
     * preps table columns for holding HealthData-specific data, sets dropdown
     * menu data.
     */
    @FXML private void initialize()
    {
        noneRadio.setUserData("none");
        lte.setUserData("lte");
        equ.setUserData("equ");
        gte.setUserData("gte");
        noneRadio.setToggleGroup(radioToggle);
        lte.setToggleGroup(radioToggle);
        equ.setToggleGroup(radioToggle);
        gte.setToggleGroup(radioToggle);
        noneRadio.setSelected(true);

        variableDropdown.setItems(FXCollections.observableArrayList(
            "(Variable Topic)", "population", "exercise", "obesity", "smoking", "pollution", "water", "alcohol", "mortality", "poverty", "homicide"
        ));
        variableDropdown.getSelectionModel().selectFirst();
        stateCombo.setValue("(State)");

        stateCombo.setItems(FXCollections.observableArrayList(states));

        idCol.setCellValueFactory(
                new PropertyValueFactory<HealthData, String>("id"));
        nameCol.setCellValueFactory(
                new PropertyValueFactory<HealthData, String>("name"));
        typeCol.setCellValueFactory(
                new PropertyValueFactory<HealthData, String>("type"));
        yearCol.setCellValueFactory(
                new PropertyValueFactory<HealthData, Integer>("year"));
        variableCol.setCellValueFactory(
                new PropertyValueFactory<HealthData, String>("variable"));
        valueCol.setCellValueFactory(
                new PropertyValueFactory<HealthData, String>("value"));
        //radioToggle.selectedToggleProperty().addListener((observable, oldVal, newVal) -> System.out.println(newVal + " was selected"));
    }

    /**
     * Calls methods to generate a query string based on user input,
     * passes it into the readUrl method, sets the resulting data into
     * the table.
     * @param event the user's action that triggered the function call
     */
    @FXML private void submitQuery(ActionEvent event)
    {
        String query = parseQuery();
        readUrl(query);
        table.setItems(hdList);
    }

    /**
     * Looks at the user's selections, builds a query around them.
     * @return the newly-built query
     */
    private String parseQuery()
    {
        int where = 0;
        StringBuilder queryString = new StringBuilder(apiBase);

        // If the user selected a state
        if(stateCombo.getValue() != "(State)") {
            queryString.append("$where=name%20like%20%27%25");
            queryString.append(stateCombo.getValue());
            queryString.append("%25%27");
            where++;
        }
        // Variable topics, in this dataset, is quite expansive.  I selected a small
        // "variable" handful to choose from, but since there is tons of data and
        // many unique strings, for now, the user selects a broad topic and lets the
        // query bring back every piece of data with that topic in its "variable"
        // string.
        if(variableDropdown.getValue() != "(Variable Topic)") {
            if(where==0)
                queryString.append("$where=variable%20like%20%27%25");
            else
                queryString.append("%20AND%20variable%20like%20%27%25");
            queryString.append(variableDropdown.getValue());
            queryString.append("%25%27");
            where++;
        }
        // The radio buttons allow for selecting equality operators tied
        // to the data's value and the selected focal point in the slider.
        if(radioToggle.getSelectedToggle().getUserData() != "none") {
            if(where==0)
                queryString.append("$where=");
            else
                queryString.append("%20AND%20");
            if(radioToggle.getSelectedToggle().getUserData() == "lte")
                queryString.append("value<=");
            if(radioToggle.getSelectedToggle().getUserData() == "equ")
                queryString.append("value==");
            if(radioToggle.getSelectedToggle().getUserData() == "gte")
                queryString.append("value>=");
            queryString.append(valueSlider.getValue());
            where++;
        }
        return queryString.toString();
    }

    /**
     * Modified version of method by Dr.&nbsp;Spiegel; returns the result of
     * a visit to a desired URL in plain text, parses returned JSON, saves to
     * a *.CSV file, and imports the final data from this file.
     * @param query the full URL to the desired data
     */
    private void readUrl(String query)
    {
        URL TheFile=null;
        try {	// Set up a URL to the file
            TheFile=new URL(query);
        }
        catch (Exception e) {
            System.err.println("URL Setup failed...");
            e.printStackTrace();
        }
        InputStream s=null;
        try { // Hook up to the file on the server
            s=TheFile.openStream();
        }
        catch (Exception e)  {
            e.printStackTrace();
            System.err.println("!! Stream open failed !!");
        }
        BufferedReader Inf=null;
        try {
            Inf=new BufferedReader(new InputStreamReader(s));
        }
        catch (Exception e){
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        int next;
        try {
            next = Inf.read();
            while (next >= 0) {
                sb.append((char) next);
                next = Inf.read();
            }
        } catch(IOException e){}

        try {
            List<HealthData> list = parse(sb.toString());
            String filename = "healthdata.csv";
            toCsv(list, filename);
            hdList = FXCollections.observableArrayList(readCsv(filename));

        } catch(Exception e){}
    }

    /**
     * Imports a String of JSON data and breaks it down into its
     * component parts, creating a list of HealthCare objects
     * each representing one line of the results.
     * @param jsonLine the line of JSON data returned from the query call
     * @return a list of HealthData objects each containing one "table row" of the parsed JSON data
     */
    public List parse(String jsonLine)
    {
        JsonParser parser = new JsonParser();
        JsonElement tradeElement = parser.parse(jsonLine);
        JsonArray jarray = tradeElement.getAsJsonArray();

        List<HealthData> list = new ArrayList<>();

        for (int i = 0; i < jarray.size()-1; i++) {
            JsonObject jsonobject = jarray.get(i).getAsJsonObject();
            String id = jsonobject.get("id").getAsString();
            String name = jsonobject.get("name").getAsString().replace(",", "");
            String type = jsonobject.get("type").getAsString();
            String year = jsonobject.get("year").getAsString();
            String variable = jsonobject.get("variable").getAsString();
            String value = jsonobject.get("value").getAsString();
            list.add(new HealthData(id, name, type, Integer.valueOf(year), variable, Float.valueOf(value)));

        }
        return list;
    }

    /**
     * Writes the result's row's attributes to a csv file for each
     * row-object in the passed-in list.
     * @param list an array of row-objects, each containing the HealthData attributes
     *             for one returned row in the results.
     * @param filename the name of the file which will be created and written to.
     * @throws FileNotFoundException
     */
    public void toCsv(List<HealthData> list, String filename) throws FileNotFoundException
    {
        PrintWriter pw = new PrintWriter(new File(filename));
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,type,year,variable,value\n");

        for(int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).getId());
            sb.append(",");
            sb.append(list.get(i).getName());
            sb.append(",");
            sb.append(list.get(i).getType());
            sb.append(",");
            sb.append(list.get(i).getYear());
            sb.append(",");
            sb.append(list.get(i).getVariable());
            sb.append(",");
            sb.append(list.get(i).getValue());
            sb.append("\n");
        }

        pw.write(sb.toString());
        pw.close();


    }

    /**
     * Imports a CSV file and generates a list containing HealthData objects,
     * each containing the data for one resulting row.
     * @param filename the name of the file to read
     * @return the list of HealthData objects
     * @throws IOException
     */
    public List<HealthData> readCsv(String filename) throws IOException {
        List<HealthData> hdList = new ArrayList<>();
        try{
            BufferedReader br = new BufferedReader(new FileReader(filename));
            br.readLine(); //Skip first line (the header info)

            String line;
            String[] splitData;
            while ((line = br.readLine()) != null) {
                splitData = line.split(",");
                hdList.add(new HealthData(splitData[0], splitData[1], splitData[2], Integer.valueOf(splitData[3]), splitData[4], Float.valueOf(splitData[5])));
            }
        } catch (FileNotFoundException e) { }
        return hdList;
    }

}



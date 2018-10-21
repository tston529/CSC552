package infoTable;

/**
 * A HealthData Object will contain data pertinent to a single row of results
 * from any given query, and will be used to populate the final table.
 */
public class HealthData {
    private String id;
    private String name;
    private String type;
    private int year;
    private String variable;
    private float value;


    /**
     * Constructor; only sets values.
     * @param hdId Row's unique ID number
     * @param hdName Row's county and/or state
     * @param hdType Whether the row is for the county or the state
     * @param hdYear The year from which this row's data was accurate
     * @param hdVar Contains the general topic of what the row's data is about
     * @param hdVal A numerical value associated with the row
     */
    public HealthData(String hdId, String hdName, String hdType,int hdYear, String hdVar, float hdVal) {
        this.id = hdId;
        this.name = hdName;
        this.type = hdType;
        this.year = hdYear;
        this.variable = hdVar;
        this.value = hdVal;
    }

    /* Accessors */

    /**
     * Returns the row's ID
     * @return the row's ID
     */
    public String getId() { return id; }

    /**
     * Returns the row's associated county/state
     * @return row's associated county/state
     */
    public String getName() { return name; }

    /**
     * Returns whether this was for a county or state
     * @return whether this was for a county or state
     */
    public String getType() { return type; }

    /**
     * Returns the year of the data's accumulation
     * @return year associated with the acquisition of the row's data
     */
    public int getYear() { return year; }

    /**
     * Returns the string containing the general topic of the row
     * @return the variable data (contains the general topic of the row)
     */
    public String getVariable() { return variable; }

    /**
     * Returns the value associated with the row
     * @return the numerical value associated with the variable
     */
    public float getValue() { return value; }

    /* Mutators */
    private void setId(String hdId) { id = hdId; }

    private void setName(String hdName) { name = hdName; }

    private void setType(String hdType) { type = hdType; }

    private void setYear(int hdYear) { year = hdYear; }

    private void setVariable(String hdVar) { variable = hdVar; }

    private void setValue(float hdVal) { value = hdVal; }
}
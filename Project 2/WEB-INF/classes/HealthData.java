/**
 * A HealthData Object will contain data pertinent to a single row of results
 * from any given query, and will be used to populate the final table.
 */
public abstract class HealthData {
    protected String id;
    protected String type;
    protected int year;
    protected String description;
    protected float rawValue;
    protected String location;
    protected String value;
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
    public String getLocation() { return location; }

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
     * @return the description data (contains the general topic of the row)
     */
    public String getDescription() { return description; }

    /**
     * Returns the value associated with the row
     * @return the numerical value associated with the description
     */
    public String getValue() { return value; }

    /**
     * Returns the value associated with the row
     * @return the numerical value associated with the description
     */
    public float getRawValue() { return rawValue; }

    /**
     * Returns the type of object, for table management purposes
     * @return the type of HealthData object the object is
     */
    public abstract String getObjectType();

    /* Mutators */
    public abstract void setId(String hdId);

    public abstract void setLocation(String hdLocation);

    public abstract void setType(String hdType);

    public abstract void setYear(int hdYear);

    public abstract void setDescription(String hdDesc);

    public abstract void setValue(float hdVal);
}
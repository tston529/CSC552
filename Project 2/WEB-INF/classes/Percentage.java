/**
 * A HealthData Object will contain data pertinent to a single row of results
 * from any given query, and will be used to populate the final table.
 */
public class Percentage extends HealthData {
    /**
     * Constructor; only sets values.
     * @param hdId Row's unique ID number
     * @param hdLocation Row's county and/or state
     * @param hdType Whether the row is for the county or the state
     * @param hdYear The year from which this row's data was accurate
     * @param hdDesc Contains the general topic of what the row's data is about
     * @param hdVal A numerical value associated with the row
     */


    public Percentage(String hdLoc, float hdVal) {
        this.location = hdLoc;
        setValue(hdVal);
    }

    public Percentage(String hdLoc, float hdNum, float hdDen) {
        this.location = hdLoc;
        setValue(hdNum, hdDen);
    }

    public Percentage(String hdId, String hdLocation, String hdType,int hdYear, String hdDesc, float hdVal) {
        this.id = hdId;
        this.location = hdLocation;
        this.type = hdType;
        this.year = hdYear;
        this.description = hdDesc;
        setValue(hdVal);
    }

    public Percentage(String hdId, String hdLocation, String hdType,int hdYear, String hdDesc, float hdNumerator, float hdDenominator) {
        this.id = hdId;
        this.location = hdLocation;
        this.type = hdType;
        this.year = hdYear;
        this.description = hdDesc;
        setValue(hdNumerator, hdDenominator);
    }

    /* Accessors */

    @Override
    public String getObjectType() { return "Percentage"; };


    /* Mutators */
    @Override
    public void setId(String hdId) { id = hdId; }

    @Override
    public void setLocation(String hdLocation) { location = hdLocation; }
    
    @Override
    public void setType(String hdType) { type = hdType; }
    
    @Override
    public void setYear(int hdYear) { year = hdYear; }
    
    @Override
    public void setDescription(String hdDesc) { description = hdDesc; }
    
    @Override
    public void setValue(float hdVal) { 
        this.rawValue = hdVal;
        value = Float.toString(100*hdVal) + "%";
    }
    
    public void setValue(float hdNum, float hdDenom) { setValue(hdNum/hdDenom); }
}
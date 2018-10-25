import java.util.*;
public class StatesData {

    private static final List<String> names = new ArrayList<>(Arrays.asList(
        "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", 
        "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
        "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island",
        "South Carolina","South Dakota","Tennessee","Texas","Utah","Vermont","Virginia","Washington","West Virginia","Wisconsin","Wyoming"
    ));

    private static final List<String> abbr = new ArrayList<>(Arrays.asList(
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", 
        "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV",
        "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI",
        "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    ));

    public static HashMap<String, String> getStateToAbbrMap() {
        HashMap<String, String> states = new HashMap<String, String>();
        for(int i = 0; i < names.size(); i++) {
            states.put(names.get(i), abbr.get(i));
        }
        return states;
    }

    public static HashMap<String, String> getAbbrToStateMap() {
        HashMap<String, String> states = new HashMap<String, String>();
        for(int i = 0; i < names.size(); i++) {
            states.put(abbr.get(i), names.get(i));
        }
        return states;
    }

    public static ArrayList<String> getStateNames() {
        return new ArrayList(names);
    }

    public static ArrayList<String> getStateAbbr() {
        return new ArrayList(abbr);
    }
}

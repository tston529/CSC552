public class HealthDataFactory {

    public static HealthData getHealthData(String hdId, String hdLocation, String hdType,int hdYear, String hdDesc, float hdVal) {
        if(hdType.indexOf("numerator")!=-1 || hdType.indexOf("denominator")!=-1) {
            return new Percentage(hdId, hdLocation, hdType, hdYear, hdDesc, hdVal);
        }
        else if(hdVal < 1 && hdVal >= 0) {
            return new Percentage(hdId, hdLocation, hdType, hdYear, hdDesc, hdVal);
        } else {
            return new Population(hdId, hdLocation, hdType, hdYear, hdDesc, hdVal);
        }
    }

    public static HealthData getHealthData(String hdId, String hdLocation, String hdType,int hdYear, String hdDesc, float hdNumerator, float hdDenominator) {
        return new Percentage(hdId, hdLocation, hdType, hdYear, hdDesc, hdNumerator, hdDenominator);
    }

    public static HealthData getHealthData(String hdLocation, float hdNumerator, float hdDenominator) {
        return new Percentage(hdLocation, hdNumerator, hdDenominator);
    }

    public static HealthData getHealthData(String hdLocation, float hdVal) {
        if(hdVal < 1 && hdVal >= 0) {
            return new Percentage(hdLocation, hdVal);
        }
        return new Population(hdLocation, hdVal);
    }

}
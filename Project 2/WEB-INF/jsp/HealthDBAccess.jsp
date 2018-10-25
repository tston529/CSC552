<%@page language="java" contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
    <p>poop</p>
        <div Align=Center>
            <form name='loginForm' method='post' action='${pageContext.request.contextPath}/HealthDBAccess'>
                <select name='state' size='1' required='true'>
                    <option value='(State)' selected>(SELECT STATE)</option>
                    <option value='AL'>Alabama</option>
                    <option value='AK'>Alaska</option>
                    <option value='AZ'>Arizona</option>
                    <option value='AR'>Arkansas</option>
                    <option value='CA'>California</option>
                    <option value='CO'>Colorado</option>
                    <option value='CT'>Connecticut</option>
                    <option value='DE'>Delaware</option>
                    <option value='FL'>Florida</option>
                    <option value='GA'>Georgia</option>
                    <option value='HI'>Hawaii</option>
                    <option value='ID'>Idaho</option>
                    <option value='IL'>Illinois</option>
                    <option value='IN'>Indiana</option>
                    <option value='IA'>Iowa</option>
                    <option value='KS'>Kansas</option>
                    <option value='KY'>Kentucky</option>
                    <option value='LA'>Louisiana</option>
                    <option value='ME'>Maine</option>
                    <option value='MD'>Maryland</option>
                    <option value='MA'>Massachusetts</option>
                    <option value='MI'>Michigan</option>
                    <option value='MN'>Minnesota</option>
                    <option value='MS'>Mississippi</option>
                    <option value='MO'>Missouri</option>
                    <option value='MT'>Montana</option>
                    <option value='NE'>Nebraska</option>
                    <option value='NV'>Nevada</option>
                    <option value='NH'>New Hampshire</option>
                    <option value='NJ'>New Jersey</option>
                    <option value='NM'>New Mexico</option>
                    <option value='NY'>New York</option>
                    <option value='NC'>North Carolina</option>
                    <option value='ND'>North Dakota</option>
                    <option value='OH'>Ohio</option>
                    <option value='OK'>Oklahoma</option>
                    <option value='OR'>Oregon</option>
                    <option value='PA'>Pennsylvania</option>
                    <option value='RI'>Rhode Island</option>
                    <option value='SC'>South Carolina</option>
                    <option value='SD'>South Dakota</option>
                    <option value='TN'>Tennessee</option>
                    <option value='TX'>Texas</option>
                    <option value='UT'>Utah</option>
                    <option value='VT'>Vermont</option>
                    <option value='VA'>Virginia</option>
                    <option value='WA'>Washington</option>
                    <option value='WV'>West Virginia</option>
                    <option value='WI'>Wisconsin</option>
                    <option value='WY'>Wyoming</option>
                </select>

                <select name='topic' required='true'>
                    <option value='(Variable Topic)'>(SELECT VARIABLE TOPIC)</option>
                    <option value='premature_death'>Premature Death</option>
                    <option value='preventable_hospitable_stays'>Preventable Hospital Stays</option>
                    <option value='primary_care_physicians'>Primary Care Physicians</option>
                    <option value='severe_housing_problems'>Severe Housing Problems</option>
                    <option value='sexually_transmitted_infections'>Sexually Transmitted Infections</option>
                    <option value='teen_births'>Teen Births</option>
                    <option value='unemployment'>Unemployment</option>
                    <option value='violent_crime'>Violent Crime</option>
                    <option value='diabetes'>Diabetes</option>
                    <option value='adult_smoking'>Adult Smoking</option>
                    <option value='child_mortality'>Child Mortality</option>
                    <option value='access_to_exercise'>Access to Exercise</option>
                    <option value='alcohol_impaired_driving_deaths'>Alcohol Impaired Driving Deaths</option>
                    <option value='children_in_poverty'>Children in Poverty</option>
                    <option value='2011_population_estimate'>2011 Population Estimate</option>
                </select>
                
                <br/>

                <input type='radio' name='regionSize' value='state' required='true'>State-wide
                <input type='radio' name='regionSize' value='county' required='true' checked>Per-County

                <br/>

                <input type='submit' value='Submit' />
            </form>
        </div>
    </body>
</html>
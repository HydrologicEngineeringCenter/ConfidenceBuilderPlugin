import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class PropertyFileReader {
    public static Properties Read(String _propertiesFile) {
        //variables
        double endProb = 0;
        ArrayList<Double> weights = new ArrayList<>();
        double startProb = 0;
        double incrementalWeightTemp;
        String _simulationName = "";
        ArrayList<Double> _xOrds = new ArrayList<>();
        ArrayList<Double> _CI_Values = new ArrayList<>();
        BufferedReader brp = null;

        try {
            brp = new BufferedReader(new FileReader(_propertiesFile));
            String[] tmp;
            String propertyLine;
            while ((propertyLine = brp.readLine()) != null) {
                tmp = propertyLine.split(",");
                if (tmp.length == 0) {
                    continue;
                }
                if (tmp[0].equals("SimulationName") ){_simulationName = tmp[1];}
                if (tmp[0] .equals( "XOrds")) {
                    for (String s : tmp) {
                            if (s.equals("XOrds")) { continue;}
                            _xOrds.add( Double.parseDouble(s));
                        }
                    }
                if (tmp[0] .equals( "CI_Vals")){
                        for (String s : tmp) {
                            if (s.equals("CI_Vals")) { continue;}
                            _CI_Values.add(Double.parseDouble(s));
                        }
                    }
                if (tmp[0].equals( "below")) {
                        startProb = Double.parseDouble((tmp[1]));
                    }
                if(tmp[0] .equals( "above")){
                        endProb = Double.parseDouble(tmp[1]);
                    }
                if(tmp[0].toCharArray()[0] == "L".toCharArray()[0]){
                    incrementalWeightTemp = Double.parseDouble(tmp[1]);
                    weights.add(incrementalWeightTemp);

                }
                }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (brp != null) {
                try {
                    brp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new Properties( _simulationName, _xOrds, _CI_Values, startProb, weights, endProb);
    }
}


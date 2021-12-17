import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

public class ChartPreProcessorScript {
    public static void main(String[] args){
        String filePath = "C:\\Users\\Brenn\\Downloads\\Existing_Conditions-Trinity (1).dss";
        Collection<String> locationsToChart = ChartPreProcessor.getAllSavedLocations(filePath);
        ChartPreProcessor tool = new ChartPreProcessor(filePath,locationsToChart,500,"C:\\Temp\\FinalChartsForNRC\\");
        tool.writeToExcel();
    }
}


import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecPairedData;
import hec.io.PairedDataContainer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ChartPreProcessor {
    String _fileName;
    Collection<String> locationsToChart;
    int maxPoints;
    String _outputDir;

    public ChartPreProcessor(String _fileName, Collection<String> locationsToChart, int maxPoints, String _outputDir) {
        this._fileName = _fileName;
        this.locationsToChart = locationsToChart;
        this.maxPoints = maxPoints;
        this._outputDir = _outputDir;
    }

    void writeToExcel(){
        for(String locations: locationsToChart){
            try {
                FileWriter myFileWriter = new FileWriter(_outputDir + locations + ".csv");
                HecPairedData pairedDataA = new HecPairedData();
                pairedDataA.setDSSFileName(_fileName);
                Vector<DSSPathname> chartSerieses = getPathnamesForChart(locations,252);
                for(DSSPathname series : chartSerieses){
                    PairedDataContainer pdcA = new PairedDataContainer();
                    pdcA.fullName = series.getPathname();
                    pairedDataA.read(pdcA);
                    Line myCurve = new Line(pdcA.xOrdinates,pdcA.yOrdinates[0]);
                    myCurve.ConvertToNonExceedenceProbability();
                    myCurve.ConvertXordProbabilitiesToZScores();
                    LineThinner.VisvaligamWhyattSimplify(500,myCurve);

                    myFileWriter.write( series.dPart() + series.getCollectionSequence() + ",");
                    for(double z: myCurve.getXords()){
                        myFileWriter.write(z + ",");
                    }
                    myFileWriter.write("\n" + " ,");
                    for(double value: myCurve.getYords()){
                        myFileWriter.write(value + ",");
                    }
                    myFileWriter.write("\n");
                }
                myFileWriter.write(locations + "\n");
                myFileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Vector<DSSPathname> getPathnamesForChart(String ePart, int numRealsToChart) {
        Vector<String> chartSeriesList = new Vector<>();
        String[] recordsToCollect = new String[]{"Realization Thin","0.975 Confidence Limit","0.025 Confidence Limit","Frequency Thin"};
        HecDataManager dssManager = new HecDataManager();
        dssManager.setDSSFileName(_fileName);
        for(String recordType : recordsToCollect){
            String pathWithWildChars = "/*/*/*/*" + recordType + "*/*"+ ePart + "*/*/";
            String[] pathnames = dssManager.getCatalog(false, pathWithWildChars);
            if(recordType == "Realization Thin"){
                for(int i = 0; i< pathnames.length; i++){
                    chartSeriesList.add(pathnames[i]);
                    if(i>=numRealsToChart-1){
                        break;
                    }
                }
            }
            else {
                chartSeriesList.addAll(Arrays.asList(pathnames));
            }
        }
        Vector<DSSPathname> out = new Vector<>();
        for(String pathname : chartSeriesList){
            DSSPathname name = new DSSPathname(pathname);
            out.add(name);
        }
        HecDataManager.closeAllFiles();
        return out;
    }

    public static Set<String> getAllSavedLocations(String dssFile){
        Set<String> uniqueLocations = new HashSet<>();
        HecDataManager dssManager = new HecDataManager(dssFile);
        String[] pathnames = dssManager.getCatalog(false,"/*/*/*/*/*/*/");
        for(String pathname: pathnames){
            DSSPathname name = new DSSPathname(pathname);
            uniqueLocations.add(name.getEPart());
        }
        return uniqueLocations;
    }
}


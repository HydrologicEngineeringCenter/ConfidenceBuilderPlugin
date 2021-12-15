import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecPairedData;
import hec.io.PairedDataContainer;
import rma.stats.NormalDist;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

public class ChartPreProcessor {
    String _fileName;
    Vector<String> locationsToChart;
    int maxPoints = 500;
    String _outputDir;

    void writeToExcel(){
        for(String locations: locationsToChart){
            try {
                FileWriter myFileWriter = new FileWriter(_outputDir + locations + ".csv");
                HecPairedData pairedDataA = new HecPairedData();
                pairedDataA.setDSSFileName(_fileName);
                Vector<DSSPathname> chartSerieses = getPathnamesForChart(locations);
                for(DSSPathname series : chartSerieses){
                    PairedDataContainer pdcA = new PairedDataContainer();
                    pdcA.fullName = series.getPathname();
                    pairedDataA.read(pdcA);
                    Line myCurve = new Line(pdcA.xOrdinates,pdcA.yOrdinates[0]);
                    myCurve.ConvertXordProbabilitiesToZScores();
                    LineThinner.VisvaligamWhyattSimplify(500,myCurve);

                    myFileWriter.write(series.ePart() + series.getCollectionSequence());
                    for(double z: myCurve.getXords()){
                        myFileWriter.write(z + ",");
                    }
                    myFileWriter.write("\n" + "blank,");
                    for(double value: myCurve.getYords()){
                        myFileWriter.write(value + ",");
                    }
                    myFileWriter.write("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Vector<DSSPathname> getPathnamesForChart(String ePart) {
        Vector<String> chartSeriesList = new Vector<>();
        String[] recordsToCollect = new String[]{"Realization Thin","0.975 Confidence Limit","0.025 Confidence Limit","Frequency Thin"};
        HecDataManager dssManager = new HecDataManager();
        dssManager.setDSSFileName(_fileName);

        for(String recordType : recordsToCollect){
            String pathWithWildChars = "/*/*/*/*" + recordType + "*/*"+ ePart + "*/*/";
            String[] pathnames = dssManager.getCatalog(false, pathWithWildChars);
            chartSeriesList.addAll(Arrays.asList(pathnames));
        }

        Vector<DSSPathname> out = new Vector<>();
        for(String pathname : chartSeriesList){
            DSSPathname name = new DSSPathname(pathname);
            out.add(name);
        }

        HecDataManager.closeAllFiles();
        return out;
    }

}


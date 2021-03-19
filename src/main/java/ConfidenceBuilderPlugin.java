/* * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import com.rma.client.Browser;
import com.rma.client.BrowserAction;
import com.rma.io.DssFileManagerImpl;
import com.rma.model.Manager;
import com.rma.model.ManagerProxy;
import com.rma.model.Project;
import hec.heclib.dss.DSSPathname;
import hec.io.PairedDataContainer;
import hec.model.OutputVariable;
import hec2.plugin.AbstractPlugin;
import hec2.plugin.model.ModelAlternative;
import hec2.wat.client.WatFrame;
import hec2.wat.model.FrmSimulation;
import hec2.wat.model.tracking.OutputTracker;
import hec2.wat.model.tracking.OutputVariableImpl;
import hec2.wat.plugin.SimpleWatPlugin;
import hec2.wat.plugin.WatPluginManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import org.apache.commons.lang.ArrayUtils;
import rma.swing.RmaImage;
import rma.util.RMAIO;
/**
 *
 * @author WatPowerUser
 */

public class ConfidenceBuilderPlugin extends AbstractPlugin implements SimpleWatPlugin {
    public static final String PluginName = "Confidence Builder Plugin";
    public static final String PluginShortName = "Confidence Builder";
    private static final String _pluginVersion = "1.0.0";
    private static String _simulationName = "";
    private static final String _propertiesPath = "/cbp/ConfidenceBuilder.props";
    private static List<Double> _XOrds;
    private static List<Double> _CI_Vals;
/**
     * @param args the command line arguments*/


    public static void main(String[] args) {
        ConfidenceBuilderPlugin p = new ConfidenceBuilderPlugin();
    }
    public ConfidenceBuilderPlugin(){
        super();
        setName(PluginShortName);
        WatPluginManager.register(this);
        if ( isAppInstalled())
        {
                addToToolsToolbar();
        }
    }
    protected void addToToolsToolbar() {
        Icon i = RmaImage.getImageIcon("Images/Workstation.gif");
        BrowserAction a = new BrowserAction(PluginShortName,i,this, "displayApplicationUniqueF");
        a.putValue(Action.SHORT_DESCRIPTION, getName());
        Browser.getBrowserFrame().getToolsMenu().insert(a,3);
        ((WatFrame)Browser.getBrowserFrame()).getToolsToolbarGroup().add(a);
    }
    @Override
    public boolean createProject(Project prjct) {
        return true;
    }
    @Override
    public boolean openProject(Project prjct) {
        return true;//called when the user is asked to open an existing project.
    }
    @Override
    public boolean close(boolean bln) {
        return true;
    }
    @Override
    public String getProjectName() {
        return "";
    }
    @Override
    public boolean saveProject() {
        return true;
    }
    @Override
    public String getLogfile() {
        return null;
    }
    public boolean displayApplicationUniqueF() {
        /*return displayApplication();*/
        Thread thread = new Thread() {
            public void run() {
                System.out.println("Thread Running");
                if (displayApplication()) {
                    System.out.println("Complete");
                }
                else {
                    System.out.println("Something Didn't Work");
                }
            }
        };
        thread.start();
        return true;
    }
    @Override
    public boolean displayApplication() {
        //Getting the overhead knowledge of which project we're working with is---
        Project proj = Browser.getBrowserFrame().getCurrentProject();
        String dir = proj.getProjectDirectory();
        WatFrame myWatFrame = hec2.wat.WAT.getWatFrame();
        if(dir!=null){
            myWatFrame.addMessage("Found "+dir);
        }else{
            myWatFrame.addMessage("Please Open Project");
            return false;
        }


        //read in properties and weights from properties file --
        String propertiesFile = dir + _propertiesPath;
        Properties myProperties = PropertyFileReader.Read(propertiesFile);
        _simulationName = myProperties.getSimulationName();


        //get the simulation --
        List<ManagerProxy> managerProxyListForType = proj.getManagerProxyListForType(FrmSimulation.class);
        Manager myManager = null;
        FrmSimulation myFRMSimulation = null;
        OutputTracker myOutputTracker = null;
        for(ManagerProxy mp : managerProxyListForType){
            if(mp.getName().equals(_simulationName)){
                myManager = mp.getManager();
                myFRMSimulation = (FrmSimulation)myManager;//get the FRM simulation object
                myOutputTracker =myFRMSimulation.getOutputTracker();//get the outputTracker object
                myWatFrame.addMessage("Found simulation");
            }
        }


        if(myOutputTracker!=null){
            myWatFrame.addMessage("Output Tracker found");


            //cycle through all output variables and check to ensure that Frequency Curves Output Variables exist for each
            // output variable. a frequency output variable must exist for the frequency viewer to view it.
            List<List<OutputVariableImpl>> varListList = myOutputTracker.getVarListList(); // varListList is a list of output variables, seperated in a single list for each model
            List<List<OutputVariableImpl>> freqvarListList = new ArrayList<>();
            for(int i = 0;i<varListList.size();i++){
                List<OutputVariableImpl> variablesForModel = varListList.get(i);
                List<OutputVariableImpl> freqVarForModel = new ArrayList<>();
                for(int j =0;j<variablesForModel.size();j++){
                    OutputVariableImpl c = variablesForModel.get(j).clone();//clone the output variables to have frequency curves created.
                    c.setHasFrequency(true); //here's where we set them to frequency output variables
                    freqVarForModel.add(c);
                }
                freqvarListList.add(freqVarForModel);
            }
            myOutputTracker.setFreqVarListList(freqvarListList);//now they exist on the list, set the data. This list is is almost identical to getVarListList() but has all frequency turned on.
            myFRMSimulation.saveData();//not null because myOutputTracker was retrieved from it... SAVE



            //now compute frequency with weights for all frequency curves.
            //Get the variable list of lists and model list for this simulation--
            varListList = myOutputTracker.getVarListList();
            List<ModelAlternative> models = myFRMSimulation.getAllModelAlternativeList();

            for(int i = 0;i<varListList.size();i++){ //for each variable
                ModelAlternative modelAlt = models.get(i);//get the model it comes from
                List<OutputVariableImpl> variablesForModel = varListList.get(i); //get the output variables associated with that model
                if (variablesForModel != null) { //if that's not Null
                    int size = variablesForModel.size(); //record how many variable for this model
                    for (int j = 0; j < size; j++) { //for how ever many variables in the model
                        OutputVariableImpl myOutputVariable = variablesForModel.get(j); // read them
                        PairedDataContainer pdc = myOutputVariable.getPairedDataContainer(); // add them to a pdc

                        //if the pdc doesn't have a file to go to yet save it in the run directory as the simulation name .dss
                        if (pdc.fileName == null || pdc.fileName.isEmpty()) {
                            String runDir = myFRMSimulation.getSimulationDirectory();
                            runDir = runDir.concat(RMAIO.userNameToFileName(myFRMSimulation.getName())).concat(".dss");
                            pdc.fileName = runDir;
                            //myWatFrame.addMessage(runDir);
                        }

                        //if the pdc doesnt have a record name yet, build it one, and give it to it.
                        if (pdc.fullName == null || pdc.fullName.isEmpty()) {
                            DSSPathname path = myOutputTracker.buildDSSPathname(myFRMSimulation, modelAlt, myOutputVariable);
                            path.setCollectionSequence(0); //This Zero freaks me out. What's it doing here?
                            pdc.fullName = path.getPathname();
                        }else{ }

                        //Write to console where the files are gonna be saved, and what they're gonna be called.
                        myWatFrame.addMessage("Saving to: " + pdc.fileName);
                        myWatFrame.addMessage("It is called: " + pdc.fullName);

                        myFRMSimulation.addMessage(myFRMSimulation.getName() + ":" + modelAlt.getProgram() + "-" + modelAlt.getName()
                                        + ":Computing weighted output variable frequency curve " + (j + 1) + "/" + size);

                        //This is just cleaning things up:--
                        OutputVariableImpl freqVar = myOutputTracker.getFreqVarForOutputVar(myOutputVariable, i);
                        myWatFrame.addMessage("Computing weighted output variable frequency curve for " + freqVar._name);
                        List<PairedDataContainer> pdcList = myOutputVariable.getAllPairedDataList();
                        myWatFrame.addMessage(freqVar._name + " has " + pdcList.size() + " realizations");
                        freqVar.deleteAllFrequencyPairedData();//tidy up.




                        List<ValueBinIncrementalWeight[]> allData = new ArrayList<>();
                        int realization = 0;
                        for(PairedDataContainer pdci : pdcList){//build the frequency output
                            freqVar.setPairedDataContainer(pdci);
                            ValueBinIncrementalWeight[] tmp = saveVariableFrequencyRealization(freqVar,pdci,myFRMSimulation,myProperties.getBinStartWeight(),myProperties.getBinEndWeights(),myProperties.getBinWeights(),realization);
                            //saveVariableFrequencyRealization_Thin(freqVar,pdci,myFRMSimulation,startProb,endProb,weights,realization);
                            if(tmp==null){
                                myWatFrame.addMessage("aborting frequency curve calculation.");
                                return false;
                            }
                            
                            //sort it to be ascending for the thinned work...
                            //Arrays.sort(tmp);
                            allData.add(tmp);
                            realization++;
                            myWatFrame.addMessage(freqVar._name + " realization " + allData.size() + " computed.");

                        }
                        //double[][] allData = getVariableAllFrequencyData(freqVar,myFRMSimulation);
                        //saveVariableFrequencyPercent(freqVar, allData,myFRMSimulation); //5 and 95 percent

                        ValueBinIncrementalWeight[] fullCurve = saveVariableFrequencyFull(freqVar, allData, myFRMSimulation,myProperties.getBinEndWeights(),myProperties.getBinStartWeight(),myProperties.getBinWeights());
                        if(fullCurve!=null){
                            if(_XOrds!=null){
                                saveVariableFrequencyConfidenceLimits(freqVar, fullCurve, myFRMSimulation,myProperties.getBinStartWeight(),myProperties.getBinEndWeights(),myProperties.getBinWeights());
                                //write method to sort valuebinincremetalweight, xcoords cumulitive incrimental weight, y cords will be values
                            }
                        
                        }else{
                            //myWatFrame.addMessage("Simulation thinning didnt work.");
                            return false;
                        }
                        if(fullCurve!=null){
                        //saveVariableFrequencyFull_Thin(freqVar, fullCurve, myFRMSimulation,endProb,startProb,weights);
                        }else{
                            //myWatFrame.addMessage("Simulation thinning didnt work.");
                            return false;
                        }
                    }
                }
            }
        }else{
            myWatFrame.addMessage("A WAT simulation named "+_simulationName+" was not found, please check your simulation names, and fix the \\cbp\\ConfidenceBuilder.props file to contain the name of the simulation you wish to destratify.");
            return false;
        }
        return true;
    }


    private ValueBinIncrementalWeight[] saveVariableFrequencyRealization(OutputVariableImpl vv, PairedDataContainer outPdc, FrmSimulation frm,Double startProb, double endProb, List<Double> weights, int real){
        //BUILD DATA
        int newOrdinates = frm.getYearsInRealization();
        double[] newXOrd = new double[newOrdinates];
        int numlifecycles = frm.getNumberLifeCycles();
        int numreals = frm.getNumberRealizations();

        int numOrdinates = outPdc.numberOrdinates;//should be number of events in the lifecycles?
        int numCurves = outPdc.numberCurves; //should be nubmer of lifecycles

        //Checking for errors in Data--
        if(numCurves!=(numlifecycles/numreals)){
            frm.addMessage("there are more curves than lifecycles per real, ignoring old data");
            numCurves = numlifecycles/numreals;
        }
        int realsPerWeightList = 1;
        if(numCurves!=weights.size()){
            frm.addMessage("Weight count does not match lifecycle count");
            double val = (double)weights.size()/(double)numCurves;
            if((val-java.lang.Math.floor(val))>0){
                frm.addMessage("Weight count is not evenly divisible by lifecycle count");
                return null;
            }else{
                realsPerWeightList = (int)val;
            }
        }

        //frm.addMessage("There are " + realsPerWeightList + " realizations per the list of weights");
        double totWeight = startProb;
        totWeight+=endProb;
        for (int k = 0; k < weights.size(); k++) { 
            totWeight+=weights.get(k);
        }
        int availOrdinates = numOrdinates * numCurves;
        double[] newYOrd = new double[Math.max(availOrdinates, newOrdinates)];
        double cumWeight = endProb;
        ValueBinIncrementalWeight[] data= new ValueBinIncrementalWeight[Math.max(availOrdinates, newOrdinates)];
        int shifter = real % realsPerWeightList;
        for (int curve = 0; curve < numCurves; curve++) {
            double[] yOrd = outPdc.yOrdinates[curve];//this is the number of lifecycles
            for (int ord = 0; ord < numOrdinates; ord++) {
                int newIdx = curve * numOrdinates + ord;//this is putting the events in a master realization list in order
                data[newIdx] = new ValueBinIncrementalWeight(yOrd[ord],(shifter*(numCurves))+ curve,weights.get((shifter*(numCurves))+ curve)/numOrdinates, real);
            }
        }
        
        Arrays.sort(data);
        ArrayUtils.reverse(data);
        //String s = "Value, Bin, EventNumber\n";
        for(int i = 0; i<data.length;i++){
            newYOrd[i] = data[i].getValue();
            cumWeight += data[i].getIncrimentalWeight();
            newXOrd[i] = (cumWeight - (data[i].getIncrimentalWeight())/2)/totWeight;//plotting position
            data[i].setPlottingPosition(newXOrd[i]);
        }

        //SAVE PDC
        PairedDataContainer freqPdc = vv.getPairedDataContainer();
        freqPdc.numberOrdinates = newOrdinates;
        freqPdc.numberCurves = 1;
        freqPdc.xOrdinates = newXOrd;
        freqPdc.yOrdinates = new double[][]{newYOrd};
        freqPdc.xparameter = "Probability";
        freqPdc.labelsUsed = true;
        freqPdc.labels = new String[1];

        final String sequence = DSSPathname.getCollectionSequence(outPdc.fullName); // first is 000000
        Integer realization = Integer.valueOf(sequence);
        realization++;  // plus one b/c we are going to show this to the user.

        freqPdc.labels[0] = "Realization ".concat(realization.toString());


        int zeroOnSuccess = DssFileManagerImpl.getDssFileManager().write(freqPdc);
        if (zeroOnSuccess != 0) {
                frm.addWarningMessage("Failed to save PD Output Variable Frequency to " + outPdc.fileName + ":" + outPdc.fullName + " rv=" + zeroOnSuccess);
        }
        return data;
    }
        private String removeSpecialChar(String input){
            input = input.replace('/', '_');
            input = input.replace('\\', '_');
            input = input.replace('?', '_');
            return input;
        }
    protected ValueBinIncrementalWeight[] saveVariableFrequencyFull(OutputVariableImpl vv, List<ValueBinIncrementalWeight[]> allData, FrmSimulation frm, double endProb, double startProb, List<Double> weights) {
        int colSize = allData == null ? 0 : allData.size();//number of realizations?
        int numOrds = colSize <= 0 ? 0 : allData.get(0).length;

        int realsPerWeightList = 1;
        int realizations = frm.getNumberRealizations();
        int lifecycles = frm.getNumberLifeCycles();
        lifecycles = lifecycles/realizations;
        if(lifecycles!=weights.size()){
            frm.addMessage("Weight count does not match lifecycle count");
            double val = (double)weights.size()/(double)lifecycles;
            if((val-java.lang.Math.floor(val))>0){
                frm.addMessage("Weight count is not evenly divisible by lifecycle count");
                return null;
            }else{
                realsPerWeightList = (int)val;
            }
        }
        frm.addMessage("There are " + realsPerWeightList + " realizations per the list of weights");
        int fullSize = colSize * numOrds;
        double totWeight = startProb;
        totWeight+=endProb;
        for (int k = 0; k < weights.size(); k++) { 
            totWeight+=weights.get(k);
        }
        
        ValueBinIncrementalWeight[] fullCurve = new ValueBinIncrementalWeight[fullSize];
        for (int i = 0; i < colSize; i++) {
                ValueBinIncrementalWeight[] colData = allData.get(i);
                System.arraycopy(colData, 0, fullCurve, i * numOrds, numOrds);
        }
        allData = null;
        //sort the full curve
        Arrays.sort(fullCurve);
        ArrayUtils.reverse(fullCurve);

        //build full curve and save - new pdc
        double[] xOrdinates = new double[fullSize];
        double[] yOrdinates = new double[fullSize];
        double cumWeight = endProb;
        int scaleFactor =realizations/realsPerWeightList;

        for (int k = 0; k < fullSize; k++) {
            cumWeight += fullCurve[k].getIncrimentalWeight()/scaleFactor;
            xOrdinates[k] = (cumWeight-((fullCurve[k].getIncrimentalWeight()/scaleFactor)/2))/totWeight;
            yOrdinates[k] = fullCurve[k].getValue();
        }

        //SAVE FULL PDCs
        PairedDataContainer freqPdc = vv.getFullFrequencyPairedData();
        PairedDataContainer freqThinPdc = vv.getThinFrequencyPairedData(); // Use this guy. Overwrite the data. Consider cleaning up extras from the initial longer array.
        Line thinFreqLine = LineThinner.DouglasPeukerReduction(ConvertValueBinIncrementalWeight2Line(fullCurve), .1);

        freqThinPdc.numberOrdinates = thinFreqLine.getVerticesCount();
        freqThinPdc.numberCurves = 1;
        freqThinPdc.xOrdinates = thinFreqLine.getXords();
        freqThinPdc.yOrdinates = new double[][]{thinFreqLine.getYords()};
        freqThinPdc.xparameter = "Probability";
        freqThinPdc.labelsUsed = true;
        freqThinPdc.labels = new String[1];
        freqThinPdc.labels[0] = "All Realizations";

        int zeroOnSuccess = DssFileManagerImpl.getDssFileManager().write(freqThinPdc);
        if (zeroOnSuccess != 0) {
            frm.addWarningMessage("Failed to save PD Output Variable Frequency to " + freqThinPdc.fileName + ":" + freqThinPdc.fullName + " rv=" + zeroOnSuccess);
        }
        else{
            frm.addMessage("The thinned line for " + freqThinPdc.fullName + " is " + freqThinPdc.numberOrdinates + "long.");
            frm.addMessage("saved " + freqThinPdc.fullName + " to " + freqThinPdc.fileName + "!");
        }

        freqPdc.numberOrdinates = fullSize;
        freqPdc.numberCurves = 1;
        freqPdc.xOrdinates = xOrdinates;
        freqPdc.yOrdinates = new double[][]{yOrdinates};
        freqPdc.xparameter = "Probability";
        freqPdc.labelsUsed = true;
        freqPdc.labels = new String[1];
        freqPdc.labels[0] = "All Realizations";

        zeroOnSuccess = DssFileManagerImpl.getDssFileManager().write(freqPdc);
        if (zeroOnSuccess != 0) {
            frm.addWarningMessage("Failed to save PD Output Variable Frequency to " + freqPdc.fileName + ":" + freqPdc.fullName + " rv=" + zeroOnSuccess);
        }
        else{
            frm.addMessage("saved " + freqPdc.fullName + " to " + freqPdc.fileName + "!");
        }

        return fullCurve;
    }
    public void saveVariableFrequencyConfidenceLimits(OutputVariableImpl vv, ValueBinIncrementalWeight[] fullCurve, FrmSimulation frm, double endProb, double startProb, List<Double> weights){
        //sort by realization (ascending)
        ValueBinIncrementalWeight.setSort(false);
        Arrays.sort(fullCurve);
        //separate into bin arrays.
        List<ValueBinIncrementalWeight[]> realizations = new ArrayList<>();
        int real = fullCurve[0].getRealizationNumber();
        int numEventsPerReal =0;
        //List<Integer> proxys = Arrays.asList(62, 87, 125, 175, 225, 262, 287, 337, 375, 425, 475, 512, 550, 612, 637, 675, 725, 775, 825, 862, 887, 925, 962, 999);
        double[] maxs = new double[_XOrds.size()];
        double[] mins = new double[_XOrds.size()];
        for(ValueBinIncrementalWeight event: fullCurve){
            if(real==event.getRealizationNumber()){
                numEventsPerReal++;
            }else{
                break;
            }
        }
        realizations.add(new ValueBinIncrementalWeight[numEventsPerReal]);
        int currentEvent = 0;
        
        //sort by value
        ValueBinIncrementalWeight.setSort(true);
        for(int i = 0; i<_XOrds.size();i++){
            maxs[i] = Double.MIN_VALUE;
            mins[i] = Double.MAX_VALUE;
        }
        //frm.addMessage("working on real " + real);
        for(ValueBinIncrementalWeight event: fullCurve){
            if(real == event.getRealizationNumber()){
                //add it to a list
                realizations.get(real)[currentEvent] = event;
                currentEvent++;
            }else{
                Arrays.sort(realizations.get(real));//sort by value - per real//ascending..
                for(int i = 0; i<_XOrds.size();i++){
                    int proxy = (int)Math.floor(_XOrds.get(i)*realizations.get(real).length);
                    if (proxy >= realizations.get(real).length) proxy = realizations.get(real).length -1;
                    if(realizations.get(real)[proxy].getValue()>maxs[i]){maxs[i] = realizations.get(real)[proxy].getValue();}
                    if(realizations.get(real)[proxy].getValue()<mins[i]){mins[i] = realizations.get(real)[proxy].getValue();}
                }
                real = event.getRealizationNumber();
                //frm.addMessage("working on real " + real);
                currentEvent = 0;
                realizations.add(new ValueBinIncrementalWeight[numEventsPerReal]);
                realizations.get(real)[currentEvent] = event;
                currentEvent++;
            }
        }
        String s = "Real,Event,Value,AEP";
        currentEvent = 0;
        int realization = 0;

        String fileloc = frm.getProject().getProjectDirectory() + "\\Weights_TextFiles\\" + removeSpecialChar(vv._name) + "_RawData.txt";
        File destFileDirPath = new File(frm.getProject().getProjectDirectory() + "\\Weights_TextFiles\\");
        if(!destFileDirPath.exists()){
            destFileDirPath.mkdirs();
        }
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileloc));
            bw.write(s + "\n");
            for(ValueBinIncrementalWeight[] events : realizations){
                realization++;
                currentEvent = 0;
                for(ValueBinIncrementalWeight event : events){
                    currentEvent++;
                    bw.write(realization + "," + currentEvent + "," + event.getValue() + "," + (1-event.getPlottingPosition()) + "\n");
                    
                }
            }
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfidenceBuilderPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        //interpolate to find ploting position of interest and bin
        List<HistDist> verticalSlices = new ArrayList<>();
        int ordcount= 0;
        int bincount = (int)Math.ceil(Math.pow(2.0*realizations.size(),1/3));
        if(bincount<20){bincount=20;}
        
        for(Double d: _XOrds){
            int failureCount=0;//          
            verticalSlices.add(new HistDist(bincount,mins[ordcount],maxs[ordcount]));//how to intelligently get min and max for this prob range?
            //frm.addMessage("Probability: " + d + " with bin Max: " + maxs[ordcount] + " and Min: " + mins[ordcount]);
            ValueBinIncrementalWeight prevVal = null;
            int realcount = 0;
            for(ValueBinIncrementalWeight[] realdata: realizations){
                realcount++;
                boolean foundVal = false;
                for(ValueBinIncrementalWeight obj: realdata ){
                    //should be ascending.
                    if(obj.getPlottingPosition()<=d){
                        //now figure out how to interpolate...
                        foundVal = true;
                        if(prevVal==null){
                            //shit.
                            verticalSlices.get(ordcount).addObservation(obj.getValue()); }
                        else{
                            //interpolate
                            double y1 = prevVal.getValue();
                            double y2 = obj.getValue();
                            //-log(-log(p));
                            double x1 = prevVal.getPlottingPosition();
                            double x2 = obj.getPlottingPosition();
                            double ret = y1+((d-x1)*((y1-y2)/(x1-x2))); //is it x1-d or is it d-x1?
                            //frm.addMessage("Max: " + verticalSlices.get(ordcount).getMax() + " Min: " + verticalSlices.get(ordcount).getMin() + " New Value:" + ret);
                            if(!verticalSlices.get(ordcount).addObservation(ret)){
                                failureCount++;
                            }else{
                                if(realcount>100){
                                    verticalSlices.get(ordcount).testForConvergence(.05, .95, .1, .0001);
                                }
                            }
                            break;
                        }
                    }
                    prevVal = obj;
                }
                if(!foundVal){frm.addMessage("Did not find a value for ord " + ordcount + " which has probability " + d + " on realization " + realcount + " for location " + vv.getName());}
            }
            frm.addMessage(vv._name + " converged: " + verticalSlices.get(ordcount).getConverged() + " for vertical probabilty slice of " + d);
            if(verticalSlices.get(ordcount).getConverged()){
                frm.addMessage(vv._name + " converged on iter: " + verticalSlices.get(ordcount).getConvergedIteration());
            }
            ordcount++;  
        }
        double[] xordinates = new double[_XOrds.size()];
        for(int i = 0; i<_XOrds.size();i++){
            xordinates[i] = _XOrds.get(i);
        }
                //SAVE FULL PDCs
            
        for(double o : _CI_Vals){
            double[] vals = new double[_XOrds.size()];
            for(int i= 0; i<verticalSlices.size();i++){
                vals[i] = verticalSlices.get(i).invCDF(o);
            }
            PairedDataContainer freqPdc = vv.getFullFrequencyPairedData();
            freqPdc.numberOrdinates = _XOrds.size();
            freqPdc.numberCurves = 1;
            freqPdc.xOrdinates = xordinates;
            freqPdc.yOrdinates = new double[][]{vals};
            freqPdc.xparameter = "Probability";
            freqPdc.labelsUsed = true;
            freqPdc.labels = new String[1];
            freqPdc.labels[0] = "Confidence Interval - " + o;
            DSSPathname pathname = new DSSPathname(freqPdc.fullName);
            pathname.setDPart(o + " Exceedance Val");
            freqPdc.fullName = pathname.getPathname(true);
            int zeroOnSuccess = DssFileManagerImpl.getDssFileManager().write(freqPdc);
            if (zeroOnSuccess != 0) {
                    frm.addWarningMessage("Failed to save PD Output Variable Frequency to "
                                    + freqPdc.fileName + ":" + freqPdc.fullName + " rv=" + zeroOnSuccess);
            }
        }
    }
    @Override
    public String getVersion() {
        return _pluginVersion;
    }
    @Override
    public String getDirectory() {
        return "";
    }
    private boolean isAppInstalled() {
            return true;
    }
    private Line ConvertValueBinIncrementalWeight2Line( ValueBinIncrementalWeight[] myVBIWArray){
        ArrayList<Double> tempIncWeights = new ArrayList<Double>();
        ArrayList<Double> tempValues = new ArrayList<Double>();
        double[] arrayX = new double[myVBIWArray.length];
        double[] arrayY = new double[myVBIWArray.length];

        for(int i = 0; i< myVBIWArray.length; i++ ){
            tempIncWeights.add(myVBIWArray[i].getIncrimentalWeight());
            tempValues.add(myVBIWArray[i].getValue());}

        double cumIncrementalWeight = 0;
        for(int i = 0; i< myVBIWArray.length; i++) {
            cumIncrementalWeight += tempIncWeights.get(i).doubleValue();
            arrayX[i] = cumIncrementalWeight;
            arrayY[i] = tempValues.get(i).doubleValue();
        }

        Line myLine = new Line(arrayX,arrayY);
        return myLine;
    }
}

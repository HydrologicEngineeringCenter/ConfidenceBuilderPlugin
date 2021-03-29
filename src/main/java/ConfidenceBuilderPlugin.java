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
                            path.setCollectionSequence(0);
                            pdc.fullName = path.getPathname();
                        }else{ }

                        //Write to console where the files are gonna be saved, and what they're gonna be called. A Diagnostic
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
                            ValueBinIncrementalWeight[] tmp = saveVariableFrequencyRealization(freqVar,pdci,myFRMSimulation,myProperties,realization);
                            if(tmp==null) {
                                myWatFrame.addMessage("aborting frequency curve calculation.");
                                return false;
                            }
                            //sort it to be ascending for the thinned work... //this is suspicious. shoudl already be sorted from the previous method
                            Arrays.sort(tmp);
                            allData.add(tmp);
                            realization++;
                            myWatFrame.addMessage(freqVar._name + " realization " + realization + " computed.");
                        }

                        ValueBinIncrementalWeight[] fullCurve = saveVariableFrequencyFull(freqVar, allData, myFRMSimulation,myProperties.getBinEndWeights(),myProperties.getBinStartWeight(),myProperties.getBinWeights());
                        if(allData!=null){
                            if(myProperties.getCI_Values()!=null){
                                saveVariableFrequencyConfidenceLimits(freqVar, allData, myFRMSimulation,myProperties);
                                //write method to sort valuebinincremetalweight, xcoords cumulitive incrimental weight, y cords will be values
                            }
                        
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


    private ValueBinIncrementalWeight[] saveVariableFrequencyRealization(OutputVariableImpl vv, PairedDataContainer outPdc, FrmSimulation frm,Properties props, int real){
        //BUILD DATA
        int numlifecycles = frm.getNumberLifeCycles();
        int numreals = frm.getNumberRealizations();
        int numEventsPLifecycleDSS = outPdc.numberOrdinates;//should be number of events in the lifecycles?
        int numLifeCyclesDSS = outPdc.numberCurves; //should be number of lifecycles

        //total weight should equal one now.
        double totWeight = props.getBinStartWeight();
        totWeight+=props.getBinEndWeights();
        for (int k = 0; k < props.getBinWeights().size(); k++) {
            totWeight+=props.getBinWeights().get(k);
        }

        //Checking for errors in Data--
        if(numLifeCyclesDSS!=(numlifecycles/numreals)){
            frm.addMessage("there are more curves than lifecycles per real. Aborting");
            return null;
        }
        if(numLifeCyclesDSS!=props.getBinWeights().size()){
            frm.addMessage("Weight count does not match lifecycle count");
            return null;
        }
       // Done Checking--

        //put all the events into a a single array, representing the entire realization as VBIW.
        int eventsInAReal = numEventsPLifecycleDSS * numLifeCyclesDSS; //events in a realization.
        double[] eventValuesArray = new double[eventsInAReal];
        ValueBinIncrementalWeight[] data= new ValueBinIncrementalWeight[eventsInAReal];
        for (int lifecycleNumber = 0; lifecycleNumber < numLifeCyclesDSS; lifecycleNumber++) { //lifecycle loop
            double[] events = outPdc.yOrdinates[lifecycleNumber];
            for (int eventNumberInLifeCycle = 0; eventNumberInLifeCycle < numEventsPLifecycleDSS; eventNumberInLifeCycle++) { // event loop
                int eventIndex = lifecycleNumber * numEventsPLifecycleDSS + eventNumberInLifeCycle;
                data[eventIndex] = new ValueBinIncrementalWeight(events[eventNumberInLifeCycle], lifecycleNumber,props.getBinWeights().get(lifecycleNumber)/numEventsPLifecycleDSS,real);
            }
        }

        //Sort the array
        Arrays.sort(data);
        ArrayUtils.reverse(data);

        //Convert that VBIW array into a double[] of values, and a double[] of plotting position
        double[] plottingPosArray = new double[eventsInAReal];
        double cumWeight = props.getBinEndWeights();
        for(int i = 0; i<data.length;i++){
            eventValuesArray[i] = data[i].getValue();
            cumWeight += data[i].getIncrimentalWeight();
            plottingPosArray[i] = (cumWeight - (data[i].getIncrimentalWeight())/2)/totWeight;//plotting position array I don't understand
            data[i].setPlottingPosition(plottingPosArray[i]); //saving the plotting position into the VBIW object
        }

        //SAVE PDC with all data------
        PairedDataContainer freqPdc = vv.getPairedDataContainer();
        freqPdc.numberOrdinates = plottingPosArray.length;
        freqPdc.numberCurves = 1;
        freqPdc.xOrdinates = plottingPosArray;
        freqPdc.yOrdinates = new double[][]{eventValuesArray};
        freqPdc.xparameter = "Probability";
        freqPdc.labelsUsed = true;
        freqPdc.labels = new String[1];

        final String sequence = DSSPathname.getCollectionSequence(freqPdc.fullName); // first is 000000
        Integer realization = Integer.valueOf(sequence);
        realization++;  // plus one b/c we are going to show this to the user.
        freqPdc.labels[0] = "Realization ".concat(realization.toString());

        //SAVE PDC with thin data-------
        //Thin this before we save it out
        Line myLine = new Line(plottingPosArray,eventValuesArray);
        myLine.ConvertXordProbabilitiesToZScores();
        Line myThinLine = LineThinner.DouglasPeukerReduction(myLine,.001);
        myThinLine.ConvertXordZScoresToProbabilities();

        //SAVE THIN PDC (change the d part of the pathname)
        PairedDataContainer thinFreqPdc = vv.getPairedDataContainer();
        thinFreqPdc.numberOrdinates = myThinLine.getVerticesCount();
        thinFreqPdc.numberCurves = 1;
        thinFreqPdc.xOrdinates = myThinLine.getXords();
        thinFreqPdc.yOrdinates = new double[][]{myThinLine.getYords()};
        thinFreqPdc.xparameter = "Probability";
        thinFreqPdc.labelsUsed = true;
        thinFreqPdc.labels = new String[1];
        thinFreqPdc.labels[0] = "Realization ".concat(realization.toString());
        DSSPathname pathname = new DSSPathname(thinFreqPdc.fullName);
        pathname.setDPart("Realization Thin");
        thinFreqPdc.fullName = pathname.getPathname(false);

        //Save PDC
        int zeroOnSuccess = DssFileManagerImpl.getDssFileManager().write(freqPdc);
        if (zeroOnSuccess != 0) {
                frm.addWarningMessage("Failed to save PD Output Variable Frequency to " + freqPdc.fileName + ":" + freqPdc.fullName + " rv=" + zeroOnSuccess);
        }
        //Save Thin PDC
        int anotherZeroOnSuccess = DssFileManagerImpl.getDssFileManager().write(thinFreqPdc);
        if (anotherZeroOnSuccess != 0) {
            frm.addWarningMessage("Failed to save PD Output Variable Frequency to " + thinFreqPdc.fileName + ":" + thinFreqPdc.fullName + " rv=" + zeroOnSuccess);
        }

        return data;
    }
    protected ValueBinIncrementalWeight[] saveVariableFrequencyFull(OutputVariableImpl vv, List<ValueBinIncrementalWeight[]> allData, FrmSimulation frm, double endProb, double startProb, List<Double> weights) {
        int numReals = allData == null ? 0 : allData.size();//number of realizations?
        int numEventsInReal = numReals <= 0 ? 0 : allData.get(0).length;// What's happening here

        double totWeight = startProb;
        totWeight+=endProb;
        for (int k = 0; k < weights.size(); k++) {
            totWeight+=weights.get(k);
        }

        int fullSize = numReals * numEventsInReal;
        ValueBinIncrementalWeight[] fullCurve = new ValueBinIncrementalWeight[fullSize];

        for (int i = 0; i < numReals; i++) {
                ValueBinIncrementalWeight[] tmpReal = allData.get(i);
                System.arraycopy(tmpReal, 0, fullCurve, i * numEventsInReal, numEventsInReal);
        }

        //sort the full curve
        Arrays.sort(fullCurve);
        ArrayUtils.reverse(fullCurve);

        //build full curve and save - new pdc
        double[] xOrdinates = new double[fullSize];
        double[] yOrdinates = new double[fullSize];
        double cumWeight = endProb;

        for (int k = 0; k < fullSize; k++) {
            cumWeight += fullCurve[k].getIncrimentalWeight()/numReals;
            xOrdinates[k] = (cumWeight-((fullCurve[k].getIncrimentalWeight())/numReals/2))/totWeight;
            yOrdinates[k] = fullCurve[k].getValue();
        }

        //SAVE FULL PDCs
        PairedDataContainer freqPdc = vv.getFullFrequencyPairedData();
        PairedDataContainer freqThinPdc = vv.getThinFrequencyPairedData(); // Use this guy. Overwrite the data. Consider cleaning up extras from the initial longer array.
        Line tmpLine = new Line(xOrdinates,yOrdinates);
        tmpLine.ConvertXordProbabilitiesToZScores();
        Line thinFreqLine = LineThinner.DouglasPeukerReduction(tmpLine, .001);
        thinFreqLine.ConvertXordZScoresToProbabilities();

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
            frm.addWarningMessage("Failed to save PD Output Variable Frequency to " + freqThinPdc.fullName + " rv=" + zeroOnSuccess);
        }
        else{
            frm.addMessage("The thinned line for " + freqThinPdc.fullName + " is " + freqThinPdc.numberOrdinates + " points long.");
            frm.addMessage("saved " + freqThinPdc.fullName);
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
            frm.addWarningMessage("Failed to save PD Output Variable Frequency to " + freqPdc.fullName + " rv=" + zeroOnSuccess);
        }
        else{
            frm.addMessage("saved " + freqPdc.fullName + "!");
        }

        return fullCurve;
    }
    public void saveVariableFrequencyConfidenceLimits(OutputVariableImpl vv, List<ValueBinIncrementalWeight[]> allData, FrmSimulation frm, Properties props ) {
/*        //sort by realization (ascending)
        ValueBinIncrementalWeight.setSortByValue(false);
        Arrays.sort(fullCurve);
        //Consider just referencing All data, rather than sorting full curve***

        //separate into bin arrays.
        List<ValueBinIncrementalWeight[]> realizations = new ArrayList<>();
        int real = fullCurve[0].getRealizationNumber();
        int numEventsPerReal =0;
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
        ValueBinIncrementalWeight.setSortByValue(true);
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
 */

        //Step1 make sure the arrays in All data are sorted by value
        //now max a mins represents the maximum value and minimum value for each realization

        /*
        Step1: Create an array of min and maxes the size of the Vertical Slices [realdata for 7 and beyond likely just needs to be subsituted with allData
        2.  coffee
        3. Need to make sure all the arrays in All data are sorted by value.
        4. Make sure incremental weights have been cumulated between all of them.
        5. Iterate through the realizations to set maxes and mins based on the cumulated frequency fo all data.
        6. Create a list of inline histograms for each vertical slice.
        7. (populate the data in this location using interpolation)
        8. Test for convergence
        9. Compute confidence intervals.
        10. Write conf intervals to disc

         */
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (ValueBinIncrementalWeight[] realization : allData) {
            for (ValueBinIncrementalWeight event : realization) {
                if (event.getValue() > max) {
                    max = event.getValue();
                }
                if (event.getValue() < min) {
                    min = event.getValue();
                }
            }
        }
        List<HistDist> verticalSlices = new ArrayList<>(); //This is step 6
        int ordcount = 0;
        int bincount = (int) Math.ceil(Math.pow(2.0 * allData.size(), 1 / 3));
        if (bincount < 20) {
            bincount = 20;
        }

        for (Double location : props.getXOrds()) {
            int failureCount = 0;//
            verticalSlices.add(new HistDist(bincount, max, min));// This is actaully step 6
            ValueBinIncrementalWeight prevVal = null;
            int realcount = 0;
            for (ValueBinIncrementalWeight[] realization : allData) { //This block is step 7
                realcount++;
                boolean foundVal = false;
                for (ValueBinIncrementalWeight event : realization) {
                    //should be ascending.
                    if (event.getPlottingPosition() <= location) {
                        foundVal = true;
                        //now figure out how to interpolate...
                        if (prevVal == null) {
                            verticalSlices.get(ordcount).addObservation(event.getValue());
                        } else {
                            //interpolate
                            double y1 = prevVal.getValue();
                            double y2 = event.getValue();
                            //-log(-log(p));
                            double x1 = prevVal.getPlottingPosition();
                            double x2 = event.getPlottingPosition();
                            double ret = y1 + ((location - x1) * ((y1 - y2) / (x1 - x2))); //is it x1-d or is it d-x1?
                            //frm.addMessage("Max: " + verticalSlices.get(ordcount).getMax() + " Min: " + verticalSlices.get(ordcount).getMin() + " New Value:" + ret);
                            verticalSlices.get(ordcount).addObservation(ret);
                        }
                        break;
                    }
                    prevVal = event;
                }
                if (foundVal == false) {
                    frm.addMessage("Did not find a value for ord " + ordcount + " which has probability " + location + " on realization " + realcount + " for location " + vv.getName());
                }
            }
            ordcount++;
            boolean pass = verticalSlices.get(ordcount).testForConvergence(.05, .95, .1, .0001);
            frm.addMessage("Confidence Interval for X ordinate: " + location + " was " + pass);
        }

        double[] xOrds = new double[props.getXOrds().size()]; // This is the x values for confidence limits. unboxing list of double to array of double.
        for (int i = 0; i < props.getXOrds().size(); i++) {
            xOrds[i] = props.getXOrds().get(i);
        }

        double[] vals = new double[verticalSlices.size()];
        for (double confidenceLimit : props.getCI_Values()) {
            for (int i = 0; i < verticalSlices.size(); i++) {
                vals[i] = verticalSlices.get(i).invCDF(confidenceLimit);
            }

            //SAVE FULL PDCs
            PairedDataContainer freqPdc = vv.getFullFrequencyPairedData();//write to disk Step 10
            freqPdc.numberOrdinates = xOrds.length;
            freqPdc.numberCurves = 1;
            freqPdc.xOrdinates = xOrds;
            freqPdc.yOrdinates = new double[][]{vals};
            freqPdc.xparameter = "Probability";
            freqPdc.labelsUsed = true;
            freqPdc.labels = new String[1];
            freqPdc.labels[0] = "Confidence Limit - " + confidenceLimit;
            DSSPathname pathname = new DSSPathname(freqPdc.fullName);
            pathname.setDPart(confidenceLimit + " Confidence Limit");
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
}

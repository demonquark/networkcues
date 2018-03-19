package networkcues;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.joone.engine.DirectSynapse;
import org.joone.engine.FullSynapse;
import org.joone.engine.Layer;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.Pattern;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.io.MemoryInputSynapse;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;

import repast.simphony.random.RandomHelper;

public class AgentController implements NeuralNetListener {
	
	public static final int LOGGING_START = 20000;
	public static final Mode LOGGING_MODE = Mode.FULL_BOOSTS;
	public static enum Mode { FULL_BOOSTS, RANDOM_BOOSTS, NO_BOOSTS, IMPROVE_WITH_BOOSTS };
	public static int logging_record = 0;

	private String pathToNeuralNetFile;
	private NeuralNet nnet;
	private MemoryInputSynapse inputSynapse;
	private MemoryInputSynapse desiredOutputSynapse;
	protected HashMap<Integer, Integer> groups;
	protected HashMap<Integer, Double> groupAffinities;
	private int dontOverDoTheLogging1;
	private int dontOverDoTheLogging2;
	private int dontOverDoTheLogging3;
	private int boostRequests;
	private double correctnessValue;
	private  double trustValue;
	private double boostCountValue;
	private Mode supervisorMode;

	public AgentController(String fileName) {
		groups = new HashMap<Integer, Integer> ();
		groupAffinities = new HashMap<Integer, Double> ();

		this.dontOverDoTheLogging1 = 0;
		this.dontOverDoTheLogging2 = 0;
		this.dontOverDoTheLogging3 = 0;
		this.boostRequests = 0;
		this.correctnessValue = 0;
		this.trustValue = 0;
		this.boostCountValue = 0;
		this.supervisorMode = LOGGING_MODE;
		this.pathToNeuralNetFile = fileName != null ? fileName : NetworkCuesBuilder.NNET_PATH; 

		// initialize the neural network
		this.initNeuralNet();
	}

    public void act(double[][] inputArray, double[][] desiredOutputArray) {
    	
    		logging_record = this.dontOverDoTheLogging2;
    	
	    	if (this.dontOverDoTheLogging1 < LOGGING_START) {
	    		this.train(inputArray, desiredOutputArray);
	    	} else if (this.dontOverDoTheLogging2 > LOGGING_START) {
	    		this.interrogate(inputArray, desiredOutputArray);
	    	} else if (this.dontOverDoTheLogging2 >= LOGGING_START - 1){
            // get the monitor object to train or feed forward
            Monitor monitor = nnet.getMonitor();
            
            // set the monitor parameters
            monitor.setTrainingPatterns(inputArray.length);
            monitor.setTotCicles(1);
            monitor.setLearning(false);
	    		this.nnet.stop();
	    		this.saveNeuralNet(this.pathToNeuralNetFile);
	    		
	    		this.dontOverDoTheLogging2++;
	    	}
    	
    }

	private void initNeuralNet() {

		LinearLayer input; 
		SigmoidLayer output;
		
		// Try to load an existing neural network
		if (this.pathToNeuralNetFile != null) {
			NeuralNetLoader loader = new NeuralNetLoader(this.pathToNeuralNetFile); 
			this.nnet = loader.getNeuralNet();			
		}
		 
		if (this.nnet == null) {
	        // Create the layers
			input = new LinearLayer();
			SigmoidLayer hidden1 = new SigmoidLayer();
			SigmoidLayer hidden2 = new SigmoidLayer();
			output = new SigmoidLayer();
	        
	        input.setRows(20);
	        hidden1.setRows(24);
	        hidden2.setRows(8);
	        output.setRows(1);
	        
	        input.setLayerName("L.input");
	        hidden1.setLayerName("L.hidden1");
	        hidden2.setLayerName("L.hidden2");
	        output.setLayerName("L.output");
	        
	        // Now create the Synapses
	        FullSynapse synapse_IH = new FullSynapse();	// input   -> hidden1
	        FullSynapse synapse_HH = new FullSynapse();	// hidden1 -> hidden2
	        FullSynapse synapse_HO = new FullSynapse();	// hidden2 -> output
	        
	        // Connect the layers using the synapses
	        input.addOutputSynapse(synapse_IH);
	        hidden1.addInputSynapse(synapse_IH);
	        hidden1.addOutputSynapse(synapse_HH);
	        hidden2.addInputSynapse(synapse_HH);
	        hidden2.addOutputSynapse(synapse_HO);
	        output.addInputSynapse(synapse_HO);
	        
	        // Create a new Neural network object
	        nnet = new NeuralNet();
	        
	        nnet.addLayer(input, NeuralNet.INPUT_LAYER);
	        nnet.addLayer(hidden1, NeuralNet.HIDDEN_LAYER);
	        nnet.addLayer(hidden2, NeuralNet.HIDDEN_LAYER);
	        nnet.addLayer(output, NeuralNet.OUTPUT_LAYER);

		} else {
			input = (LinearLayer) this.nnet.getInputLayer();
			output = (SigmoidLayer) this.nnet.getOutputLayer();
			
			input.removeAllInputs();
			output.removeAllOutputs();
			this.nnet.getMonitor().removeAllListeners();
		}

        // Connect the input layer using an input synapse
        this.inputSynapse = new MemoryInputSynapse();
        input.addInputSynapse(inputSynapse);
        
        // Connect the desired output layer using an input synapse
        TeachingSynapse trainer = new TeachingSynapse();
        this.desiredOutputSynapse = new MemoryInputSynapse();
        trainer.setDesired(desiredOutputSynapse);

        nnet.setTeacher(trainer);
        output.addOutputSynapse(trainer);
        nnet.getMonitor().addNeuralNetListener(this);
        
	}

	public void train(double[][] inputArray, double[][] desiredOutputArray) {
        
        // set the inputs
        inputSynapse.setInputArray(inputArray);
        inputSynapse.setAdvancedColumnSelector("1-20");
        
        // set the desired outputs
        desiredOutputSynapse.setInputArray(desiredOutputArray);
        desiredOutputSynapse.setAdvancedColumnSelector("1");
        
        // get the monitor object to train or feed forward
        Monitor monitor = nnet.getMonitor();
        
        // set the monitor parameters
        monitor.setLearningRate(0.8);
        monitor.setMomentum(0.3);
        monitor.setTrainingPatterns(inputArray.length);
        monitor.setTotCicles(10);
        monitor.setLearning(true);
        
        // Run the network in single-thread, synchronized mode
        nnet.getMonitor().setSingleThreadMode(true);
        nnet.go(true);
    }
    
    public boolean interrogate (double[][] inputArray, double[][] desiredOutputArray) {
    	
    		return this.interrogate(inputArray, desiredOutputArray, true);
    }
    
    private boolean interrogate (double[][] inputArray, double[][] desiredOutputArray, boolean addToStats) {
    	    		
    		// Attach a DirectSynapse to the input layer
    		Layer input = nnet.getInputLayer(); 
    		input.removeAllInputs();
    		DirectSynapse memInp = new DirectSynapse(); 
    		input.addInputSynapse(memInp);

    		// Attach a DirectSynapse to the output layer
    		Layer output = nnet.getOutputLayer(); 
    		output.removeAllOutputs();
    		DirectSynapse memOut = new DirectSynapse();  
    		output.addOutputSynapse(memOut);
    		
    		// Interrogate the input array
    		Pattern iPattern = new Pattern(inputArray[0]);
    		iPattern.setCount(1);

    		// Interrogate the net
        Monitor monitor = nnet.getMonitor();
        monitor.setTrainingPatterns(inputArray.length);
        monitor.setTotCicles(1);
        monitor.setLearning(false);
        monitor.setSingleThreadMode(false);

		nnet.go();
    		
    		memInp.fwdPut(iPattern);
    		Pattern pattern = memOut.fwdGet();
    		
    		boolean NNcorrectlyPredictsOutputs = (pattern.getArray()[0] < 0.5 && desiredOutputArray[0][0] < 0.5) || (pattern.getArray()[0] > 0.5 && desiredOutputArray[0][0] > 0.5); 
    		nnet.stop();

    		// Statistics
    		if (addToStats) {
        		if (NNcorrectlyPredictsOutputs)
        			this.correctnessValue += 1;
        		
        		if (desiredOutputArray[0][0] > 0.5)
        			this.trustValue += 1;

        		if (this.dontOverDoTheLogging3 % 10 == 0) {

        			System.out.println(Profile.COOPERATION_PERCENTAGE + ";" + String.format("%.2f" , this.trustValue / this.dontOverDoTheLogging3));

        			
//        			System.out.println((this.dontOverDoTheLogging3 / 100) + ". correctness_ratio: " + String.format("%.2f" , this.correctnessValue / this.dontOverDoTheLogging3)
//        							   + " cooperation_ratio: " + String.format("%.2f" , this.trustValue / this.dontOverDoTheLogging3)
//        							   + " boosted: " + String.format("%.2f" , this.boostCountValue / this.boostRequests));
//        			System.out.println( (this.dontOverDoTheLogging3 / 100) + ". " + String.format("%.2f" , desiredOutputArray[0][0]) + "|" + String.format("%.2f", pattern.getArray()[0]) 
//        								+ " " + tradeResultWithoutBoosts + " => " + tradeResult 
//        								+ " [" + (inputArray[0][16] > 0.2 ? "1" : "0") + (inputArray[0][17] > 0.2 ? "1" : "0") 
//        								+ (inputArray[0][18] > 0.2 ? "1" : "0") + (inputArray[0][19] > 0.2 ? "1" : "0") + "]"
//        								+ " " + String.format("%.2f" , this.improvementValue / this.dontOverDoTheLogging3));    			
    			
        		}

        		this.dontOverDoTheLogging3++;
    		}

    		return NNcorrectlyPredictsOutputs;
    }
    
    public double [] getBoosts(double[][] inputArray) {

    		double[] boosts = new double [4];
    		switch(this.supervisorMode) {
    		case FULL_BOOSTS:
	    		boosts[0] = 0.9;
	    		boosts[1] = 0.9;
	    		boosts[2] = 0.9;
	    		boosts[3] = 0.9;
	    		break;
    		case RANDOM_BOOSTS:
	    		boosts[0] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
	    		boosts[1] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
	    		boosts[2] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
	    		boosts[3] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
	    		break;

		case IMPROVE_WITH_BOOSTS:
			// The supervisor has left the training state and can be used for interrogation
			// Note that once the neural net starts interrogating it can NO LONGER BE USED FOR TRAINING
    			if (this.dontOverDoTheLogging3 > 10) {
    				
    				this.boostRequests++;
    				double [][] desiredInputArray = new double[1][20];
    				double [][] desiredOutputArray = new double[1][1];
    				
    				// Create an input and output array for the interrogation
    				// 1 is cooperated and 0 is defected
    				desiredOutputArray[0][0] = 1;
    				for(int i = 0; i < 20; i++) {
    					desiredInputArray[0][i] = inputArray[0][i];
    				}

    				// Keep trying boosts until we find something we find something that makes the user cooperate
    				for(int i = 0; i < 12; i++) {
    					boosts[0] = (i == 1 || i == 5 || i == 8 || i == 9 )  ? 0.9 : 0;
    					boosts[1] = (i == 2 || i == 5 || i == 6 || i == 10 ) ? 0.9 : 0;
    					boosts[2] = (i == 3 || i == 7 || i == 6 || i == 9 )  ? 0.9 : 0;
    					boosts[3] = (i == 4 || i == 7 || i == 8 || i == 10 ) ? 0.9 : 0;
    					
    					desiredInputArray[0][16] = boosts[0];
    					desiredInputArray[0][17] = boosts[1];
    					desiredInputArray[0][18] = boosts[2];
    					desiredInputArray[0][19] = boosts[3];
    					
    					if(interrogate (desiredInputArray, desiredOutputArray, false)) {
    						if(i > 0)
    							this.boostCountValue++;
    						break;
    					}
    				}
    			} else {
		    		boosts[0] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
		    		boosts[1] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
		    		boosts[2] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
		    		boosts[3] = RandomHelper.nextDouble() > 0.2 ? 0 : RandomHelper.nextDoubleFromTo(0.75, 1);
    			}
    			break;
    		case NO_BOOSTS:
    		default:
    			boosts = new double [4];
    		}

    		return boosts;
    }

	public void addAgentToGroup(int groupID) {
		
		if (this.groups.containsKey(groupID)) {
			this.groups.put(groupID, this.groups.get(groupID) + 1);
		} else {
			this.groups.put(groupID, 1);
			this.groupAffinities.put(groupID, RandomHelper.nextDoubleFromTo(0.3, 0.6));
		}
		
	}
	
	public void removeAgentFromGroup(int groupID) {
		if (this.groups.containsKey(groupID)) {
			if (this.groups.get(groupID) <= 1) {
				this.groups.remove(groupID);
				this.groupAffinities.remove(groupID);
			} else {
				this.groups.put(groupID, this.groups.get(groupID) - 1);				
			}
		}
	}

	public void saveNeuralNet(String fileName) {
		try {
			NeuralNet cloneNet = nnet.cloneNet();
			FileOutputStream stream = new FileOutputStream(fileName); 
			ObjectOutputStream out = new ObjectOutputStream(stream); 
			out.writeObject(cloneNet);
			out.close();
		} catch (Exception excp) {
			excp.printStackTrace(); 
		}
	}
	
	public NeuralNet restoreNeuralNet(String fileName) {
		
		NeuralNet newNet = null;
		try {
			FileInputStream stream = new FileInputStream(fileName); 
			ObjectInputStream inp = new ObjectInputStream(stream); 
			newNet = (NeuralNet) inp.readObject();
			inp.close();
		} catch (Exception excp) { excp.printStackTrace();
			excp.printStackTrace(); 
		}
		
		return newNet;
	}

	@Override
	public void cicleTerminated(NeuralNetEvent arg0) {
		// Don't do anything at the end of a cycle
	}

	@Override
	public void errorChanged(NeuralNetEvent arg0) {
        Monitor mon = (Monitor) arg0.getSource();
        if (mon.getCurrentCicle() % 100 == 0)
            System.out.println("Epoch: "+(mon.getTotCicles() - mon.getCurrentCicle())+" RMSE:" + mon.getGlobalError());
    }

	@Override
	public void netStarted(NeuralNetEvent arg0) {
//        Monitor mon = (Monitor) arg0.getSource();
//        if (this.dontOverDoTheLogging1 % 20 == 0) {
//            System.out.print(this.dontOverDoTheLogging1 + ". Network started for ");
//            if (mon.isLearning())
//                System.out.println("training.");
//            else
//                System.out.println("interrogation.");		
//        }
        this.dontOverDoTheLogging1++;
	}

	@Override
	public void netStopped(NeuralNetEvent arg0) {
        Monitor mon = (Monitor) arg0.getSource();
        if (mon.isLearning() && this.dontOverDoTheLogging2 % 20 == 0) {
        		System.out.println(this.dontOverDoTheLogging2 + ". Network stopped. Last RMSE=" + mon.getGlobalError() + " " + nnet.isRunning());
        }
        this.dontOverDoTheLogging2++;
	}

	@Override
	public void netStoppedError(NeuralNetEvent arg0, String errorMessage) {
		System.out.println("Network stopped due the following error: " + errorMessage);
		
	}
}

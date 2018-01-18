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
import org.joone.io.MemoryOutputSynapse;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;

import repast.simphony.random.RandomHelper;

public class AgentController implements NeuralNetListener {

	
	private NeuralNet nnet;
	private MemoryInputSynapse inputSynapse;
	private MemoryInputSynapse desiredOutputSynapse;
	protected HashMap<Integer, Integer> groups;
	protected HashMap<Integer, Double> groupAffinities;
	private int dontOverDoTheTraining1;
	private int dontOverDoTheLogging1;
	private int dontOverDoTheLogging2;
	private double correctnessValue;

	public AgentController(String fileName) {
		groups = new HashMap<Integer, Integer> ();
		groupAffinities = new HashMap<Integer, Double> ();

		this.dontOverDoTheTraining1 = 0;
		this.dontOverDoTheLogging1 = 0;
		this.dontOverDoTheLogging2 = 0;
		this.correctnessValue = 0;

		// initialize the neural network
		this.initNeuralNet(fileName);
	}

	private void initNeuralNet(String fileName) {

		// TODO: READ NNET FROM FILE INSTEAD OF CREATING A NEW ONE
		
        // Create the layers
		LinearLayer input = new LinearLayer();
		SigmoidLayer hidden1 = new SigmoidLayer();
		SigmoidLayer hidden2 = new SigmoidLayer();
		SigmoidLayer output = new SigmoidLayer();
        
        input.setRows(20);
        hidden1.setRows(12);
        hidden2.setRows(6);
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
        
        // Connect the input layer using an input synapse
        this.inputSynapse = new MemoryInputSynapse();
        input.addInputSynapse(inputSynapse);
        
        // Connect the desired output layer using an input synapse
        TeachingSynapse trainer = new TeachingSynapse();
        this.desiredOutputSynapse = new MemoryInputSynapse();
        trainer.setDesired(desiredOutputSynapse);

        // Create a new Neural network object
        nnet = new NeuralNet();
        
        nnet.addLayer(input, NeuralNet.INPUT_LAYER);
        nnet.addLayer(hidden1, NeuralNet.HIDDEN_LAYER);
        nnet.addLayer(hidden2, NeuralNet.HIDDEN_LAYER);
        nnet.addLayer(output, NeuralNet.OUTPUT_LAYER);
        nnet.setTeacher(trainer);
        output.addOutputSynapse(trainer);
        nnet.addNeuralNetListener(this);
        
	}

    public void act(double[][] inputArray, double[][] desiredOutputArray) {
    	
    	this.dontOverDoTheTraining1++;
    	
    	if (this.dontOverDoTheTraining1 < 20000) {
    		this.train(inputArray, desiredOutputArray);
    	}else if (this.dontOverDoTheTraining1 > 30000) {
    		this.interrogate(inputArray, desiredOutputArray);
    	}
    	
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
    
    public void interrogate (double[][] inputArray, double[][] desiredOutputArray) {
    	
    		this.dontOverDoTheLogging2++;
    		
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
    		nnet.go(); 
    		memInp.fwdPut(iPattern);
    		Pattern pattern = memOut.fwdGet();
    		if (this.dontOverDoTheLogging2 % 20 == 0) {
    			System.out.println( (this.dontOverDoTheLogging2 / 20) + ". " + String.format("%.2f" , desiredOutputArray[0][0]) + "|" + String.format("%.2f", pattern.getArray()[0]) );    			
    		}
    		nnet.stop();
    }
    
    public double [] getBoosts() {
    	
    		double[] boosts = new double [4];
    		
    		return boosts;
    }

	public void addAgentToGroup(int groupID) {
		
		if (this.groups.containsKey(groupID)) {
			this.groups.put(groupID, this.groups.get(groupID) + 1);
		} else {
			this.groups.put(groupID, 1);
			this.groupAffinities.put(groupID, RandomHelper.nextDouble());
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
			FileOutputStream stream = new FileOutputStream(fileName); 
			ObjectOutputStream out = new ObjectOutputStream(stream); 
			out.writeObject(this.nnet);
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
        Monitor mon = (Monitor) arg0.getSource();
        if (this.dontOverDoTheLogging1 % 200 == 0 && mon.isLearning()) {
            System.out.print("Network started for ");
            if (mon.isLearning())
                System.out.println("training.");
            else
                System.out.println("interrogation.");		
        }
        this.dontOverDoTheLogging1++;
	}

	@Override
	public void netStopped(NeuralNetEvent arg0) {
        Monitor mon = (Monitor) arg0.getSource();
        if ((this.dontOverDoTheLogging1 - 1) % 200 == 0 && mon.isLearning()) {
        		System.out.println("Network stopped. Last RMSE="+mon.getGlobalError());
        }
	}

	@Override
	public void netStoppedError(NeuralNetEvent arg0, String errorMessage) {
		System.out.println("Network stopped due the following error: " + errorMessage);
		
	}
}

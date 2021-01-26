package at.ac.uibk.dps.cloud.simulator.test.complex;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import at.ac.uibk.dps.cloud.simulator.test.ConsumptionEventAssert;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinConsumer;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinProvider;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceSpreader;
import hu.mta.sztaki.lpds.cloud.simulator.util.SeedSyncer;

public class Exercice {
	public int runningCounter = 0;
	public int destroyCounter = 0;
	//static final int maxTaskCount = 5;
	static final double maxTaskLen = 5;
	static ArrayList<ResourceSpreader> providersList = new ArrayList<ResourceSpreader>();
	static ArrayList<ResourceSpreader> consummersList = new ArrayList<ResourceSpreader>();
	
	@Before
	public void resetSim() throws Exception {
		SeedSyncer.resetCentral();
	}
	
	//new provider
	public static ResourceSpreader getNewProvider(final double randomNumber) {
		MaxMinProvider provider = new MaxMinProvider(randomNumber);
		return provider;
		}
	
	//new consummer
public static ResourceSpreader getNewConsummer(final double randomNumber) {
		MaxMinConsumer consummer= new MaxMinConsumer(randomNumber);
		return consummer;
		}
	
	
	//infrastructure creation
public static void getInfrastructure(final int spreadersNumber) {

	for (int i = 0; i < spreadersNumber; i++) {
		double randomNumber=SeedSyncer.centralRnd.nextDouble();
		ResourceSpreader currentProv = Exercice.getNewProvider(randomNumber);
		providersList.add(currentProv);
	}
	for (int i = 0; i < spreadersNumber; i++) {
		double randomNumber=SeedSyncer.centralRnd.nextDouble();
		ResourceSpreader currentConsummer = Exercice.getNewConsummer(randomNumber);
		consummersList.add(currentConsummer);
	}	
}
	
//randomConsumptions
public void createRandomConsumptions(int consumptions,ArrayList<ResourceSpreader> providersList,ArrayList<ResourceSpreader> consummersList) {
	int randomConsummer;
	int randomProvider;
	//runningCounter++;
	//myTaskCount = 1 + SeedSyncer.centralRnd.nextInt(maxTaskCount - 1);
	ArrayList<ResourceConsumption> consumptionsList = new ArrayList<ResourceConsumption>();
			for (int j = 0; j < consumptions; j++) {
				randomConsummer=SeedSyncer.centralRnd.nextInt(200) ;
				randomProvider=SeedSyncer.centralRnd.nextInt(200);
				ResourceConsumption currentCons = new ResourceConsumption(
						SeedSyncer.centralRnd.nextDouble() * maxTaskLen,
						ResourceConsumption.unlimitedProcessing, consummersList.get(randomConsummer) , providersList.get(randomProvider),
						new ConsumptionEventAssert());
				currentCons.registerConsumption();
				consumptionsList.add(currentCons);
			}
			System.out.println(consumptions+" consumptions created");
	}
		
	
@Test()
public void randomConsumptions() {
	int spreadersNumber=200;
	getInfrastructure(spreadersNumber);
	createRandomConsumptions(1000,providersList,consummersList);
	System.out.println("Simulation started");
	Timed.simulateUntilLastEvent();
	System.out.println("Simulation finished");
}
	
}

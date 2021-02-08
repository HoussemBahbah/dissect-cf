package at.ac.uibk.dps.cloud.simulator.test.complex;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import at.ac.uibk.dps.cloud.simulator.test.ConsumptionEventAssert;
import at.ac.uibk.dps.cloud.simulator.test.ConsumptionEventFoundation;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinConsumer;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinProvider;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceSpreader;
import hu.mta.sztaki.lpds.cloud.simulator.util.SeedSyncer;

public class Exercice extends ConsumptionEventFoundation{
	public int runningCounter = 0;
	public int destroyCounter = 0;
	//static final int maxTaskCount = 5;
	static final double maxTaskLen = 5;
	static ArrayList<ResourceSpreader> providersList = new ArrayList<ResourceSpreader>();
	static ArrayList<ResourceSpreader> consummersList = new ArrayList<ResourceSpreader>();
	static ArrayList<ResourceConsumption> consumptionsList = new ArrayList<ResourceConsumption>();
	
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
	public void createRandomConsumptions(int consumptions,int spreadersNumber,ArrayList<ResourceSpreader> providersList,ArrayList<ResourceSpreader> consummersList) {
		int randomConsummer;
		int randomProvider;
		//runningCounter++;
		//myTaskCount = 1 + SeedSyncer.centralRnd.nextInt(maxTaskCount - 1);
				for (int j = 0; j < consumptions; j++) {
					randomConsummer=SeedSyncer.centralRnd.nextInt(spreadersNumber) ;
					randomProvider=SeedSyncer.centralRnd.nextInt(spreadersNumber);
					ResourceConsumption currentCons = new ResourceConsumption(
							SeedSyncer.centralRnd.nextDouble() * maxTaskLen,
							ResourceConsumption.unlimitedProcessing, consummersList.get(randomConsummer) , providersList.get(randomProvider),
							new ConsumptionEventAssert());
					currentCons.registerConsumption();
					consumptionsList.add(currentCons);
				}
				System.out.println(spreadersNumber+" Providers and "+spreadersNumber+ " Consummers have been createrd");
				System.out.println(consumptions+" consumptions have been created");
		}
		
	
	@Test()
	public void randomConsumptions() {
		int spreadersNumber=20;
		int consumptions=30;
		getInfrastructure(spreadersNumber);
		createRandomConsumptions(consumptions,spreadersNumber,providersList,consummersList);
		System.out.println("The Simulation started");
		Timed.simulateUntilLastEvent();
		System.out.println("The Simulation finished");
	}
		
	}

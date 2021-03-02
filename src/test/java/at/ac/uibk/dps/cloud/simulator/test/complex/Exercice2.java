package at.ac.uibk.dps.cloud.simulator.test.complex;

import at.ac.uibk.dps.cloud.simulator.test.ConsumptionEventAssert;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinConsumer;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinProvider;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import org.junit.Test;

public class Exercice2 {

    @Test(/*timeout = 100*/)
    public void groupManagement() {
        MaxMinProvider prov1 = new MaxMinProvider(1);
        MaxMinProvider prov2 = new MaxMinProvider(1);
        MaxMinConsumer cons1 = new MaxMinConsumer(1);
        MaxMinConsumer cons2 = new MaxMinConsumer(1);


       // new ResourceConsumption(1000, 1, new MaxMinConsumer(1), prov1, new ConsumptionEventAssert())
         //       .registerConsumption();
        new ResourceConsumption(1000, 1, cons1, prov2, new ConsumptionEventAssert()).registerConsumption();
        new ResourceConsumption(500, 1, cons2, prov1, new ConsumptionEventAssert()).registerConsumption();
        new ResourceConsumption(1000, 1, cons2, prov2, new ConsumptionEventAssert()).registerConsumption();
        Timed.simulateUntilLastEvent();

    }

}
